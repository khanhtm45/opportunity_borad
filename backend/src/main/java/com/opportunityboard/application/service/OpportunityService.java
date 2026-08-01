package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.opportunity.*;
import com.opportunityboard.common.exception.*;
import com.opportunityboard.domain.entity.*;
import com.opportunityboard.domain.enums.*;
import com.opportunityboard.infrastructure.repository.*;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;
    private final DomainRepository domainRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final OpportunityDomainRepository opportunityDomainRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /** Tính display_status dẫn xuất (Mục 3.1). */
    private OpportunityResponse.DisplayStatus displayStatusOf(Opportunity o) {
        if (o.getStatus() == OppStatus.HIDDEN) return OpportunityResponse.DisplayStatus.HIDDEN;
        if (o.getStatus() != OppStatus.APPROVED) return null; // không public
        if (o.getDeadline().isBefore(Instant.now())) return OpportunityResponse.DisplayStatus.EXPIRED;
        if (o.getDeadline().isBefore(Instant.now().plusSeconds(3 * 24 * 3600L)))
            return OpportunityResponse.DisplayStatus.CLOSING_SOON;
        return OpportunityResponse.DisplayStatus.OPEN;
    }

    private OpportunityResponse toResponse(Opportunity o) {
        String logo = o.getLogoUrl() != null && !o.getLogoUrl().isBlank()
                ? o.getLogoUrl() : o.getOrg().getLogoUrl();
        return new OpportunityResponse(
                o.getOppId(), o.getTitle(), o.getSlug(),
                o.getOrg().getOrgName(), logo,
                o.getCategory().getCode(), displayStatusOf(o),
                o.getDeadline(), o.getWorkType(), o.getLocation(),
                o.isFeatured(), o.getViewCount(), o.getBookmarkCount(), o.getApplicationCount());
    }

    // -------- F05: PROVIDER LIST OWN --------
    public PagedResponseHolder listMine(int page, int size) {
        User user = currentUser.get();
        Pageable pg = PageRequest.of(page, size);
        Page<Opportunity> p = opportunityRepository.findByCreatedByUserId(user.getUserId(), pg);
        return new PagedResponseHolder(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                (long) p.getTotalElements());
    }

    // -------- F06: ADMIN MODERATION QUEUE --------
    public PagedResponseHolder listPending(int page, int size) {
        Pageable pg = PageRequest.of(page, size);
        Page<Opportunity> p = opportunityRepository.findByStatus(OppStatus.PENDING, pg);
        return new PagedResponseHolder(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                (long) p.getTotalElements());
    }

    public PagedResponseHolder listPublic(int page, int size) {
        Pageable pg = PageRequest.of(page, size);
        Page<Opportunity> p = opportunityRepository.findPublicBoard(pg, Instant.now());
        return new PagedResponseHolder(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                (long) p.getTotalElements());
    }

    public PagedResponseHolder listFeatured(int page, int size) {
        Pageable pg = PageRequest.of(page, size);
        Page<Opportunity> p = opportunityRepository.findFeatured(pg, Instant.now());
        return new PagedResponseHolder(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                (long) p.getTotalElements());
    }

    // -------- F02: SEARCH --------
    public PagedResponseHolder search(String q, UUID categoryId, String workType,
                                       String location, String sort, int page, int size) {
        Pageable pg = PageRequest.of(page, size, resolveSort(sort));
        WorkType wt = workType == null ? null : WorkType.valueOf(workType);
        LocationType lt = location == null ? null : LocationType.valueOf(location);
        Page<Opportunity> p = opportunityRepository.search(q, categoryId, wt, lt, Instant.now(), pg);
        return new PagedResponseHolder(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                (long) p.getTotalElements());
    }

    private static Sort resolveSort(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "applicationCount"));
            case "deadline" -> Sort.by(Sort.Direction.ASC, "deadline");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt");
        };
    }

    // -------- F03: DETAIL --------
    @Transactional
    public OpportunityDetailResponse detail(String slug) {
        Opportunity o = opportunityRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (o.getStatus() != OppStatus.APPROVED && o.getStatus() != OppStatus.HIDDEN) {
            throw new NotFoundException("Opportunity chưa công khai");
        }
        // tăng view_count (idempotent đơn giản; production dùng Redis/log)
        o.setViewCount(o.getViewCount() + 1);
        opportunityRepository.save(o);

        List<UUID> domainIds = List.of(); // load từ opportunity_domains nếu cần
        List<OpportunityResponse> related = opportunityRepository
                .findPublicBoard(PageRequest.of(0, 5), Instant.now())
                .getContent().stream().filter(r -> !r.getOppId().equals(o.getOppId()))
                .map(this::toResponse).limit(5).collect(Collectors.toList());

        String detailLogo = o.getLogoUrl() != null && !o.getLogoUrl().isBlank()
                ? o.getLogoUrl() : o.getOrg().getLogoUrl();
        return new OpportunityDetailResponse(
                o.getOppId(), o.getOrg().getOrgId(), o.getTitle(), o.getSlug(),
                o.getOrg().getOrgName(), detailLogo, o.getOrg().getDescription(),
                o.getCategory().getCode(), o.getCategory().getCategoryName(),
                o.getDescription(), o.getRequirements(), o.getBenefits(),
                o.getLocation(), o.getWorkType(), o.getApplyMode(), o.getExternalLink(),
                o.getDeadline(), o.getStatus(), o.getPublishedAt(), o.isFeatured(),
                o.getViewCount(), o.getBookmarkCount(), o.getApplicationCount(), domainIds, related);
    }

    // -------- F05.1: PROVIDER CREATE (DRAFT) --------
    @Transactional
    public UUID create(OpportunityRequest req) {
        User user = currentUser.get();
        if (user.getRole() != UserRole.PROVIDER) throw new ForbiddenException("Chỉ Provider được đăng tin");
        Organization org = organizationRepository.findByOwnerUserUserId(user.getUserId()).stream()
                .findFirst().orElseThrow(() -> new ForbiddenException("Bạn chưa có tổ chức được duyệt"));
        if (org.getVerifiedStatus() != OrgVerified.VERIFIED)
            throw new ForbiddenException("Tổ chức chưa được xác minh (verified)");

        if (req.categoryId() == null)
            throw new BadRequestException("categoryId bắt buộc");
        Category cat = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category không tồn tại"));
        Opportunity o = Opportunity.builder()
                .org(org).createdBy(user).category(cat)
                .title(req.title()).description(req.description()).requirements(req.requirements())
                .benefits(req.benefits()).location(req.location()).workType(req.workType())
                .applyMode(req.applyMode()).externalLink(req.externalLink()).internalForm(req.internalForm())
                .logoUrl(req.logoUrl()).deadline(req.deadline()).status(OppStatus.DRAFT)
                .slug(slugify(req.title())).build();
        o = opportunityRepository.save(o);
        // gán domains vào bảng join opportunity_domains
        if (req.domainIds() != null) {
            for (UUID d : req.domainIds()) {
                opportunityDomainRepository.save(
                        OpportunityDomain.builder().oppId(o.getOppId()).domainId(d).build());
            }
        }
        return o.getOppId();
    }

    // -------- F05.1: SUBMIT -> PENDING --------
    @Transactional
    public void submit(UUID oppId) {
        Opportunity o = requireOwner(oppId);
        if (o.getStatus() != OppStatus.DRAFT && o.getStatus() != OppStatus.HIDDEN)
            throw new ConflictException("Chỉ DRAFT/HIDDEN mới gửi duyệt");
        o.setStatus(OppStatus.PENDING);
        opportunityRepository.save(o);
        // thông báo Admin có tin chờ duyệt (PENDING_REVIEW)
        notificationService.notifyPendingReview(o);
    }

    // -------- F05: EDIT --------
    @Transactional
    public void update(UUID oppId, OpportunityRequest req) {
        Opportunity o = requireOwner(oppId);
        if (o.getStatus() != OppStatus.DRAFT && o.getStatus() != OppStatus.HIDDEN)
            throw new ConflictException("Chỉ sửa được khi DRAFT hoặc HIDDEN");
        o.setTitle(req.title()); o.setDescription(req.description());
        o.setRequirements(req.requirements()); o.setBenefits(req.benefits());
        o.setLocation(req.location()); o.setWorkType(req.workType());
        o.setApplyMode(req.applyMode()); o.setExternalLink(req.externalLink());
        o.setInternalForm(req.internalForm()); o.setLogoUrl(req.logoUrl()); o.setDeadline(req.deadline());
        opportunityRepository.save(o);
    }

    // -------- F05: HIDE/SHOW --------
    @Transactional
    public void setHidden(UUID oppId, boolean hidden) {
        Opportunity o = requireOwner(oppId);
        if (o.getStatus() == OppStatus.APPROVED && hidden) o.setStatus(OppStatus.HIDDEN);
        else if (o.getStatus() == OppStatus.HIDDEN && !hidden) o.setStatus(OppStatus.APPROVED);
        else throw new ConflictException("Chỉ APPROVED↔HIDDEN");
        opportunityRepository.save(o);
    }

    // -------- F05: CLOSE --------
    @Transactional
    public void close(UUID oppId) {
        Opportunity o = requireOwner(oppId);
        if (o.getStatus() != OppStatus.APPROVED) throw new ConflictException("Chỉ APPROVED mới đóng");
        o.setStatus(OppStatus.CLOSED);
        opportunityRepository.save(o);
    }

    // -------- F05: EXTEND --------
    @Transactional
    public void extend(UUID oppId, Instant newDeadline) {
        Opportunity o = requireOwner(oppId);
        if (newDeadline.isBefore(Instant.now())) throw new ConflictException("Deadline mới phải ở tương lai");
        o.setDeadline(newDeadline);
        opportunityRepository.save(o);
    }

    // -------- F06.1: ADMIN APPROVE --------
    @Transactional
    public void approve(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        if (o.getStatus() != OppStatus.PENDING) throw new ConflictException("Chỉ PENDING mới duyệt");
        User admin = currentUser.get();
        o.setStatus(OppStatus.APPROVED);
        o.setModeratedBy(admin); o.setModeratedAt(Instant.now());
        o.setPublishedAt(Instant.now());
        opportunityRepository.save(o);
        moderationLogRepository.save(ModerationLog.builder().opportunity(o).admin(admin)
                .action(ModerationAction.APPROVED).build());
        notificationService.notifyOppApproved(o);
        // gửi NEW_OPP cho user quan tâm category/domain
        notificationService.notifyNewOpp(o);
    }

    // -------- F06.1: ADMIN REJECT --------
    @Transactional
    public void reject(UUID oppId, ModerateRequest req) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        if (o.getStatus() != OppStatus.PENDING) throw new ConflictException("Chỉ PENDING mới từ chối");
        User admin = currentUser.get();
        o.setStatus(OppStatus.REJECTED); o.setRejectionReason(req.reason());
        o.setModeratedBy(admin); o.setModeratedAt(Instant.now());
        opportunityRepository.save(o);
        moderationLogRepository.save(ModerationLog.builder().opportunity(o).admin(admin)
                .action(ModerationAction.REJECTED).reason(req.reason()).build());
        notificationService.notifyOppRejected(o);
    }

    // -------- FEATURED (Mục 3) --------
    @Transactional
    public void feature(UUID oppId, FeatureRequest req, boolean isAdmin) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        if (isAdmin) {
            User admin = currentUser.get();
            o.setFeatured(true);
            o.setFeaturedBy(admin);
            o.setFeaturedAt(Instant.now());
            o.setFeaturedUntil(req.featuredUntil());
            opportunityRepository.save(o);
        } else {
            // Provider đề xuất -> Admin duyệt. Ở MVP lưu ý định vào rejectionReason tạm (hoặc bảng riêng).
            throw new ForbiddenException("Chỉ Admin set Featured (Provider đề xuất qua feature-request)");
        }
    }

    // -------- HELPERS / RBAC --------
    /** Chặn IDOR: chỉ owner org hoặc admin mới sửa. */
    private Opportunity requireOwner(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        User user = currentUser.get();
        if (user.getRole() == UserRole.ADMIN) return o;
        boolean owner = o.getOrg().getOwnerUser().getUserId().equals(user.getUserId())
                || orgMemberRepository.existsByOrgOrgIdAndUserUserId(o.getOrg().getOrgId(), user.getUserId());
        if (!owner) throw new ForbiddenException("Không có quyền với opportunity này");
        return o;
    }

    private String slugify(String title) {
        return java.util.UUID.randomUUID().toString().substring(0, 8)
                + "-" + title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    // holder đơn giản để controller map pagination
    public record PagedResponseHolder(List<OpportunityResponse> items, long total) {}
}

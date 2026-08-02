package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.document.DocumentResponse;
import com.opportunityboard.application.dto.document.OppDocumentInput;
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
import java.util.Map;
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
    private final OpportunityDocumentRepository opportunityDocumentRepository;
    private final NotificationService notificationService;
    private final MediaLinkService mediaLinkService;
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
        return toResponse(o, true);
    }

    /** signMedia=false khi provider sửa tin — giữ ref ob-s3:// trong form. */
    private OpportunityResponse toResponse(Opportunity o, boolean signMedia) {
        String logoRaw = o.getLogoUrl() != null && !o.getLogoUrl().isBlank()
                ? o.getLogoUrl() : o.getOrg().getLogoUrl();
        String logo = mediaLinkService.resolveForDisplay(logoRaw, signMedia);
        String banner = mediaLinkService.resolveForDisplay(o.getBannerUrl(), signMedia);
        return new OpportunityResponse(
                o.getOppId(), o.getTitle(), o.getSlug(),
                o.getOrg().getOrgName(), logo, banner,
                o.getCategory().getCode(), displayStatusOf(o),
                o.getDeadline(), o.getWorkType(), o.getLocation(),
                o.getEmploymentType(), o.getJobLevel(), o.getExperienceLevel(),
                o.getSalaryMin(), o.getSalaryMax(), o.getSalaryCurrency(), o.isSalaryNegotiable(),
                o.isFeatured(), o.getViewCount(), o.getBookmarkCount(),
                o.getApplicationCount(), o.getShareCount(),
                o.getStatus(), o.getRejectionReason(), o.getAiModerationNote());
    }

    // -------- F05: PROVIDER LIST OWN --------
    public PagedResponseHolder listMine(int page, int size) {
        User user = currentUser.get();
        Pageable pg = PageRequest.of(page, size);
        Page<Opportunity> p = opportunityRepository.findByCreatedByUserId(user.getUserId(), pg);
        return new PagedResponseHolder(p.getContent().stream()
                .map(o -> toResponse(o, false))
                .collect(Collectors.toList()),
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
        List<DocumentResponse> docs = listDocuments(o.getOppId());
        Organization org = o.getOrg();
        return new OpportunityDetailResponse(
                o.getOppId(), org.getOrgId(), o.getTitle(), o.getSlug(),
                org.getOrgName(), detailLogo, o.getBannerUrl(),
                org.getDescription(), org.getWebsite(),
                org.getContactEmail(), org.getContactPhone(),
                org.getTaxCode(), org.getAddress(), org.getIndustry(), org.getCompanySize(),
                o.getCategory().getCode(), o.getCategory().getCategoryName(),
                o.getDescription(), o.getRequirements(), o.getBenefits(),
                o.getSalaryOrReward(), o.getSalaryMin(), o.getSalaryMax(),
                o.getSalaryCurrency(), o.isSalaryNegotiable(),
                o.getSelectionProcess(),
                o.getJobLevel(), o.getExperienceLevel(), o.getEducationLevel(),
                o.getHeadcount(), o.getEmploymentType(),
                o.getAddressDetail(), o.getWorkingSchedule(), o.getSkills(),
                o.getLocation(), o.getWorkType(), o.getApplyMode(), o.getExternalLink(),
                o.getDeadline(), o.getStatus(), o.getPublishedAt(), o.isFeatured(),
                o.getViewCount(), o.getBookmarkCount(), o.getApplicationCount(),
                o.getShareCount(), domainIds, related, docs);
    }

    /** F04.3: tăng share_count, trả URL công khai để FE copy/share. */
    @Transactional
    public java.util.Map<String, Object> share(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (o.getStatus() != OppStatus.APPROVED) throw new NotFoundException("Opportunity chưa công khai");
        o.setShareCount(o.getShareCount() + 1);
        opportunityRepository.save(o);
        return java.util.Map.of(
                "oppId", o.getOppId(),
                "slug", o.getSlug(),
                "shareCount", o.getShareCount());
    }

    /** Tracking click link ngoài (EXTERNAL apply). */
    @Transactional
    public void externalClick(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (o.getStatus() != OppStatus.APPROVED) throw new NotFoundException("Opportunity chưa công khai");
        o.setViewCount(o.getViewCount() + 1);
        opportunityRepository.save(o);
    }

    /** Explicit view bump (khi FE chỉ cần ping, không load full detail). */
    @Transactional
    public void recordView(UUID oppId) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Opportunity không tồn tại"));
        if (o.getStatus() != OppStatus.APPROVED) throw new NotFoundException("Opportunity chưa công khai");
        o.setViewCount(o.getViewCount() + 1);
        opportunityRepository.save(o);
    }

    // -------- F05.1: PROVIDER CREATE (DRAFT) --------
    @Transactional
    public Map<String, Object> create(OpportunityRequest req) {
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
        validateSalary(req);
        Opportunity o = Opportunity.builder()
                .org(org).createdBy(user).category(cat)
                .title(req.title()).description(req.description()).requirements(req.requirements())
                .benefits(req.benefits())
                .salaryOrReward(req.salaryOrReward())
                .salaryMin(req.salaryMin()).salaryMax(req.salaryMax())
                .salaryCurrency(req.salaryCurrency() != null && !req.salaryCurrency().isBlank() ? req.salaryCurrency() : "VND")
                .salaryNegotiable(Boolean.TRUE.equals(req.salaryNegotiable()))
                .selectionProcess(req.selectionProcess())
                .jobLevel(req.jobLevel()).experienceLevel(req.experienceLevel())
                .educationLevel(req.educationLevel()).headcount(req.headcount())
                .employmentType(req.employmentType())
                .addressDetail(req.addressDetail()).workingSchedule(req.workingSchedule())
                .skills(req.skills())
                .location(req.location()).workType(req.workType())
                .applyMode(req.applyMode()).externalLink(req.externalLink()).externalRef(req.externalRef()).internalForm(req.internalForm())
                .logoUrl(req.logoUrl()).bannerUrl(req.bannerUrl())
                .deadline(req.deadline()).status(OppStatus.DRAFT)
                .slug(slugify(req.title())).build();
        o = opportunityRepository.save(o);
        // gán domains vào bảng join opportunity_domains
        if (req.domainIds() != null) {
            for (UUID d : req.domainIds()) {
                opportunityDomainRepository.save(
                        OpportunityDomain.builder().oppId(o.getOppId()).domainId(d).build());
            }
        }
        replaceDocuments(o, req.documents());
        return Map.of("oppId", o.getOppId());
    }

    // -------- F05.1: SUBMIT -> PENDING --------
    @Transactional
    public void submit(UUID oppId) {
        Opportunity o = requireOwner(oppId);
        if (o.getStatus() != OppStatus.DRAFT && o.getStatus() != OppStatus.HIDDEN)
            throw new ConflictException("Chỉ DRAFT/HIDDEN mới gửi duyệt");
        if (opportunityDocumentRepository.countByOpportunityOppId(oppId) < 1) {
            throw new BadRequestException("Cần cung cấp ít nhất 1 hồ sơ liên quan trước khi gửi duyệt");
        }
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
        validateSalary(req);
        o.setTitle(req.title()); o.setDescription(req.description());
        o.setRequirements(req.requirements()); o.setBenefits(req.benefits());
        o.setSalaryOrReward(req.salaryOrReward());
        o.setSalaryMin(req.salaryMin()); o.setSalaryMax(req.salaryMax());
        o.setSalaryCurrency(req.salaryCurrency() != null && !req.salaryCurrency().isBlank() ? req.salaryCurrency() : "VND");
        o.setSalaryNegotiable(Boolean.TRUE.equals(req.salaryNegotiable()));
        o.setSelectionProcess(req.selectionProcess());
        o.setJobLevel(req.jobLevel()); o.setExperienceLevel(req.experienceLevel());
        o.setEducationLevel(req.educationLevel()); o.setHeadcount(req.headcount());
        o.setEmploymentType(req.employmentType());
        o.setAddressDetail(req.addressDetail()); o.setWorkingSchedule(req.workingSchedule());
        o.setSkills(req.skills());
        o.setLocation(req.location()); o.setWorkType(req.workType());
        o.setApplyMode(req.applyMode()); o.setExternalLink(req.externalLink());
        o.setExternalRef(req.externalRef());
        o.setInternalForm(req.internalForm());
        o.setLogoUrl(req.logoUrl()); o.setBannerUrl(req.bannerUrl());
        o.setDeadline(req.deadline());
        opportunityRepository.save(o);
        if (req.documents() != null) {
            replaceDocuments(o, req.documents());
        }
    }

    public List<DocumentResponse> listDocumentsForOwnerOrAdmin(UUID oppId) {
        requireOwner(oppId);
        return listDocuments(oppId);
    }

    private List<DocumentResponse> listDocuments(UUID oppId) {
        return opportunityDocumentRepository.findByOpportunityOppId(oppId).stream()
                .map(d -> new DocumentResponse(
                        d.getDocId(), d.getDocType().name(), d.getTitle(), d.getFileUrl(),
                        mediaLinkService.resolveForDisplay(d.getFileUrl(), true),
                        d.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void validateSalary(OpportunityRequest req) {
        if (req.salaryMin() != null && req.salaryMin() < 0)
            throw new BadRequestException("salaryMin không hợp lệ");
        if (req.salaryMax() != null && req.salaryMax() < 0)
            throw new BadRequestException("salaryMax không hợp lệ");
        if (req.salaryMin() != null && req.salaryMax() != null && req.salaryMin() > req.salaryMax())
            throw new BadRequestException("salaryMin không được lớn hơn salaryMax");
        if (req.headcount() != null && req.headcount() < 1)
            throw new BadRequestException("headcount phải ≥ 1");
    }

    private void replaceDocuments(Opportunity o, List<OppDocumentInput> inputs) {
        opportunityDocumentRepository.deleteByOpportunityOppId(o.getOppId());
        opportunityDocumentRepository.flush();
        if (inputs == null || inputs.isEmpty()) return;
        for (OppDocumentInput input : inputs) {
            if (input == null || input.docType() == null
                    || input.title() == null || input.title().isBlank()
                    || input.fileUrl() == null || input.fileUrl().isBlank()) {
                throw new BadRequestException("Hồ sơ tin đăng thiếu docType/title/fileUrl");
            }
            String fu = input.fileUrl().trim();
            if (!(fu.startsWith("http://") || fu.startsWith("https://") || fu.startsWith("ob-s3://"))) {
                throw new BadRequestException("fileUrl hồ sơ tin phải là https:// hoặc ob-s3://");
            }
            opportunityDocumentRepository.save(OpportunityDocument.builder()
                    .opportunity(o)
                    .docType(input.docType())
                    .title(input.title().trim())
                    .fileUrl(input.fileUrl().trim())
                    .build());
        }
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
        o.setAiModerationNote(null);
        o.setRejectionReason(null);
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

    /**
     * Yêu cầu provider bổ sung hồ sơ / sửa tin — trả về DRAFT kèm lý do, gửi thông báo.
     * Admin có thể quét AI lại sau khi provider gửi duyệt.
     */
    @Transactional
    public void requestUpdate(UUID oppId, ModerateRequest req) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        if (o.getStatus() != OppStatus.PENDING && o.getStatus() != OppStatus.REJECTED) {
            throw new ConflictException("Chỉ tin PENDING/REJECTED mới yêu cầu cập nhật");
        }
        String reason = req != null && req.reason() != null && !req.reason().isBlank()
                ? req.reason().trim()
                : "Vui lòng bổ sung hồ sơ chương trình / sửa nội dung tin rồi gửi duyệt lại.";
        User admin = currentUser.get();
        o.setStatus(OppStatus.DRAFT);
        o.setAiModerationNote(reason);
        o.setRejectionReason(reason);
        o.setModeratedBy(admin);
        o.setModeratedAt(Instant.now());
        opportunityRepository.save(o);
        moderationLogRepository.save(ModerationLog.builder().opportunity(o).admin(admin)
                .action(ModerationAction.REJECTED).reason("REQUEST_UPDATE: " + reason).build());
        notificationService.notifyOppUpdateRequired(o, reason);
    }

    /** Lưu ghi chú AI scan (không đổi status) — Admin xem lại trước khi quyết định. */
    @Transactional
    public void saveAiScanNote(UUID oppId, String note) {
        Opportunity o = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Không tồn tại"));
        o.setAiModerationNote(note);
        o.setAiScannedAt(Instant.now());
        opportunityRepository.save(o);
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

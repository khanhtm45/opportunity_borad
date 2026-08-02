package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.document.DocumentResponse;
import com.opportunityboard.application.dto.document.OrgDocumentInput;
import com.opportunityboard.application.dto.org.OrgProfileResponse;
import com.opportunityboard.application.dto.org.OrgProfileUpdateRequest;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.ForbiddenException;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.OrgDocument;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.OrgDocumentRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgDocumentService {

    private final OrgDocumentRepository orgDocumentRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUser currentUser;
    private final MediaLinkService mediaLinkService;

    public Organization requireOwnedOrg() {
        User user = currentUser.get();
        if (user.getRole() != UserRole.PROVIDER && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Chỉ Provider quản lý hồ sơ tổ chức");
        }
        return organizationRepository.findByOwnerUserUserId(user.getUserId()).stream()
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("Bạn chưa có tổ chức"));
    }

    public OrgProfileResponse getMyProfile() {
        return toProfile(requireOwnedOrg());
    }

    @Transactional
    public OrgProfileResponse updateMyProfile(OrgProfileUpdateRequest req) {
        Organization org = requireOwnedOrg();
        if (req.orgName() != null && !req.orgName().isBlank()) {
            org.setOrgName(req.orgName().trim());
        }
        if (req.website() != null) org.setWebsite(blankToNull(req.website()));
        if (req.description() != null) org.setDescription(blankToNull(req.description()));
        if (req.contactPhone() != null) org.setContactPhone(blankToNull(req.contactPhone()));
        if (req.taxCode() != null) org.setTaxCode(blankToNull(req.taxCode()));
        if (req.address() != null) org.setAddress(blankToNull(req.address()));
        if (req.industry() != null) org.setIndustry(blankToNull(req.industry()));
        if (req.companySize() != null) org.setCompanySize(req.companySize());
        if (req.logoUrl() != null) {
            String logo = blankToNull(req.logoUrl());
            org.setLogoUrl(logo);
        }
        org.setUpdatedAt(Instant.now());
        markPendingForRereview(org);
        return toProfile(organizationRepository.save(org));
    }

    public List<DocumentResponse> listMine() {
        Organization org = requireOwnedOrg();
        return toResponses(orgDocumentRepository.findByOrgOrgId(org.getOrgId()));
    }

    public List<DocumentResponse> listByOrg(UUID orgId) {
        User user = currentUser.get();
        if (user.getRole() != UserRole.ADMIN) {
            Organization mine = requireOwnedOrg();
            if (!mine.getOrgId().equals(orgId)) {
                throw new ForbiddenException("Không có quyền xem hồ sơ tổ chức này");
            }
        }
        if (!organizationRepository.existsById(orgId)) {
            throw new NotFoundException("Tổ chức không tồn tại");
        }
        return toResponses(orgDocumentRepository.findByOrgOrgId(orgId));
    }

    @Transactional
    public DocumentResponse add(OrgDocumentInput input) {
        Organization org = requireOwnedOrg();
        validateInput(input);
        OrgDocument doc = OrgDocument.builder()
                .org(org)
                .docType(input.docType())
                .title(input.title().trim())
                .fileUrl(input.fileUrl().trim())
                .build();
        doc = orgDocumentRepository.save(doc);
        markPendingForRereview(org);
        organizationRepository.save(org);
        return toResponse(doc);
    }

    @Transactional
    public void delete(UUID docId) {
        Organization org = requireOwnedOrg();
        if (org.getVerifiedStatus() == OrgVerified.VERIFIED) {
            throw new BadRequestException("Không xoá hồ sơ khi tổ chức đã được xác minh");
        }
        OrgDocument doc = orgDocumentRepository.findById(docId)
                .orElseThrow(() -> new NotFoundException("Hồ sơ không tồn tại"));
        if (!doc.getOrg().getOrgId().equals(org.getOrgId())) {
            throw new ForbiddenException("Không có quyền xoá hồ sơ này");
        }
        orgDocumentRepository.delete(doc);
        if (org.getVerifiedStatus() == OrgVerified.NEEDS_UPDATE
                || org.getVerifiedStatus() == OrgVerified.REJECTED) {
            markPendingForRereview(org);
            organizationRepository.save(org);
        }
    }

    /** Sau khi provider sửa hồ sơ/thông tin → chờ Admin/AI quét lại. */
    private void markPendingForRereview(Organization org) {
        if (org.getVerifiedStatus() == OrgVerified.VERIFIED) {
            return;
        }
        org.setVerifiedStatus(OrgVerified.PENDING);
        org.setVerifiedAt(null);
        org.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void saveAllForOrg(Organization org, List<OrgDocumentInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException("Cần cung cấp ít nhất 1 hồ sơ tổ chức");
        }
        for (OrgDocumentInput input : inputs) {
            validateInput(input);
            orgDocumentRepository.save(OrgDocument.builder()
                    .org(org)
                    .docType(input.docType())
                    .title(input.title().trim())
                    .fileUrl(input.fileUrl().trim())
                    .build());
        }
    }

    public void requireHasDocuments(UUID orgId) {
        if (orgDocumentRepository.countByOrgOrgId(orgId) < 1) {
            throw new BadRequestException("Tổ chức chưa nộp hồ sơ xác minh");
        }
    }

    private void validateInput(OrgDocumentInput input) {
        if (input == null || input.docType() == null
                || input.title() == null || input.title().isBlank()
                || input.fileUrl() == null || input.fileUrl().isBlank()) {
            throw new BadRequestException("Hồ sơ tổ chức thiếu docType/title/fileUrl");
        }
        String u = input.fileUrl().trim();
        if (!(u.startsWith("http://") || u.startsWith("https://") || u.startsWith("ob-s3://"))) {
            throw new BadRequestException("fileUrl phải là https:// hoặc ob-s3:// (upload S3)");
        }
    }

    private List<DocumentResponse> toResponses(List<OrgDocument> docs) {
        return docs.stream().map(this::toResponse).toList();
    }

    private DocumentResponse toResponse(OrgDocument d) {
        String ref = d.getFileUrl();
        return new DocumentResponse(
                d.getDocId(), d.getDocType().name(), d.getTitle(), ref,
                mediaLinkService.resolveForDisplay(ref, true),
                d.getCreatedAt());
    }

    private OrgProfileResponse toProfile(Organization org) {
        boolean needsUpdate = org.getVerifiedStatus() == OrgVerified.NEEDS_UPDATE;
        String hint = needsUpdate
                ? "Hồ sơ/thông tin chưa đạt. Vui lòng cập nhật giấy tờ hoặc liên hệ Admin theo ghi chú bên dưới, rồi chờ kiểm duyệt lại."
                : null;
        String logoRef = org.getLogoUrl();
        return new OrgProfileResponse(
                org.getOrgId(),
                org.getOrgName(),
                org.getWebsite(),
                org.getDescription(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getTaxCode(),
                org.getAddress(),
                org.getIndustry(),
                org.getCompanySize(),
                logoRef,
                mediaLinkService.resolveForDisplay(logoRef, true),
                org.getVerifiedStatus(),
                org.getVerificationNote(),
                org.getAiScannedAt(),
                needsUpdate,
                hint
        );
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

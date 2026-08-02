package com.opportunityboard.integration;

import com.opportunityboard.application.dto.auth.ProviderRegisterRequest;
import com.opportunityboard.application.dto.document.OppDocumentInput;
import com.opportunityboard.application.dto.document.OrgDocumentInput;
import com.opportunityboard.application.dto.opportunity.OpportunityRequest;
import com.opportunityboard.application.service.AdminService;
import com.opportunityboard.application.service.AuthService;
import com.opportunityboard.application.service.OpportunityService;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.OppDocType;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.OrgDocType;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.domain.enums.WorkType;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import com.opportunityboard.infrastructure.repository.OpportunityDocumentRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrgDocumentRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enforce hồ sơ org (register/verify) và hồ sơ tin đăng (submit).
 */
class ProviderDocumentsTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired AdminService adminService;
    @Autowired OpportunityService opportunityService;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired OrgDocumentRepository orgDocumentRepository;
    @Autowired OpportunityRepository opportunityRepository;
    @Autowired OpportunityDocumentRepository opportunityDocumentRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    void register_provider_requires_documents() {
        cleanupUser("prov-nodoc@test.com");
        ProviderRegisterRequest req = new ProviderRegisterRequest(
                "Org NoDoc", null, null, "prov-nodoc@test.com", null, null, null, null, null,
                "Prov", "password123", List.of());
        assertThrows(BadRequestException.class, () -> authService.registerProvider(req));
    }

    @Test
    void register_provider_saves_org_documents() {
        cleanupUser("prov-docs@test.com");
        ProviderRegisterRequest req = new ProviderRegisterRequest(
                "Org Docs", "https://org.example", "desc", "prov-docs@test.com", "0900000000",
                "0123456789", "Hà Nội", "IT", null, "Prov Docs", "password123",
                List.of(new OrgDocumentInput(OrgDocType.TAX_CODE, "MST", "https://example.com/mst.pdf")));
        authService.registerProvider(req);
        User u = userRepository.findByEmail("prov-docs@test.com").orElseThrow();
        Organization org = organizationRepository.findByOwnerUserUserId(u.getUserId()).get(0);
        assertEquals(1, orgDocumentRepository.countByOrgOrgId(org.getOrgId()));
        assertEquals(OrgVerified.PENDING, org.getVerifiedStatus());
    }

    @Test
    void verify_org_rejected_without_documents() {
        User provider = loginAs(UserRole.PROVIDER, "prov-verify-nodoc@test.com");
        organizationRepository.save(Organization.builder()
                .ownerUser(provider).orgName("Org bare")
                .verifiedStatus(OrgVerified.PENDING).build());

        User admin = loginAs(UserRole.ADMIN, "admin-verify-nodoc@test.com");
        assertThrows(BadRequestException.class, () -> adminService.verifyOrg(provider.getUserId()));
    }

    @Test
    void verify_org_ok_with_documents() {
        User provider = loginAs(UserRole.PROVIDER, "prov-verify-ok@test.com");
        Organization org = organizationRepository.save(Organization.builder()
                .ownerUser(provider).orgName("Org ok")
                .verifiedStatus(OrgVerified.PENDING).build());
        orgDocumentRepository.save(com.opportunityboard.domain.entity.OrgDocument.builder()
                .org(org).docType(OrgDocType.BUSINESS_LICENSE)
                .title("GP").fileUrl("https://example.com/gp.pdf").build());

        User admin = loginAs(UserRole.ADMIN, "admin-verify-ok@test.com");
        adminService.verifyOrg(provider.getUserId());
        assertEquals(OrgVerified.VERIFIED,
                organizationRepository.findById(org.getOrgId()).orElseThrow().getVerifiedStatus());
    }

    @Test
    void submit_rejected_without_opportunity_documents() {
        User provider = loginAs(UserRole.PROVIDER, "prov-submit-nodoc@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository,
                categoryRepository, provider, OppStatus.DRAFT);
        assertEquals(0, opportunityDocumentRepository.countByOpportunityOppId(o.getOppId()));
        assertThrows(BadRequestException.class, () -> opportunityService.submit(o.getOppId()));
    }

    @Test
    void submit_ok_with_opportunity_documents() {
        User provider = loginAs(UserRole.PROVIDER, "prov-submit-ok@test.com");
        Opportunity o = TestFixtures.oppWithDocs(organizationRepository, opportunityRepository,
                categoryRepository, opportunityDocumentRepository, provider, OppStatus.DRAFT);
        opportunityService.submit(o.getOppId());
        assertEquals(OppStatus.PENDING, opportunityRepository.findById(o.getOppId()).orElseThrow().getStatus());
    }

    @Test
    void create_persists_documents_and_returns_oppId() {
        User provider = loginAs(UserRole.PROVIDER, "prov-create-doc@test.com");
        TestFixtures.verifiedOrg(organizationRepository, provider);
        UUID categoryId = TestFixtures.anyCategory(categoryRepository).getCategoryId();

        OpportunityRequest req = new OpportunityRequest(
                categoryId, "Tin có hồ sơ", "<p>desc</p>", null, null, null,
                10_000_000L, 20_000_000L, "VND", false, null,
                null, null, null, 1, null, null, null, null,
                LocationType.TOAN_QUOC, WorkType.ONLINE, ApplyMode.INTERNAL,
                null, null, null, null, null,
                Instant.now().plusSeconds(86400), null,
                List.of(new OppDocumentInput(OppDocType.PROGRAM_PROOF, "Proof", "https://example.com/p.pdf")));

        Map<String, Object> created = opportunityService.create(req);
        UUID oppId = (UUID) created.get("oppId");
        assertNotNull(oppId);
        assertEquals(1, opportunityDocumentRepository.countByOpportunityOppId(oppId));
    }
}

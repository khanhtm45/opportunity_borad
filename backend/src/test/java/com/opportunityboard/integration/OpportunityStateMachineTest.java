package com.opportunityboard.integration;

import com.opportunityboard.application.service.OpportunityService;
import com.opportunityboard.common.exception.ConflictException;
import com.opportunityboard.common.exception.ForbiddenException;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Opportunity state machine (DRAFT→PENDING→APPROVED→HIDDEN/CLOSED/EXPIRED)
 * + RBAC guard (chỉ owner/Admin sửa).
 */
class OpportunityStateMachineTest extends AbstractIntegrationTest {

    @Autowired OpportunityService opportunityService;
    @Autowired OpportunityRepository opportunityRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    void draft_to_pending_to_approved_flow() {
        User provider = loginAs(UserRole.PROVIDER, "prov-flow@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.DRAFT);

        // submit -> PENDING
        opportunityService.submit(o.getOppId());
        assertEquals(OppStatus.PENDING, opportunityRepository.findById(o.getOppId()).get().getStatus());

        // admin approve -> APPROVED + publishedAt
        User admin = loginAs(UserRole.ADMIN, "admin-flow@test.com");
        opportunityService.approve(o.getOppId());
        Opportunity reloaded = opportunityRepository.findById(o.getOppId()).get();
        assertEquals(OppStatus.APPROVED, reloaded.getStatus());
        assertNotNull(reloaded.getPublishedAt());
    }

    @Test
    void provider_can_submit_own_opp() {
        User provider = loginAs(UserRole.PROVIDER, "prov-submit@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.DRAFT);
        // provider submit opp của mình -> PENDING (không bị Forbidden)
        opportunityService.submit(o.getOppId());
        assertEquals(OppStatus.PENDING, opportunityRepository.findById(o.getOppId()).get().getStatus());
    }

    @Test
    void reject_requires_pending_and_stores_reason() {
        User provider = loginAs(UserRole.PROVIDER, "prov-rej2@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.PENDING);

        User admin = loginAs(UserRole.ADMIN, "admin-rej@test.com");
        opportunityService.reject(o.getOppId(),
                new com.opportunityboard.application.dto.opportunity.ModerateRequest("Vi phạm nội dung"));
        Opportunity reloaded = opportunityRepository.findById(o.getOppId()).get();
        assertEquals(OppStatus.REJECTED, reloaded.getStatus());
        assertEquals("Vi phạm nội dung", reloaded.getRejectionReason());
    }

    @Test
    void cannot_approve_non_pending() {
        User provider = loginAs(UserRole.PROVIDER, "prov-np@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.DRAFT);

        User admin = loginAs(UserRole.ADMIN, "admin-np@test.com");
        assertThrows(ConflictException.class, () -> opportunityService.approve(o.getOppId()));
    }

    @Test
    void hidden_toggle_only_when_approved() {
        User provider = loginAs(UserRole.PROVIDER, "prov-hide@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.APPROVED);

        opportunityService.setHidden(o.getOppId(), true);
        assertEquals(OppStatus.HIDDEN, opportunityRepository.findById(o.getOppId()).get().getStatus());
        opportunityService.setHidden(o.getOppId(), false);
        assertEquals(OppStatus.APPROVED, opportunityRepository.findById(o.getOppId()).get().getStatus());
    }

    @Test
    void idor_other_provider_cannot_modify() {
        User ownerProvider = loginAs(UserRole.PROVIDER, "prov-owner@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, ownerProvider, OppStatus.DRAFT);

        // Provider khác cố sửa -> Forbidden (chống BOLA)
        User intruder = loginAs(UserRole.PROVIDER, "prov-intruder@test.com");
        assertThrows(ForbiddenException.class, () -> opportunityService.submit(o.getOppId()));
    }
}

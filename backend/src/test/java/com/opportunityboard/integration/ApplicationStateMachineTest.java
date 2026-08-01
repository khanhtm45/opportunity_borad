package com.opportunityboard.integration;

import com.opportunityboard.application.service.ApplicationService;
import com.opportunityboard.common.exception.ConflictException;
import com.opportunityboard.common.exception.ForbiddenException;
import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.AppStatus;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.ApplicationRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Application state machine (6 trạng thái) + IDOR/BOLA guard.
 */
class ApplicationStateMachineTest extends AbstractIntegrationTest {

    @Autowired ApplicationService applicationService;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired OpportunityRepository opportunityRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired CategoryRepository categoryRepository;

    private Opportunity approvedOpp(User owner) {
        return TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, owner, OppStatus.APPROVED);
    }

    @Test
    void student_applies_then_provider_moves_forward() {
        User provider = loginAs(UserRole.PROVIDER, "prov-app@test.com");
        Opportunity o = approvedOpp(provider);

        User student = loginAs(UserRole.STUDENT, "stu-app@test.com");
        UUID appId = applicationService.applyInternal(o.getOppId(), "cv.pdf", "Cover");
        Application app = applicationRepository.findById(appId).get();
        assertEquals(AppStatus.SUBMITTED, app.getStatus());

        // Provider chuyển SUBMITTED -> REVIEWING
        asUser(provider);
        applicationService.changeStatusByProvider(appId, AppStatus.REVIEWING, null);
        assertEquals(AppStatus.REVIEWING, applicationRepository.findById(appId).get().getStatus());

        // -> INTERVIEW
        applicationService.changeStatusByProvider(appId, AppStatus.INTERVIEW, null);
        assertEquals(AppStatus.INTERVIEW, applicationRepository.findById(appId).get().getStatus());

        // -> ACCEPTED
        applicationService.changeStatusByProvider(appId, AppStatus.ACCEPTED, "Pass");
        assertEquals(AppStatus.ACCEPTED, applicationRepository.findById(appId).get().getStatus());
    }

    @Test
    void cannot_move_backward() {
        User provider = loginAs(UserRole.PROVIDER, "prov-back@test.com");
        Opportunity o = approvedOpp(provider);
        User student = loginAs(UserRole.STUDENT, "stu-back@test.com");
        UUID appId = applicationService.applyInternal(o.getOppId(), "cv.pdf", null);

        asUser(provider);
        applicationService.changeStatusByProvider(appId, AppStatus.REVIEWING, null);
        // INTERVIEW -> REVIEWING (lùi) phải bị từ chối
        assertThrows(ConflictException.class,
                () -> applicationService.changeStatusByProvider(appId, AppStatus.REVIEWING, null));
    }

    @Test
    void idor_other_provider_cannot_see_or_change() {
        User ownerProvider = loginAs(UserRole.PROVIDER, "prov-ownapp@test.com");
        Opportunity o = approvedOpp(ownerProvider);
        User student = loginAs(UserRole.STUDENT, "stu-ownapp@test.com");
        UUID appId = applicationService.applyInternal(o.getOppId(), "cv.pdf", null);

        // Provider khác cố đổi status app của opp không thuộc về mình -> Forbidden (BOLA)
        User intruder = loginAs(UserRole.PROVIDER, "prov-intrapp@test.com");
        assertThrows(ForbiddenException.class,
                () -> applicationService.changeStatusByProvider(appId, AppStatus.REVIEWING, null));
    }

    @Test
    void student_cannot_apply_external_opp() {
        User provider = loginAs(UserRole.PROVIDER, "prov-ext@test.com");
        Opportunity o = TestFixtures.opp(organizationRepository, opportunityRepository, categoryRepository, provider, OppStatus.APPROVED);
        // set EXTERNAL
        o.setApplyMode(com.opportunityboard.domain.enums.ApplyMode.EXTERNAL);
        o.setExternalLink("https://example.com/apply");
        opportunityRepository.save(o);

        User student = loginAs(UserRole.STUDENT, "stu-ext@test.com");
        assertThrows(ConflictException.class, () -> applicationService.applyInternal(o.getOppId(), "cv.pdf", null));
    }

    @Test
    void cannot_apply_twice() {
        User provider = loginAs(UserRole.PROVIDER, "prov-twice@test.com");
        Opportunity o = approvedOpp(provider);
        User student = loginAs(UserRole.STUDENT, "stu-twice@test.com");
        applicationService.applyInternal(o.getOppId(), "cv.pdf", null);
        assertThrows(ConflictException.class, () -> applicationService.applyInternal(o.getOppId(), "cv2.pdf", null));
    }

    @Test
    void student_withdraw_own_app() {
        User provider = loginAs(UserRole.PROVIDER, "prov-wd@test.com");
        Opportunity o = approvedOpp(provider);
        User student = loginAs(UserRole.STUDENT, "stu-wd@test.com");
        UUID appId = applicationService.applyInternal(o.getOppId(), "cv.pdf", null);
        applicationService.withdraw(appId);
        assertEquals(AppStatus.WITHDRAWN, applicationRepository.findById(appId).get().getStatus());
    }
}

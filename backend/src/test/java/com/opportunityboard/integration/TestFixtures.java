package com.opportunityboard.integration;

import com.opportunityboard.domain.entity.Category;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.domain.enums.WorkType;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Helper tạo dữ liệu cho integration test (org verified, opportunity các trạng thái).
 */
public class TestFixtures {

    public static Organization verifiedOrg(OrganizationRepository orgRepo, User owner) {
        return orgRepo.save(Organization.builder()
                .ownerUser(owner).orgName("Org " + UUID.randomUUID().toString().substring(0, 6))
                .verifiedStatus(OrgVerified.VERIFIED).verifiedAt(Instant.now()).build());
    }

    public static Category anyCategory(CategoryRepository catRepo) {
        return catRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Category seed chưa có (chạy schema.sql)"));
    }

    public static Opportunity opp(OrganizationRepository orgRepo, OpportunityRepository oppRepo,
                                  CategoryRepository catRepo, User owner, OppStatus status) {
        Organization org = verifiedOrg(orgRepo, owner);
        Category cat = anyCategory(catRepo);
        return oppRepo.save(Opportunity.builder()
                .org(org).createdBy(owner).category(cat)
                .title("Opp " + UUID.randomUUID().toString().substring(0, 6))
                .slug("opp-" + UUID.randomUUID().toString().substring(0, 8))
                .description("<p>Mô tả test</p>").requirements("<p>Yêu cầu</p>").benefits("<p>Quyền lợi</p>")
                .location(LocationType.TOAN_QUOC).workType(WorkType.ONLINE)
                .applyMode(ApplyMode.INTERNAL)
                .deadline(Instant.now().plusSeconds(7 * 24 * 3600L))
                .status(status)
                .publishedAt(status == OppStatus.APPROVED ? Instant.now() : null)
                .build());
    }
}

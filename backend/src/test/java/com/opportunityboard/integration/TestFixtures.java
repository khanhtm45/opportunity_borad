package com.opportunityboard.integration;

import com.opportunityboard.domain.entity.Category;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.OpportunityDocument;
import com.opportunityboard.domain.entity.OrgDocument;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.OppDocType;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.OrgDocType;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.domain.enums.WorkType;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import com.opportunityboard.infrastructure.repository.OpportunityDocumentRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
import com.opportunityboard.infrastructure.repository.OrgDocumentRepository;
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

    public static Organization verifiedOrgWithDocs(OrganizationRepository orgRepo,
                                                   OrgDocumentRepository orgDocRepo,
                                                   User owner) {
        Organization org = verifiedOrg(orgRepo, owner);
        orgDocRepo.save(OrgDocument.builder()
                .org(org)
                .docType(OrgDocType.BUSINESS_LICENSE)
                .title("GPĐKKD test")
                .fileUrl("https://example.com/license.pdf")
                .build());
        return org;
    }

    public static Category anyCategory(CategoryRepository catRepo) {
        return catRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Category seed chưa có (chạy schema.sql)"));
    }

    public static Opportunity opp(OrganizationRepository orgRepo, OpportunityRepository oppRepo,
                                  CategoryRepository catRepo, User owner, OppStatus status) {
        Organization org = verifiedOrg(orgRepo, owner);
        return oppOnOrg(oppRepo, catRepo, null, org, owner, status);
    }

    /** Tạo opp kèm ≥1 hồ sơ liên quan (đủ điều kiện submit). */
    public static Opportunity oppWithDocs(OrganizationRepository orgRepo, OpportunityRepository oppRepo,
                                          CategoryRepository catRepo,
                                          OpportunityDocumentRepository oppDocRepo,
                                          User owner, OppStatus status) {
        Organization org = verifiedOrg(orgRepo, owner);
        return oppOnOrg(oppRepo, catRepo, oppDocRepo, org, owner, status);
    }

    public static Opportunity oppOnOrg(OpportunityRepository oppRepo, CategoryRepository catRepo,
                                       OpportunityDocumentRepository oppDocRepo,
                                       Organization org, User owner, OppStatus status) {
        Category cat = anyCategory(catRepo);
        Opportunity o = oppRepo.save(Opportunity.builder()
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
        if (oppDocRepo != null) {
            oppDocRepo.save(OpportunityDocument.builder()
                    .opportunity(o)
                    .docType(OppDocType.PROGRAM_PROOF)
                    .title("Chứng minh chương trình")
                    .fileUrl("https://example.com/program.pdf")
                    .build());
        }
        return o;
    }
}

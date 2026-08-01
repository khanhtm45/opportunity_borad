package com.opportunityboard.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "opportunity_domains")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(OpportunityDomainId.class)
public class OpportunityDomain {
    @Id
    @Column(name = "opp_id")
    private UUID oppId;

    @Id
    @Column(name = "domain_id")
    private UUID domainId;
}

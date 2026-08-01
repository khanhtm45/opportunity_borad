package com.opportunityboard.domain.entity;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class OpportunityDomainId implements Serializable {
    private UUID oppId;
    private UUID domainId;
}

package com.opportunityboard.domain.enums;

/**
 * Trạng thái LƯU của Opportunity (khác với trạng thái HIỂN THỊ dẫn xuất).
 * display_status được tính qua view/servic: OPEN, CLOSING_SOON, EXPIRED, HIDDEN.
 */
public enum OppStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN,
    CLOSED,
    EXPIRED
}

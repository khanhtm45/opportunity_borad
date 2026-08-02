package com.opportunityboard.domain.enums;

public enum OrgVerified {
    PENDING,
    VERIFIED,
    REJECTED,
    /** AI/Admin yêu cầu provider bổ sung / cập nhật hồ sơ */
    NEEDS_UPDATE
}

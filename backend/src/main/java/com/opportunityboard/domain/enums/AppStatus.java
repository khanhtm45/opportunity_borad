package com.opportunityboard.domain.enums;

/**
 * State machine ứng tuyển nội bộ (Mục 2):
 * SUBMITTED -> REVIEWING -> INTERVIEW -> ACCEPTED
 *                               -> REJECTED
 * SUBMITTED/REVIEWING -> WITHDRAWN (SV rút)
 */
public enum AppStatus {
    SUBMITTED,
    REVIEWING,
    INTERVIEW,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}

package com.pitahmzalendo.kejafy.report.entity;

/**
 * PENDING            - filed, sitting in the admin review queue.
 * ACTION_TAKEN       - admin agreed something was wrong (listing taken down, agent warned/suspended, etc).
 * DISMISSED          - admin reviewed it and found nothing actionable.
 */
public enum ReportStatus {
    PENDING,
    ACTION_TAKEN,
    DISMISSED
}

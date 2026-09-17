package com.fintecture.demobank

/** The tester's choice. */
data class Outcome(
    val sessionStatus: String,
    val transferState: String,
    val transferReason: String,
)

/**
 * Mirrors the web consent screen. Verified against the live screen on
 * api.test.fintecture.com on 2026-09-16 rather than read from the source alone.
 */
object Outcomes {
    val SESSION_STATUSES = listOf("payment_created", "payment_pending", DemoBankSession.PAYMENT_UNSUCCESSFUL)

    /**
     * The pairs matter: an unsupported combination returns no payload from the connector and the
     * callback then fails on a missing render, so offering every state against every status would
     * hand the tester a way to break the flow that has nothing to do with their integration.
     *
     * `received` is deliberately absent from payment_created. The web screen only adds it for an
     * internal beneficiary, which this app cannot know, having no payment details by design.
     */
    val TRANSFER_STATES_FOR = mapOf(
        "payment_created" to listOf("completed"),
        "payment_pending" to listOf("pending", "processing"),
        DemoBankSession.PAYMENT_UNSUCCESSFUL to listOf("rejected"),
    )

    /** All 37 the web screen offers, in its order, so a tester can reproduce any rejection. */
    val TRANSFER_REASONS = listOf(
        "amount_exceeds_limit",
        "api_approval",
        "api_cancellation",
        "blocked_account",
        "cancelled",
        "cancelled_order",
        "closed_account",
        "custom_reason",
        "customer",
        "externally_paid",
        "forgiven",
        "fraud_reassessment",
        "fraud_suspected",
        "fraudulent_activity",
        "fraudulent_originated",
        "incorrect_account_number",
        "incorrect_session_amount",
        "insufficient_funds",
        "invalid_execution_date",
        "invalid_file_format",
        "invalid_party",
        "manually_updated",
        "missing_creditor",
        "missing_debtor",
        "multi_signature",
        "no_answer",
        "not_received",
        "order_change",
        "order_rejected",
        "payout_order_failed",
        "pending_timeout",
        "regulatory_reason",
        "session_aborted",
        "technical",
        "too_many_transactions",
        "transaction_forbidden",
        "unknown",
    )

    fun transferStatesFor(sessionStatus: String): List<String> = TRANSFER_STATES_FOR[sessionStatus].orEmpty()
}

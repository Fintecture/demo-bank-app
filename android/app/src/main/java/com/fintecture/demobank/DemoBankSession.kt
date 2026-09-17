package com.fintecture.demobank

import android.net.Uri

/**
 * What the Demo Bank hands us, and what we hand back.
 *
 * The app deliberately holds no payment details: everything it needs to complete the journey is in
 * the link it was opened with, plus the outcome the tester picks. There is no API to call.
 */
data class DemoBankSession(
    /** Scope-prefixed state, e.g. `PIS:0123...`, exactly as received. */
    val state: String,
    val code: String,
    /** api-gateway origin, taken from the link so the app needs no environment configuration. */
    val origin: String,
) {
    /**
     * The URL that completes the payment. The web consent screen posts these same fields; driving
     * the same callback with the same fields is what lets a native screen replace it.
     *
     * Must NOT be the Demo Bank path this app claims as an app link. Confirm hands this URL to the
     * OS, and a claimed URL resolves back to this app instead of the browser, stranding the payer
     * on the loader with the payment never completed.
     */
    fun callbackUrl(outcome: Outcome): String =
        Uri.parse("$origin/oauth/callback").buildUpon()
            .appendQueryParameter("code", code)
            .appendQueryParameter("state", state)
            .appendQueryParameter("_step", "2")
            .appendQueryParameter("session_state", outcome.sessionStatus)
            // The web form sends the transfer state on every outcome, rejection included.
            .appendQueryParameter("transfer_state", outcome.transferState)
            .apply {
                if (outcome.sessionStatus == PAYMENT_UNSUCCESSFUL) {
                    appendQueryParameter("transfer_reason", outcome.transferReason)
                }
            }
            .build()
            .toString()

    companion object {
        const val PAYMENT_UNSUCCESSFUL = "payment_unsuccessful"

        /**
         * The path this app claims as an app link, mirroring DEMO_BANK_AUTH_PATH in
         * provider-gateway and api-gateway. Nothing the app itself navigates to may use it.
         */
        const val DEMO_BANK_AUTH_PATH = "/demo-bank/auth"

        /** Only PIS is handled natively; the Demo Bank also issues AIS links on the same path. */
        const val PIS_PREFIX = "PIS:"

        /**
         * The api-gateway origins this app will talk to. An exported activity is reachable by any
         * app on the device, so the origin is allowlisted rather than trusted: without this a
         * crafted `ftedemobank://auth?origin=...` link would make the phone drive a request of the
         * caller's choosing. Production is absent because the route is not mounted there.
         */
        val ALLOWED_ORIGINS = setOf(
            "https://api.test.fintecture.com",
            "https://api.sandbox.fintecture.com",
            // Legacy name for the same sandbox host, still emitted by service discovery.
            "https://api-sandbox.fintecture.com",
        )

        /**
         * @return the session, or null when the link is not a PIS Demo Bank link this app can serve.
         */
        fun from(uri: Uri?): DemoBankSession? {
            if (uri == null) return null

            val state = uri.getQueryParameter("state")?.takeIf { it.startsWith(PIS_PREFIX) } ?: return null
            if (state.removePrefix(PIS_PREFIX).isBlank()) return null
            // `$` segments are promoted into callback parameters downstream, so a state carrying
            // one is not something to forward verbatim.
            if (state.contains('$')) return null

            val code = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() } ?: return null

            val origin = when (uri.scheme) {
                // The https link tells us the environment by construction.
                "https" -> "https://${uri.host}"
                // A custom scheme carries no host, so it must name one, and it is not trusted.
                else -> uri.getQueryParameter("origin")
            } ?: return null

            return if (origin in ALLOWED_ORIGINS) {
                DemoBankSession(state = state, code = code, origin = origin)
            } else {
                null
            }
        }
    }
}

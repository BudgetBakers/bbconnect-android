// Return-link parsing (DESIGN.md §8.2): the hosted flow's final redirect
// appends `connectionId`, `resultCode` (Ok|Error|Cancelled), `sessionId` and
// `error` (on failure) to the partner's returnUrl — an https app link or a
// custom scheme. Pinned by the language-neutral vectors in
// contract-tests/fixtures/return-url.json (shared with the Swift Link SDK).
//
// Deliberately built on java.net (not android.net.Uri) so the parser unit-
// tests on a plain JVM — no Robolectric, no emulator.

package com.budgetbakers.bbconnect

import java.net.URI
import java.net.URLDecoder

/** Result code of a finished hosted connect flow. */
enum class BBConnectResultCode(val wire: String) {
    OK("Ok"),
    ERROR("Error"),
    CANCELLED("Cancelled");

    companion object {
        internal fun fromWire(value: String): BBConnectResultCode? =
            entries.firstOrNull { it.wire == value }
    }
}

/** A parsed BudgetBakers return link. */
data class BBConnectReturnUrl(
    /** The connect session this result belongs to (opaque — never parse). */
    val sessionId: String,
    /** Present when a connection materialized (resultCode == OK). */
    val connectionId: String?,
    val resultCode: BBConnectResultCode,
    /** Machine-readable failure reason (present on resultCode == ERROR). */
    val error: String?,
) {
    companion object {
        /**
         * Parse a candidate URL. Returns null when the URL is not a
         * BudgetBakers return link: `sessionId` and a valid `resultCode` are
         * mandatory, any scheme/host/path is accepted (https app links and
         * custom schemes), unrelated partner query params are ignored.
         */
        @JvmStatic
        fun parse(url: String): BBConnectReturnUrl? {
            val query =
                try {
                    URI(url).rawQuery ?: return null
                } catch (_: Exception) {
                    return null
                }

            val params = HashMap<String, String>()
            for (pair in query.split('&')) {
                if (pair.isEmpty()) continue
                val eq = pair.indexOf('=')
                val name = if (eq >= 0) pair.substring(0, eq) else pair
                val raw = if (eq >= 0) pair.substring(eq + 1) else ""
                // First occurrence wins; percent-decoding per application/x-www-form-urlencoded.
                if (name !in params) {
                    params[name] = URLDecoder.decode(raw, Charsets.UTF_8.name())
                }
            }

            val sessionId = params["sessionId"]?.takeIf { it.isNotEmpty() } ?: return null
            val resultCode = params["resultCode"]?.let(BBConnectResultCode::fromWire) ?: return null

            return BBConnectReturnUrl(
                sessionId = sessionId,
                connectionId = params["connectionId"],
                resultCode = resultCode,
                error = params["error"],
            )
        }
    }
}

/** Outcome delivered to the partner app's callbacks. */
sealed interface BBConnectOutcome {
    /**
     * The user connected a bank; poll `GET /v1/connect-sessions/{sessionId}`
     * server-side for the authoritative state.
     */
    data class Success(val sessionId: String, val connectionId: String?) : BBConnectOutcome

    /** The flow failed (`error` is the machine-readable reason). */
    data class Failure(val sessionId: String, val error: String?) : BBConnectOutcome

    /**
     * The user cancelled — in the flow, or by closing the Custom Tab
     * (sessionId is null in the tab-dismissal case).
     */
    data class Cancelled(val sessionId: String?) : BBConnectOutcome

    companion object {
        /** Map a parsed return link onto the outcome callbacks. */
        @JvmStatic
        fun from(returnUrl: BBConnectReturnUrl): BBConnectOutcome =
            when (returnUrl.resultCode) {
                BBConnectResultCode.OK ->
                    Success(returnUrl.sessionId, returnUrl.connectionId)
                BBConnectResultCode.ERROR ->
                    Failure(returnUrl.sessionId, returnUrl.error)
                BBConnectResultCode.CANCELLED ->
                    Cancelled(returnUrl.sessionId)
            }
    }
}

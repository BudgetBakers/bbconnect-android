// BBConnect.start — hosted connect-flow launcher (DESIGN.md §9.2).
//
// Opens the hostedUrl in a Chrome Custom Tab — NEVER an embedded WebView:
// banks block WebViews and RFC 8252 mandates the system browser for
// third-party authentication. No network calls, no API key: the partner
// backend creates the session (server SDK `connectSessions.create`) and
// hands the opaque hostedUrl to the app.
//
// Return path (both app links and custom schemes): the redirect targets the
// partner's registered returnUrl, whose intent-filter reopens the app —
// forward the received Uri to [BBConnect.handle]. Custom Tabs offer no
// dismissal callback, so tab-dismissal is detected by the launching
// activity's onResume arriving WITHOUT a prior return link — call
// [BBConnect.handleResume] there (see the sample activity).

package com.budgetbakers.bbconnect

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object BBConnect {
    private var pending: ((BBConnectOutcome) -> Unit)? = null
    private var launched = false
    private var resumedOnce = false

    /**
     * Launch the hosted connect flow in a Custom Tab.
     *
     * @param context the launching activity.
     * @param hostedUrl the opaque URL from `POST /v1/connect-sessions`.
     * @param onOutcome exactly one callback per flow — success, failure or
     *   cancelled (including the user closing the tab).
     */
    @JvmStatic
    fun start(context: Context, hostedUrl: String, onOutcome: (BBConnectOutcome) -> Unit) {
        pending = onOutcome
        launched = true
        resumedOnce = false
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(hostedUrl))
    }

    /**
     * Forward a return link (from the intent-filter'd activity / onNewIntent)
     * into the active flow. Returns true when the URL was a BudgetBakers
     * return link and the flow was completed.
     */
    @JvmStatic
    fun handle(uri: Uri): Boolean {
        val callback = pending ?: return false
        val parsed = BBConnectReturnUrl.parse(uri.toString()) ?: return false
        finish(callback, BBConnectOutcome.from(parsed))
        return true
    }

    /**
     * Call from the launching activity's onResume. The FIRST resume after
     * [start] is the tab opening handoff and is ignored; a later resume with
     * no return link received means the user closed the tab → Cancelled.
     */
    @JvmStatic
    fun handleResume() {
        val callback = pending ?: return
        if (!launched) return
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        finish(callback, BBConnectOutcome.Cancelled(sessionId = null))
    }

    /** Cancel the active flow programmatically (delivers Cancelled). */
    @JvmStatic
    fun cancel() {
        val callback = pending ?: return
        finish(callback, BBConnectOutcome.Cancelled(sessionId = null))
    }

    private fun finish(callback: (BBConnectOutcome) -> Unit, outcome: BBConnectOutcome) {
        pending = null
        launched = false
        resumedOnce = false
        callback(outcome)
    }
}

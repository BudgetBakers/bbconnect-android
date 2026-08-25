// Minimal sample for the BBConnect Link SDK (WP4.4).
//
// The hostedUrl comes from YOUR backend: it calls the server SDK's
// connectSessions.create(returnUrl) with the partner API key — the API key
// never ships in the app.

package com.budgetbakers.bbconnect.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.budgetbakers.bbconnect.BBConnect
import com.budgetbakers.bbconnect.BBConnectOutcome

class SampleActivity : AppCompatActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "Not connected" }
        val button = Button(this).apply {
            text = "Connect a bank"
            setOnClickListener { startFlow() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(button)
        })
        // Cold-start return (the intent-filter reopened the app).
        intent?.data?.let(BBConnect::handle)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Warm return: forward the return link into the active flow.
        intent.data?.let(BBConnect::handle)
    }

    override fun onResume() {
        super.onResume()
        // Tab-dismissal detection: a resume with no return link → onCancel.
        BBConnect.handleResume()
    }

    private fun startFlow() {
        // 1) Ask YOUR backend for a session (never call the partner API here).
        val hostedUrl = "https://aisp-connect.test.bbapi.dev/s/example"

        // 2) Launch the hosted flow in a Custom Tab.
        BBConnect.start(this, hostedUrl) { outcome ->
            status.text = when (outcome) {
                is BBConnectOutcome.Success ->
                    // 3) The authoritative result lives server-side: have the
                    //    backend poll GET /v1/connect-sessions/{sessionId}.
                    "Connected (session ${outcome.sessionId})"
                is BBConnectOutcome.Failure -> "Failed: ${outcome.error ?: "unknown"}"
                is BBConnectOutcome.Cancelled -> "Cancelled"
            }
        }
    }
}

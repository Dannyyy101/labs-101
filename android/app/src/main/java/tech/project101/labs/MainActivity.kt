package tech.project101.labs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tech.project101.labs.auth.AuthException
import tech.project101.labs.core.userMessage
import tech.project101.labs.ui.LabsRoot
import tech.project101.labs.ui.theme.LabsTheme

class MainActivity : ComponentActivity() {
    /** an error of the last sign in, shown on the sign in screen */
    private val signInError = MutableStateFlow<String?>(null)
    private val isCompletingSignIn = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthCallback(intent)

        val container = (application as LabsApp).container
        setContent {
            LabsTheme {
                LabsRoot(container, signInError, isCompletingSignIn)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthCallback(intent)
    }

    /** zitadel redirects to labs101://auth/callback after the login */
    private fun handleAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "labs101" || uri.host != "auth") return
        // handled once, a recreated activity must not exchange the code again
        intent.data = null

        val auth = (application as LabsApp).container.auth
        lifecycleScope.launch {
            isCompletingSignIn.value = true
            signInError.value = null
            try {
                auth.completeSignIn(uri)
            } catch (_: AuthException.InvalidCallback) {
                // closed or a stale redirect, nothing to show
            } catch (e: Exception) {
                signInError.value = e.userMessage
            } finally {
                isCompletingSignIn.value = false
            }
        }
    }
}

package tech.project101.labs.auth

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.project101.labs.R
import tech.project101.labs.core.userMessage
import tech.project101.labs.ui.theme.Labs

/** Sign in at Zitadel with password, passkey or Google, whatever the login page offers. */
@Composable
fun SignInScreen(auth: AuthSession, callbackError: String?, isCompleting: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val busy = isStarting || isCompleting

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text("labs-101", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Melde dich an, um Essen und Trainings zu synchronisieren.",
                textAlign = TextAlign.Center,
                color = Labs.colors.inkMuted,
            )
            (error ?: callbackError)?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Labs.colors.danger, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        isStarting = true
                        error = null
                        try {
                            val url = auth.startSignIn()
                            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, url)
                        } catch (e: Exception) {
                            error = e.userMessage
                        } finally {
                            isStarting = false
                        }
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Anmelden")
            }
        }
    }
}

package tech.project101.labs.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tech.project101.labs.BuildConfig
import tech.project101.labs.api.apis.UserControllerApi
import tech.project101.labs.auth.AuthSession
import tech.project101.labs.core.ApiClient
import tech.project101.labs.core.AppConfig
import tech.project101.labs.core.result
import tech.project101.labs.core.userMessage
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.ConfirmDialog
import tech.project101.labs.ui.components.LabeledRow
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.theme.Labs

/** The signed in user, the backend asks Zitadel for it (see UserController). Name and picture are changed in Zitadel. */
data class UserProfile(val id: String, val name: String?, val email: String?) {
    /** "M" for the placeholder without a picture */
    val initial: String get() = name?.trim()?.firstOrNull()?.uppercase() ?: "?"
}

class ProfileService(api: ApiClient) {
    private val users = api.create<UserControllerApi>()

    suspend fun profile(): UserProfile {
        val dto = users.userGetProfile().result()
        return UserProfile(dto.id, dto.name, dto.email)
    }
}

class ProfileViewModel(private val service: ProfileService, private val auth: AuthSession) : ViewModel() {
    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                profile = service.profile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage
            }
        }
    }

    fun signOut() = auth.signOut()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(modifier: Modifier) {
    val model = labsViewModel { ProfileViewModel(it.profileService, it.auth) }
    val context = LocalContext.current
    var confirmSignOut by remember { mutableStateOf(false) }
    val profile = model.profile

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = { Text("Profil", fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LabsSection {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Labs.colors.brand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(profile?.initial.orEmpty(), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(profile?.name ?: if (model.error != null) "Profil nicht geladen" else "…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        (profile?.email ?: model.error)?.let { Text(it, color = Labs.colors.inkMuted) }
                    }
                }
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.profileEditUrl))) },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Name und Bild ändern") }
            }

            LabsSection(title = "App") {
                LabeledRow("Version", BuildConfig.VERSION_NAME)
                LabeledRow("Server", AppConfig.apiBaseUrl.removePrefix("https://").removePrefix("http://"))
            }

            LabsSection {
                TextButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Abmelden", color = Labs.colors.danger)
                }
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Abmelden?",
            confirmLabel = "Abmelden",
            onConfirm = model::signOut,
            onDismiss = { confirmSignOut = false },
        )
    }
}

package tech.project101.labs

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import okhttp3.OkHttpClient
import tech.project101.labs.auth.AuthSession
import tech.project101.labs.auth.TokenStore
import tech.project101.labs.core.ApiClient
import tech.project101.labs.food.FoodService
import tech.project101.labs.overview.OverviewService
import tech.project101.labs.profile.ProfileService
import tech.project101.labs.workout.WorkoutService
import java.util.concurrent.TimeUnit

class LabsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The services of the app, created once. */
class AppContainer(context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    val auth = AuthSession(context, TokenStore(context), http)
    private val api = ApiClient(http, auth)

    val foodService = FoodService(api)
    val workoutService = WorkoutService(api)
    val overviewService = OverviewService(api)
    val profileService = ProfileService(api)
}

/** A view model of the current screen that gets the services of the app. */
@Composable
inline fun <reified VM : ViewModel> labsViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as LabsApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

package tech.project101.labs.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import tech.project101.labs.AppContainer
import tech.project101.labs.auth.SignInScreen
import tech.project101.labs.core.AppConfig
import tech.project101.labs.food.AddFoodScreen
import tech.project101.labs.food.FoodEntryScreen
import tech.project101.labs.food.FoodTrackerScreen
import tech.project101.labs.food.Meal
import tech.project101.labs.overview.OverviewScreen
import tech.project101.labs.profile.ProfileScreen
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.workout.WorkoutDetailScreen
import tech.project101.labs.workout.WorkoutSessionScreen
import tech.project101.labs.workout.WorkoutsScreen

// the screens on top of the tabs

@Serializable
object HomeRoute

@Serializable
data class AddFoodRoute(val meal: String)

/** exactly one of the ids is set: a food to track, an open food to track or a tracked food to change */
@Serializable
data class FoodEntryRoute(
    val meal: String,
    val foodId: Long? = null,
    val openFoodId: Long? = null,
    val trackedFoodId: Long? = null,
)

@Serializable
data class WorkoutSessionRoute(val id: Long)

@Serializable
data class WorkoutDetailRoute(val id: Long)

@Composable
fun LabsRoot(container: AppContainer, signInError: StateFlow<String?>, isCompletingSignIn: StateFlow<Boolean>) {
    val signedIn by container.auth.isSignedIn.collectAsState()
    val error by signInError.collectAsState()
    val completing by isCompletingSignIn.collectAsState()

    when {
        !AppConfig.isComplete -> Scaffold { padding ->
            EmptyState(
                Icons.Filled.VpnKey,
                "Nicht konfiguriert",
                "Trage AUTH_ISSUER und AUTH_CLIENT_ID in android/secrets.properties ein und baue die App neu.",
                modifier = Modifier.padding(padding),
            )
        }
        !signedIn -> SignInScreen(container.auth, error, completing)
        else -> LabsNavHost()
    }
}

@Composable
private fun LabsNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeTabs(nav) }
        composable<AddFoodRoute> { entry ->
            val route = entry.toRoute<AddFoodRoute>()
            AddFoodScreen(
                initialMeal = Meal.valueOf(route.meal),
                onBack = { nav.popBackStack() },
                onSelect = { route -> nav.navigate(route) },
                onTracked = { nav.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<FoodEntryRoute> { entry ->
            FoodEntryScreen(
                route = entry.toRoute(),
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<WorkoutSessionRoute> { entry ->
            WorkoutSessionScreen(id = entry.toRoute<WorkoutSessionRoute>().id, onClose = { nav.popBackStack() })
        }
        composable<WorkoutDetailRoute> { entry ->
            WorkoutDetailScreen(id = entry.toRoute<WorkoutDetailRoute>().id, onBack = { nav.popBackStack() })
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Übersicht", Icons.Filled.BarChart),
    FOOD("Food", Icons.Filled.Restaurant),
    TRAINING("Training", Icons.Filled.FitnessCenter),
    PROFILE("Profil", Icons.Filled.Person),
}

@Composable
private fun HomeTabs(nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(Tab.FOOD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        when (tab) {
            Tab.OVERVIEW -> OverviewScreen(modifier)
            Tab.FOOD -> FoodTrackerScreen(
                modifier,
                onAdd = { meal -> nav.navigate(AddFoodRoute(meal.name)) },
                onEdit = { entry -> nav.navigate(FoodEntryRoute(meal = entry.meal.name, trackedFoodId = entry.id)) },
            )
            Tab.TRAINING -> WorkoutsScreen(
                modifier,
                onOpenSession = { id -> nav.navigate(WorkoutSessionRoute(id)) },
                onOpenDetail = { id -> nav.navigate(WorkoutDetailRoute(id)) },
            )
            Tab.PROFILE -> ProfileScreen(modifier)
        }
    }
}

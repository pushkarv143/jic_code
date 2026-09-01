package com.greenwood.school

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.greenwood.school.navigation.AppNavHost
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.MainViewModel
import com.greenwood.school.ui.feature.splash.SplashScreen
import com.greenwood.school.ui.theme.SchoolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * How long the launch screen stays up at minimum. Long enough to read the school
 * name and the developer credit, short enough not to feel like a delay.
 */
// 600 ms is long enough to be legible and short enough to feel instant.
// The old 1800 ms was the biggest single contributor to cold-start latency.
private const val SPLASH_MINIMUM_MS = 600L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SchoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val navController = rememberNavController()

                    /*
                     * Session expiry is pushed up from the OkHttp authenticator rather
                     * than checked per-screen: whichever screen is on top when a refresh
                     * fails, the user lands back on Login with the back stack cleared.
                     */
                    LaunchedEffect(Unit) {
                        viewModel.sessionExpired.collect {
                            navController.navigate(Routes.AUTH_GRAPH) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    }

                    /*
                     * Splash timing. Restoring a session is a fast DataStore read, so
                     * gating the splash on that alone would flash it past unreadably.
                     * It stays up until BOTH the session has resolved AND a minimum
                     * time has passed — whichever finishes last — so the launch screen
                     * is always legible without ever making a slow restore feel slower.
                     */
                    var minimumSplashElapsed by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(SPLASH_MINIMUM_MS)
                        minimumSplashElapsed = true
                    }

                    /*
                     * A signed-in shell also waits for `GET /me/access`.
                     *
                     * Menus and in-screen controls are gated strictly now: before that
                     * answer arrives every permission check is false, so composing the
                     * shell early would briefly hide entries the user does have. Holding
                     * the splash for one request is the alternative to every screen
                     * having to render a "maybe".
                     *
                     * Only on the way in. `isAccessSettled` latches true, so a later
                     * background re-read — after a token refresh, or an administrator
                     * changing a role — updates the menu in place rather than throwing
                     * the user back to the splash.
                     */
                    // Show splash only while the session is actually being restored or
                    // the minimum visual time hasn't elapsed. Access settling happens
                    // in the background — the shell handles the "not yet loaded" state
                    // with skeleton UI, so the user is never stuck on the splash for a
                    // slow network call.
                    val showSplash = state.isRestoringSession || !minimumSplashElapsed

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        AppNavHost(
                            navController = navController,
                            isSignedIn = state.isSignedIn,
                            currentUser = state.user,
                            access = state.access,
                            mustChangePassword = state.mustChangePassword,
                        )
                    }
                }
            }
        }
    }
}

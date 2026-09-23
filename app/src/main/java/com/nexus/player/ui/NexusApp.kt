package com.nexus.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.player.AppLaunchState
import com.nexus.player.MainViewModel
import com.nexus.player.core.navigation.TopLevelDestination
import com.nexus.player.core.navigation.navigateToTopLevelDestination
import com.nexus.player.core.ui.component.NexusBottomBar
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.navigation.NexusNavHost

/**
 * Root Application Composable for Nexus Player.
 *
 * Coordinates top-level bottom navigation, dynamic idempotent start destination routing
 * via MainViewModel, and hides bottom navigation on deep screens (Onboarding, Settings).
 */
@Composable
fun NexusApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val launchState by mainViewModel.appLaunchState.collectAsStateWithLifecycle()

    when (val state = launchState) {
        is AppLaunchState.Loading -> {
            NexusLoadingIndicator(modifier = modifier)
        }
        is AppLaunchState.Ready -> {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = currentBackStackEntry?.destination

            val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { destination ->
                currentDestination?.hasRoute(destination.route::class) == true
            }

            val isTopLevelDestination = currentTopLevelDestination != null

            Box(
                modifier = modifier.fillMaxSize()
            ) {
                NexusNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    startDestination = state.startDestination
                )

                if (isTopLevelDestination) {
                    NexusBottomBar(
                        destinations = TopLevelDestination.entries,
                        currentDestination = currentTopLevelDestination,
                        onNavigateToDestination = { destination ->
                            navController.navigateToTopLevelDestination(destination)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

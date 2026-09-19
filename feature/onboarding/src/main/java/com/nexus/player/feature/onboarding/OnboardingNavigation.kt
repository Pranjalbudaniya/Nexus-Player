package com.nexus.player.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.OnboardingRoute

/**
 * Registers the type-safe Onboarding destination on the NavGraph.
 */
fun NavGraphBuilder.onboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    composable<OnboardingRoute> {
        OnboardingRoute(onOnboardingFinished = onOnboardingFinished)
    }
}

package io.silencio.app.ui.premium

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.silencio.app.ui.theme.Background

private val PremiumGold = Color(0xFFD4A847)

@Composable
fun PaywallScreen(
    onPurchase: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }

    BackHandler(enabled = page > 0) {
        page--
    }

    val progress by animateFloatAsState(
        targetValue = when (page) {
            0 -> 0.33f
            1 -> 0.66f
            else -> 1f
        },
        animationSpec = tween(500),
        label = "paywall_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // progress bar — thin, at the top
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            color = PremiumGold,
            trackColor = Color(0xFF2A2510),
            strokeCap = StrokeCap.Round
        )

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "paywall_page"
        ) { currentPage ->
            when (currentPage) {
                0 -> PaywallPage1(
                    onNext = { page = 1 },
                    onSkip = onSkip
                )

                1 -> PaywallPage2(
                    onNext = { page = 2 },
                    onSkip = onSkip
                )

                2 -> PaywallPage3(
                    onPurchase = {
                        viewModel.setPremium(true)
                        onPurchase()
                    },
                    onSkip = onSkip
                )
            }
        }
    }
}
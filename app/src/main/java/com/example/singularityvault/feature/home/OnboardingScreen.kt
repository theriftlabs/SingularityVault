package com.riftlabs.singularityvault.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.riftlabs.singularityvault.ui.theme.GradientBackground
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Security,
            title = "Encrypted by design",
            description = "Your data is encrypted locally. Forget the master password and the data is permanently lost."
        ),
        OnboardingPage(
            icon = Icons.Default.Fingerprint,
            title = "Biometric unlock",
            description = "Use fingerprint or face unlock for faster access. You can adjust this in settings."
        ),
        OnboardingPage(
            icon = Icons.Default.Timer,
            title = "Idle timeout",
            description = "The vault locks automatically when you are inactive. You can adjust this in settings."
        ),
        OnboardingPage(
            icon = Icons.Default.Apps,
            title = "Background locking",
            description = "Switching apps can lock the vault to prevent unauthorized access. You can adjust this in settings."
        ),
        OnboardingPage(
            icon = Icons.Default.ContentCopy,
            title = "Clipboard protection",
            description = "Copied passwords are cleared automatically to reduce exposure. You can adjust this in settings."
        ),
        OnboardingPage(
            icon = Icons.Default.AutoAwesome,
            title = "Stronger passwords",
            description = "Weak passwords are flagged and strong alternatives can be generated locally."
        )
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()  // Handle system bars for edge-to-edge
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val item = pages[page]

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage - 1
                                )
                            }
                        }
                    ) {
                        Text("Back")
                    }
                } else {
                    TextButton(
                        onClick = onComplete
                    ) {
                        Text("Skip")
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage == pages.lastIndex) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1
                                )
                            }
                        }
                    }
                ) {
                    Text(
                        if (pagerState.currentPage == pages.lastIndex)
                            "Done"
                        else
                            "Next"
                    )
                }
            }
        }
    }
}
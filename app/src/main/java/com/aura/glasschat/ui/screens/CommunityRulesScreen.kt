package com.aura.glasschat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.components.BuddysCard
import com.aura.glasschat.ui.components.BuddysTopBar
import com.aura.glasschat.ui.theme.BuddysTheme

@Composable
fun CommunityRulesScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            BuddysTopBar(
                title = "Community Rules",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Buddies Guidelines",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Buddies is designed for genuine connections and private messaging with close friends. To keep our community safe, clean, and respectful, please follow these guidelines:",
                    style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                val rules = listOf(
                    Pair("1. Be respectful.", "Treat everyone with kindness, respect, and positive energy."),
                    Pair("2. No harassment.", "Do not bully, stalk, insult, or threaten anyone on the platform."),
                    Pair("3. No impersonation.", "Do not pretend to be another person or create deceptive identities."),
                    Pair("4. No spam.", "Do not spam messages, send unsolicited promotions, or blast bulk links."),
                    Pair("5. Protect privacy.", "Never share private information, photos, or confidential data without explicit consent."),
                    Pair("6. No scams.", "Do not attempt fraud, phishing, or financial manipulation."),
                    Pair("7. Respect blocks & privacy.", "Honor account privacy settings and do not attempt to circumvent blocks."),
                    Pair("8. Report harmful behavior.", "Use the in-app report tools to flag violations and keep Buddies secure.")
                )

                rules.forEach { (title, desc) ->
                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.primaryRed,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BuddysTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

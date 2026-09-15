package com.aura.glasschat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.EditProfileViewModel
import com.aura.glasschat.ui.viewmodel.UsernameCheckStatus

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(context, uri)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Bar
            BuddysTopBar(
                title = "Edit Profile",
                onBack = onBack,
                actions = {
                    BuddysButton(
                        text = "Save",
                        onClick = { viewModel.saveProfile() },
                        enabled = !uiState.isSaving && (uiState.username == uiState.originalUsername || uiState.usernameStatus == UsernameCheckStatus.AVAILABLE),
                        isLoading = uiState.isSaving,
                        modifier = Modifier.height(36.dp)
                    )
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Profile Picture Section
                Spacer(modifier = Modifier.height(12.dp))
                Box(contentAlignment = Alignment.Center) {
                    AvatarView(
                        imageUrl = uiState.avatarUrl,
                        displayName = uiState.displayName.ifBlank { "Me" },
                        size = 96.dp,
                        isOnline = false,
                        isEditable = true,
                        onEditClick = { photoPickerLauncher.launch("image/*") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Change profile picture",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = BuddysTheme.colors.primaryRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                if (uiState.isUploadingPhoto) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = BuddysTheme.colors.primaryRed
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Main Information Card
                BuddysCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Name Field
                        EditProfileFieldContainer(
                            label = "Name",
                            icon = Icons.Default.Person
                        ) {
                            BasicTextFieldNoBorder(
                                value = uiState.displayName,
                                onValueChange = { viewModel.onDisplayNameChanged(it) },
                                placeholder = "Add your name"
                            )
                        }

                        HorizontalDivider(color = BuddysTheme.colors.border, thickness = 0.8.dp)

                        // Username Field
                        EditProfileFieldContainer(
                            label = "Username",
                            icon = Icons.Default.AlternateEmail
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                BasicTextFieldNoBorder(
                                    value = uiState.username,
                                    onValueChange = { viewModel.onUsernameChanged(it) },
                                    placeholder = "username"
                                )

                                // Status Indicator
                                if (uiState.usernameStatusMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        when (uiState.usernameStatus) {
                                            UsernameCheckStatus.AVAILABLE -> {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BuddysTheme.colors.success, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(uiState.usernameStatusMessage!!, color = BuddysTheme.colors.success, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            UsernameCheckStatus.TAKEN, UsernameCheckStatus.INVALID -> {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(uiState.usernameStatusMessage!!, color = BuddysTheme.colors.primaryRed, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            UsernameCheckStatus.CHECKING -> {
                                                CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.2.dp, color = BuddysTheme.colors.primaryRed)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(uiState.usernameStatusMessage!!, color = BuddysTheme.colors.textSecondary, fontSize = 11.5.sp)
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BuddysTheme.colors.border, thickness = 0.8.dp)

                        // Pronouns Field
                        EditProfileFieldContainer(
                            label = "Pronouns",
                            icon = Icons.Default.Badge
                        ) {
                            BasicTextFieldNoBorder(
                                value = uiState.pronouns,
                                onValueChange = { viewModel.onPronounsChanged(it) },
                                placeholder = "Add pronouns (e.g. he/him, she/her)"
                            )
                        }

                        HorizontalDivider(color = BuddysTheme.colors.border, thickness = 0.8.dp)

                        // Bio Field (with Character Counter)
                        EditProfileFieldContainer(
                            label = "Bio",
                            icon = Icons.Default.EditNote
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                BasicTextFieldNoBorder(
                                    value = uiState.bio,
                                    onValueChange = {
                                        if (it.length <= 150) viewModel.onBioChanged(it)
                                    },
                                    placeholder = "Write a short bio...",
                                    singleLine = false,
                                    maxLines = 4
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.bio.length} / 150",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (uiState.bio.length >= 140) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textMuted,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }

                        HorizontalDivider(color = BuddysTheme.colors.border, thickness = 0.8.dp)

                        // Link Field
                        EditProfileFieldContainer(
                            label = "Links",
                            icon = Icons.Default.Link
                        ) {
                            BasicTextFieldNoBorder(
                                value = uiState.link,
                                onValueChange = { viewModel.onLinkChanged(it) },
                                placeholder = "Add link (e.g. instagram.com/username)"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Personal & Identity Preferences
                BuddysCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Gender / Category
                        EditProfileFieldContainer(
                            label = "Gender",
                            icon = Icons.Default.Wc
                        ) {
                            BasicTextFieldNoBorder(
                                value = uiState.gender,
                                onValueChange = { viewModel.onGenderChanged(it) },
                                placeholder = "Male / Female / Custom"
                            )
                        }

                        HorizontalDivider(color = BuddysTheme.colors.border, thickness = 0.8.dp)

                        // Account Privacy Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private account",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Only approved friends can view your profile and stories",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BuddysTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Switch(
                                checked = uiState.isPrivate,
                                onCheckedChange = { viewModel.onPrivacyToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BuddysTheme.colors.surface,
                                    checkedTrackColor = BuddysTheme.colors.primaryRed,
                                    uncheckedThumbColor = BuddysTheme.colors.surface,
                                    uncheckedTrackColor = BuddysTheme.colors.border
                                )
                            )
                        }
                    }
                }

                // Error Message Display
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.errorMessage!!,
                        color = BuddysTheme.colors.primaryRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun EditProfileFieldContainer(
    label: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BuddysTheme.colors.primaryRed,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun BasicTextFieldNoBorder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textMuted,
                    fontSize = 14.5.sp
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                color = BuddysTheme.colors.textPrimary
            ),
            cursorBrush = SolidColor(BuddysTheme.colors.primaryRed)
        )
    }
}


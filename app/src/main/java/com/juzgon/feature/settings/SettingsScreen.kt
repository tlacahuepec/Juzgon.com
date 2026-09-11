@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.BuildMetadata
import com.juzgon.feature.about.AboutViewModel
import com.juzgon.feature.backup.ExportBackupViewModel
import com.juzgon.ui.theme.JuzgonVisualTheme
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    onNavigateToGeminiKey: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    exportViewModel: ExportBackupViewModel = hiltViewModel(),
    aboutViewModel: AboutViewModel = hiltViewModel(),
    geminiViewModel: GeminiKeySettingsViewModel = hiltViewModel(),
) {
    val exportState by exportViewModel.state.collectAsState()
    val geminiState by geminiViewModel.state.collectAsState()
    val metadata = remember { aboutViewModel.metadata }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val safLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                val json = exportState.exportedJson ?: return@rememberLauncherForActivityResult
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    exportViewModel.onExportConsumed()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Backup exported successfully")
                    }
                } catch (e: IOException) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Failed to export backup: ${e.message}")
                    }
                }
            } else {
                exportViewModel.onExportConsumed()
            }
        }

    LaunchedEffect(exportState.isExportComplete) {
        if (exportState.isExportComplete && exportState.exportedJson != null) {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            safLauncher.launch("juzgon-backup-$dateStr.json")
        }
    }

    LaunchedEffect(exportState.errorMessage) {
        exportState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            exportViewModel.onExportConsumed()
        }
    }

    SettingsScreen(
        buildMetadata = metadata,
        geminiKeyState = geminiState.keyState,
        maskedGeminiKey = geminiState.maskedKey,
        isExporting = exportState.isExporting,
        onExportBackup = { exportViewModel.export() },
        onNavigateToGeminiKey = onNavigateToGeminiKey,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    buildMetadata: BuildMetadata,
    geminiKeyState: GeminiKeyState,
    maskedGeminiKey: String?,
    isExporting: Boolean,
    onExportBackup: () -> Unit,
    onNavigateToGeminiKey: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val tokens = JuzgonVisualTheme.tokens
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = tokens.palette.textStrong,
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Back"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = tokens.palette.textStrong,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = tokens.palette.baseBackground,
                        titleContentColor = tokens.palette.textStrong,
                    ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(tokens.spacing.large),
        ) {
            DatabaseBackupSection(
                isExporting = isExporting,
                onExportBackup = onExportBackup,
            )

            GeminiAiSection(
                geminiKeyState = geminiKeyState,
                maskedGeminiKey = maskedGeminiKey,
                onNavigateToGeminiKey = onNavigateToGeminiKey,
            )

            AboutSection(
                buildMetadata = buildMetadata,
            )

            Spacer(modifier = Modifier.height(tokens.spacing.extraLarge))
        }
    }
}

@Composable
private fun DatabaseBackupSection(
    isExporting: Boolean,
    onExportBackup: () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens

    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        Text(
            text = "Database & Backup",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.palette.contrastAccent,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(tokens.gradients.heroSurface),
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).border(
                        width = 1.dp,
                        color = tokens.palette.panelBackground,
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).padding(tokens.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                Text(
                    text =
                        "Export database backup as a JSON file containing all categories, " +
                            "items, and custom score profiles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.palette.textSoft,
                )

                Button(
                    onClick = onExportBackup,
                    enabled = !isExporting,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = tokens.palette.primaryGlow,
                            contentColor = tokens.palette.baseBackground,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 48.dp)
                            .semantics {
                                contentDescription = "Export backup"
                                role = Role.Button
                            },
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = tokens.palette.baseBackground,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(tokens.spacing.small))
                        Text(text = "Exporting...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(tokens.spacing.small))
                        Text(text = "Export backup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiAiSection(
    geminiKeyState: GeminiKeyState,
    maskedGeminiKey: String?,
    onNavigateToGeminiKey: () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens

    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        Text(
            text = "Gemini AI Configuration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.palette.contrastAccent,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(tokens.gradients.heroSurface),
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).border(
                        width = 1.dp,
                        color = tokens.palette.panelBackground,
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).padding(tokens.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                val statusText =
                    if (geminiKeyState == GeminiKeyState.CONFIGURED) {
                        "Key configured (${maskedGeminiKey ?: "active"})"
                    } else {
                        "No API key configured. AI autofill and scoring features are unavailable."
                    }

                val statusColor =
                    if (geminiKeyState == GeminiKeyState.CONFIGURED) {
                        tokens.palette.primaryGlow
                    } else {
                        tokens.palette.textMuted
                    }
                val statusWeight =
                    if (geminiKeyState == GeminiKeyState.CONFIGURED) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = statusWeight,
                )

                OutlinedButton(
                    onClick = onNavigateToGeminiKey,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = tokens.palette.contrastAccent,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 48.dp)
                            .semantics {
                                contentDescription = "Configure Gemini Key"
                                role = Role.Button
                            },
                ) {
                    Text(text = "Configure Gemini Key", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AboutSection(buildMetadata: BuildMetadata) {
    val tokens = JuzgonVisualTheme.tokens

    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        Text(
            text = "About Juzgón",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.palette.contrastAccent,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(tokens.gradients.heroSurface),
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).border(
                        width = 1.dp,
                        color = tokens.palette.panelBackground,
                        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
                    ).semantics {
                        contentDescription = "About"
                    }.padding(tokens.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = tokens.palette.primaryGlow,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Version ${buildMetadata.versionName} (${buildMetadata.versionCode})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.palette.textStrong,
                    )
                }

                Text(
                    text = "Channel: ${buildMetadata.channel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.textSoft,
                )

                Text(
                    text = "Build: ${buildMetadata.gitSha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.textMuted,
                )

                Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))

                Text(
                    text = "Local-first luminous catalog & evaluation engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.textSoft,
                )
            }
        }
    }
}

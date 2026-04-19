package ai.wenjuanpro.app.ui.screens.configlist

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.configlist.ConfigCardUiModel
import ai.wenjuanpro.app.feature.configlist.ConfigListEffect
import ai.wenjuanpro.app.feature.configlist.ConfigListIntent
import ai.wenjuanpro.app.feature.configlist.ConfigListUiState
import ai.wenjuanpro.app.feature.configlist.ConfigListViewModel
import ai.wenjuanpro.app.feature.configlist.ErrorSheetState
import ai.wenjuanpro.app.ui.components.ErrorSheet
import ai.wenjuanpro.app.ui.components.HiddenLongPressArea
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object ConfigListScreenTags {
    const val ROOT = "config_list_root"
    const val LOADING = "config_list_loading"
    const val EMPTY = "config_list_empty"
    const val TIMEOUT = "config_list_timeout"
    const val ERROR = "config_list_error"
    const val SUCCESS_LIST = "config_list_success_list"
    const val ALL_INVALID_BANNER = "config_list_all_invalid_banner"
    const val REFRESH_BUTTON = "config_list_refresh_button"
    const val RETRY_BUTTON = "config_list_retry_button"
    const val EMPTY_REFRESH_BUTTON = "config_list_empty_refresh_button"
    const val HIDDEN_AREA = "config_list_hidden_area"
    const val SCAN_BUTTON = "config_list_scan_button"
    const val CHECK_CONFIG_BUTTON = "config_list_check_config_button"
    const val DIAG_DIALOG = "config_list_diag_dialog"
}

@Composable
fun ConfigListScreen(
    onNavigateToScan: (configId: String) -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfigListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConfigListEffect.NavigateToScan -> onNavigateToScan(effect.configId)
                ConfigListEffect.NavigateToDiagnostics -> onNavigateToDiagnostics()
            }
        }
    }

    ConfigListContent(
        state = state,
        sheetState = sheetState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

private val HeroStart = Color(0xFF1E88E5)
private val HeroEnd = Color(0xFF1565C0)

@Composable
fun ConfigListContent(
    state: ConfigListUiState,
    sheetState: ErrorSheetState?,
    onIntent: (ConfigListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiag by remember { mutableStateOf(false) }

    if (sheetState != null) {
        BackHandler(enabled = true) { onIntent(ConfigListIntent.DismissSheet) }
    }

    val cards = (state as? ConfigListUiState.Success)?.cards.orEmpty()
    val firstValidConfigId = cards.firstOrNull { it.isValid }?.configId
    val isLoading = state is ConfigListUiState.Loading

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(HeroStart, HeroEnd)))
                .testTag(ConfigListScreenTags.ROOT),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_placeholder),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(86.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.config_list_hero_greeting),
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.config_list_hero_brand),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_tagline),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    firstValidConfigId?.let { onIntent(ConfigListIntent.CardClicked(it)) }
                },
                enabled = !isLoading && firstValidConfigId != null,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = HeroEnd,
                        disabledContainerColor = Color.White.copy(alpha = 0.45f),
                        disabledContentColor = HeroEnd.copy(alpha = 0.7f),
                    ),
                shape = RoundedCornerShape(32.dp),
                modifier =
                    Modifier
                        .widthIn(min = 260.dp)
                        .heightIn(min = 64.dp)
                        .testTag(ConfigListScreenTags.SCAN_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.welcome_scan_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))
            StatusLine(state = state)

            Spacer(Modifier.height(36.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallPillButton(
                    icon = Icons.Filled.Refresh,
                    label = stringResource(R.string.config_list_refresh_button),
                    enabled = !isLoading,
                    testTag = ConfigListScreenTags.REFRESH_BUTTON,
                    onClick = { onIntent(ConfigListIntent.Refresh) },
                )
                SmallPillButton(
                    icon = Icons.Filled.CheckCircle,
                    label = stringResource(R.string.welcome_check_config),
                    enabled = true,
                    testTag = ConfigListScreenTags.CHECK_CONFIG_BUTTON,
                    onClick = { showDiag = true },
                )
            }
        }

        HiddenLongPressArea(
            onTriggered = { onIntent(ConfigListIntent.HiddenAreaLongPressed) },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .testTag(ConfigListScreenTags.HIDDEN_AREA),
        )

        if (sheetState != null) {
            ErrorSheet(
                errors = sheetState.errors,
                onDismiss = { onIntent(ConfigListIntent.DismissSheet) },
            )
        }

        if (showDiag) {
            ConfigDiagnosticsDialog(
                state = state,
                onDismiss = { showDiag = false },
                onRefresh = { onIntent(ConfigListIntent.Refresh) },
                onViewErrors = { onIntent(ConfigListIntent.ViewErrors(it)) },
            )
        }
    }
}

@Composable
private fun StatusLine(state: ConfigListUiState) {
    val msg =
        when (state) {
            ConfigListUiState.Loading -> null
            ConfigListUiState.Empty ->
                stringResource(R.string.welcome_no_valid_config)
            ConfigListUiState.Timeout ->
                stringResource(R.string.config_list_timeout_title)
            is ConfigListUiState.Error -> stringResource(state.messageRes)
            is ConfigListUiState.Success ->
                if (state.cards.none { it.isValid }) {
                    stringResource(R.string.welcome_no_valid_config)
                } else {
                    null
                }
        }

    val showSpinner = state is ConfigListUiState.Loading

    Box(
        modifier = Modifier.heightIn(min = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            showSpinner ->
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp).testTag(ConfigListScreenTags.LOADING),
                )
            msg != null ->
                Text(
                    text = msg,
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
        }
    }
}

@Composable
private fun SmallPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.10f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.45f),
            ),
        border = null,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.heightIn(min = 40.dp).testTag(testTag),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigDiagnosticsDialog(
    state: ConfigListUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onViewErrors: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
                Modifier
                    .widthIn(max = 520.dp)
                    .testTag(ConfigListScreenTags.DIAG_DIALOG),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.welcome_diag_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.config_list_refresh_cd),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier.verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (state) {
                            ConfigListUiState.Loading ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.config_list_empty_title))
                                }
                            ConfigListUiState.Empty ->
                                Text(stringResource(R.string.welcome_diag_empty))
                            ConfigListUiState.Timeout ->
                                Text(stringResource(R.string.config_list_timeout_title))
                            is ConfigListUiState.Error ->
                                Text(stringResource(state.messageRes))
                            is ConfigListUiState.Success -> {
                                if (state.cards.isEmpty()) {
                                    Text(stringResource(R.string.welcome_diag_empty))
                                } else {
                                    state.cards.forEach { card ->
                                        DiagnosticsRow(
                                            card = card,
                                            onViewErrors = { onViewErrors(card.sourceFileName) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.welcome_diag_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(
    card: ConfigCardUiModel,
    onViewErrors: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint =
                if (card.isValid) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
            Icon(
                imageVector = if (card.isValid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = tint,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.sourceFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!card.isValid) {
                TextButton(onClick = onViewErrors) {
                    Text(stringResource(R.string.config_list_view_errors))
                }
            } else {
                val count = card.questionCount
                Text(
                    text =
                        if (count == null) {
                            stringResource(R.string.config_list_question_count_unknown)
                        } else {
                            stringResource(R.string.config_list_question_count, count)
                        },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

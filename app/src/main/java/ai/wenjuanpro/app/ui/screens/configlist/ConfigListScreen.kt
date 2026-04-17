package ai.wenjuanpro.app.ui.screens.configlist

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.configlist.ConfigCardUiModel
import ai.wenjuanpro.app.feature.configlist.ConfigListEffect
import ai.wenjuanpro.app.feature.configlist.ConfigListIntent
import ai.wenjuanpro.app.feature.configlist.ConfigListUiState
import ai.wenjuanpro.app.feature.configlist.ConfigListViewModel
import ai.wenjuanpro.app.feature.configlist.ErrorSheetState
import ai.wenjuanpro.app.ui.components.AssessmentCard
import ai.wenjuanpro.app.ui.components.ErrorSheet
import ai.wenjuanpro.app.ui.components.HiddenLongPressArea
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                ConfigListEffect.NavigateToDiagnostics -> {
                    onNavigateToDiagnostics()
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigListContent(
    state: ConfigListUiState,
    sheetState: ErrorSheetState?,
    onIntent: (ConfigListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sheetState != null) {
        BackHandler(enabled = true) {
            onIntent(ConfigListIntent.DismissSheet)
        }
    }

    Scaffold(
        modifier = modifier.testTag(ConfigListScreenTags.ROOT),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_list_title)) },
                actions = {
                    IconButton(
                        onClick = { onIntent(ConfigListIntent.Refresh) },
                        enabled = state !is ConfigListUiState.Loading,
                        modifier = Modifier.testTag(ConfigListScreenTags.REFRESH_BUTTON),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.config_list_refresh_cd),
                        )
                    }
                    HiddenLongPressArea(
                        onTriggered = { onIntent(ConfigListIntent.HiddenAreaLongPressed) },
                        modifier =
                            Modifier
                                .size(48.dp)
                                .testTag(ConfigListScreenTags.HIDDEN_AREA),
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (state) {
                ConfigListUiState.Loading -> LoadingBody()
                ConfigListUiState.Empty -> EmptyBody(onIntent = onIntent)
                ConfigListUiState.Timeout ->
                    StatusBody(
                        testTag = ConfigListScreenTags.TIMEOUT,
                        messageRes = R.string.config_list_timeout_title,
                        buttonTestTag = ConfigListScreenTags.RETRY_BUTTON,
                        buttonRes = R.string.config_list_retry_button,
                        onClick = { onIntent(ConfigListIntent.Refresh) },
                    )
                is ConfigListUiState.Error ->
                    StatusBody(
                        testTag = ConfigListScreenTags.ERROR,
                        messageRes = state.messageRes,
                        buttonTestTag = ConfigListScreenTags.RETRY_BUTTON,
                        buttonRes = R.string.config_list_retry_button,
                        onClick = { onIntent(ConfigListIntent.Refresh) },
                    )
                is ConfigListUiState.Success ->
                    SuccessBody(
                        cards = state.cards,
                        allInvalid = state.allInvalid,
                        onIntent = onIntent,
                    )
            }

            if (sheetState != null) {
                ErrorSheet(
                    errors = sheetState.errors,
                    onDismiss = { onIntent(ConfigListIntent.DismissSheet) },
                )
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(ConfigListScreenTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBody(onIntent: (ConfigListIntent) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(ConfigListScreenTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.config_list_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onIntent(ConfigListIntent.Refresh) },
            modifier = Modifier.testTag(ConfigListScreenTags.EMPTY_REFRESH_BUTTON),
        ) {
            Text(stringResource(R.string.config_list_refresh_button))
        }
    }
}

@Composable
private fun StatusBody(
    testTag: String,
    messageRes: Int,
    buttonTestTag: String,
    buttonRes: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.testTag(buttonTestTag),
        ) {
            Text(stringResource(buttonRes))
        }
    }
}

@Composable
private fun SuccessBody(
    cards: List<ConfigCardUiModel>,
    allInvalid: Boolean,
    onIntent: (ConfigListIntent) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(ConfigListScreenTags.SUCCESS_LIST),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (allInvalid) {
            item(key = "__all_invalid_banner") {
                AllInvalidBanner()
            }
        }
        items(items = cards, key = { it.sourceFileName }) { card ->
            AssessmentCard(
                model = card,
                onClick = { onIntent(ConfigListIntent.CardClicked(card.configId)) },
                onViewErrors = { onIntent(ConfigListIntent.ViewErrors(card.sourceFileName)) },
            )
        }
    }
}

@Composable
private fun AllInvalidBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(ConfigListScreenTags.ALL_INVALID_BANNER),
    ) {
        Text(
            text = stringResource(R.string.config_list_all_invalid_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

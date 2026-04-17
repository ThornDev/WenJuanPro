package ai.wenjuanpro.app.ui.screens.scan

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.scan.ScanEffect
import ai.wenjuanpro.app.feature.scan.ScanIntent
import ai.wenjuanpro.app.feature.scan.ScanUiState
import ai.wenjuanpro.app.feature.scan.ScanViewModel
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object ScanScreenTags {
    const val ROOT = "scan_screen_root"
    const val PERMISSION_DENIED_BODY = "scan_permission_denied_body"
    const val NO_CAMERA_BODY = "scan_no_camera_body"
    const val LOADING_BODY = "scan_loading_body"
    const val PREVIEW_CONTAINER = "scan_preview_container"
    const val RETRY_PERMISSION_BUTTON = "scan_retry_permission_button"
    const val OPEN_SETTINGS_BUTTON = "scan_open_settings_button"
    const val NO_CAMERA_BACK_BUTTON = "scan_no_camera_back_button"
    const val TOP_BACK_BUTTON = "scan_top_back_button"
    const val SCAN_OVERLAY = "scan_overlay"
    const val TOP_HINT = "scan_top_hint"
    const val BOTTOM_HINT = "scan_bottom_hint"
}

@Composable
fun ScanScreen(
    onNavigateToWelcome: (studentId: String, configId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onIntent(ScanIntent.OnPermissionResult(granted))
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onIntent(ScanIntent.OnEnter)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScanEffect.NavigateToWelcome ->
                    onNavigateToWelcome(effect.studentId, effect.configId)
                ScanEffect.NavigateBack -> onNavigateBack()
                ScanEffect.OpenAppSettings -> {
                    val intent =
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
                ScanEffect.RequestCameraPermission ->
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    ScanContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanContent(
    state: ScanUiState,
    onIntent: (ScanIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state is ScanUiState.Recognized) {
        if (state is ScanUiState.Recognized) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(state.transientSnackbar) {
        val msg = state.transientSnackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        onIntent(ScanIntent.SnackbarShown)
    }

    Scaffold(
        modifier = modifier.testTag(ScanScreenTags.ROOT),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_top_hint)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onIntent(ScanIntent.OnNavigateBack) },
                        modifier = Modifier.testTag(ScanScreenTags.TOP_BACK_BUTTON),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.scan_back_cd),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (state) {
                is ScanUiState.Idle,
                is ScanUiState.CheckingPermission,
                is ScanUiState.Recognized,
                -> LoadingBody()
                is ScanUiState.Preview ->
                    PreviewBody(onIntent = onIntent)
                is ScanUiState.PermissionDenied -> PermissionDeniedBody(state = state, onIntent = onIntent)
                is ScanUiState.NoCamera -> NoCameraBody(onIntent = onIntent)
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
                .testTag(ScanScreenTags.LOADING_BODY),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PreviewBody(onIntent: (ScanIntent) -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(ScanScreenTags.PREVIEW_CONTAINER),
    ) {
        CameraPreviewView(
            onQrDecoded = { content -> onIntent(ScanIntent.OnQrDecoded(content)) },
            onBindFailed = { onIntent(ScanIntent.OnCameraBindFailed) },
        )
        ScanOverlay(modifier = Modifier.testTag(ScanScreenTags.SCAN_OVERLAY))
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.scan_top_hint),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ScanScreenTags.TOP_HINT),
            )
            Text(
                text = stringResource(R.string.scan_bottom_hint),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ScanScreenTags.BOTTOM_HINT),
            )
        }
    }
}

@Composable
private fun PermissionDeniedBody(
    state: ScanUiState.PermissionDenied,
    onIntent: (ScanIntent) -> Unit,
) {
    val titleRes =
        if (state.reason == ScanViewModel.REASON_CAMERA_BIND_FAILED) {
            R.string.scan_camera_bind_failed
        } else {
            R.string.scan_permission_denied_title
        }
    val primaryRes =
        if (state.reason == ScanViewModel.REASON_CAMERA_BIND_FAILED) {
            R.string.scan_camera_bind_failed_retry
        } else {
            R.string.scan_permission_denied_retry
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(ScanScreenTags.PERMISSION_DENIED_BODY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onIntent(ScanIntent.OnRetryPermission) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(ScanScreenTags.RETRY_PERMISSION_BUTTON),
        ) {
            Text(stringResource(primaryRes))
        }
        if (state.reason != ScanViewModel.REASON_CAMERA_BIND_FAILED) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onIntent(ScanIntent.OnOpenSettings) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(ScanScreenTags.OPEN_SETTINGS_BUTTON),
            ) {
                Text(stringResource(R.string.scan_permission_denied_open_settings))
            }
        }
    }
}

@Composable
private fun NoCameraBody(onIntent: (ScanIntent) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(ScanScreenTags.NO_CAMERA_BODY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.scan_no_camera_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onIntent(ScanIntent.OnNavigateBack) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(ScanScreenTags.NO_CAMERA_BACK_BUTTON),
        ) {
            Text(stringResource(R.string.scan_no_camera_back))
        }
    }
}

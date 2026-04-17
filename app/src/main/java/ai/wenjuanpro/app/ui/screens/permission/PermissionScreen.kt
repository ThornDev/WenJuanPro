package ai.wenjuanpro.app.ui.screens.permission

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.permission.PermissionEffect
import ai.wenjuanpro.app.feature.permission.PermissionIntent
import ai.wenjuanpro.app.feature.permission.PermissionUiState
import ai.wenjuanpro.app.feature.permission.PermissionViewModel
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

object PermissionScreenTags {
    const val PRIMARY_BUTTON = "permission_primary_button"
    const val RECHECK_BUTTON = "permission_recheck_button"
    const val FALLBACK_COPY_BUTTON = "permission_fallback_copy_button"
    const val FALLBACK_TEXT = "permission_fallback_text"
    const val ROOT = "permission_screen_root"
}

@Composable
fun PermissionScreen(
    onNavigateToConfigList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onIntent(PermissionIntent.CheckPermission)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                PermissionEffect.NavigateToConfigList -> onNavigateToConfigList()
                is PermissionEffect.LaunchSettings ->
                    try {
                        context.startActivity(effect.intent)
                    } catch (e: ActivityNotFoundException) {
                        showIntentUnavailableToast(context)
                    }
                PermissionEffect.ShowIntentUnavailable -> showIntentUnavailableToast(context)
            }
        }
    }

    PermissionContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionContent(
    state: PermissionUiState,
    onIntent: (PermissionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard =
        remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    val copyLabel = stringResource(R.string.permission_fallback_copy_label)
    val fallbackPath = stringResource(R.string.permission_fallback_path)
    val copiedToast = stringResource(R.string.permission_fallback_copied)

    Scaffold(
        modifier = modifier.testTag(PermissionScreenTags.ROOT),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = stringResource(R.string.permission_icon_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permission_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))

            when (state) {
                PermissionUiState.Checking -> Unit

                is PermissionUiState.NotGranted -> {
                    if (state.intentAvailable) {
                        val labelRes =
                            if (state.hasAttempted) {
                                R.string.permission_button_open_settings_retry
                            } else {
                                R.string.permission_button_open_settings
                            }
                        Button(
                            onClick = { onIntent(PermissionIntent.OpenSettings) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(PermissionScreenTags.PRIMARY_BUTTON),
                        ) {
                            Text(stringResource(labelRes))
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { onIntent(PermissionIntent.Recheck) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(PermissionScreenTags.RECHECK_BUTTON),
                        ) {
                            Text(stringResource(R.string.permission_button_recheck))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.permission_fallback_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = fallbackPath,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag(PermissionScreenTags.FALLBACK_TEXT),
                        )
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                clipboard?.setPrimaryClip(ClipData.newPlainText(copyLabel, fallbackPath))
                                Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(PermissionScreenTags.FALLBACK_COPY_BUTTON),
                        ) {
                            Text(stringResource(R.string.permission_fallback_copy_button))
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { onIntent(PermissionIntent.Recheck) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(PermissionScreenTags.RECHECK_BUTTON),
                        ) {
                            Text(stringResource(R.string.permission_button_recheck))
                        }
                    }
                }

                PermissionUiState.Granted -> Unit
            }
        }
    }
}

private fun showIntentUnavailableToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.permission_intent_unavailable_toast),
        Toast.LENGTH_LONG,
    ).show()
}

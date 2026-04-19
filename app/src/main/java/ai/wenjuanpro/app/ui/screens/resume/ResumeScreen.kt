package ai.wenjuanpro.app.ui.screens.resume

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.resume.ResumeEffect
import ai.wenjuanpro.app.feature.resume.ResumeIntent
import ai.wenjuanpro.app.feature.resume.ResumeUiState
import ai.wenjuanpro.app.feature.resume.ResumeViewModel
import ai.wenjuanpro.app.ui.components.ContentConstraint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

object ResumeScreenTags {
    const val ROOT = "resume_screen"
    const val RESUME_BUTTON = "resume_button"
    const val ABANDON_BUTTON = "abandon_button"
    const val CONFIRM_DIALOG = "abandon_confirm_dialog"
}

@Composable
fun ResumeScreen(
    onNavigateToQuestion: (studentId: String, configId: String) -> Unit,
    onNavigateToWelcome: (studentId: String, configId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResumeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ResumeEffect.NavigateToQuestion ->
                    onNavigateToQuestion(effect.studentId, effect.configId)
                is ResumeEffect.NavigateToWelcome ->
                    onNavigateToWelcome(effect.studentId, effect.configId)
            }
        }
    }

    ContentConstraint(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(ResumeScreenTags.ROOT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val s = state) {
            ResumeUiState.Loading -> {
                CircularProgressIndicator()
            }

            is ResumeUiState.Ready -> {
                Text(
                    text = stringResource(R.string.resume_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.resume_student, s.studentId),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.resume_config, s.title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.resume_progress, s.completedCount, s.totalCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.resume_last_time, s.lastSessionTime),
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.onIntent(ResumeIntent.OnResume) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ResumeScreenTags.RESUME_BUTTON),
                ) {
                    Text(stringResource(R.string.resume_continue_button))
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.onIntent(ResumeIntent.OnAbandonClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ResumeScreenTags.ABANDON_BUTTON),
                    enabled = !s.abandoning,
                ) {
                    Text(stringResource(R.string.resume_abandon_button))
                }

                if (s.showAbandonConfirm) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onIntent(ResumeIntent.OnAbandonCancelled) },
                        title = { Text(stringResource(R.string.resume_abandon_confirm_title)) },
                        text = { Text(stringResource(R.string.resume_abandon_confirm_body)) },
                        dismissButton = {
                            TextButton(
                                onClick = { viewModel.onIntent(ResumeIntent.OnAbandonCancelled) },
                            ) {
                                Text(stringResource(R.string.resume_abandon_confirm_cancel))
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.onIntent(ResumeIntent.OnAbandonConfirmed) },
                            ) {
                                Text(stringResource(R.string.resume_abandon_confirm_ok))
                            }
                        },
                        modifier = Modifier.testTag(ResumeScreenTags.CONFIRM_DIALOG),
                    )
                }
            }

            is ResumeUiState.Error -> {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    }
}

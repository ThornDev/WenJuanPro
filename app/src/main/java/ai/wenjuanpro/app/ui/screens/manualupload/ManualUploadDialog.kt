package ai.wenjuanpro.app.ui.screens.manualupload

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.manualupload.FileRow
import ai.wenjuanpro.app.feature.manualupload.FileStatus
import ai.wenjuanpro.app.feature.manualupload.ManualUploadViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object ManualUploadDialogTags {
    const val ROOT = "manual_upload_dialog_root"
    const val REFRESH = "manual_upload_refresh"
    const val UPLOAD_PREFIX = "manual_upload_button_"
}

@Composable
fun ManualUploadDialog(
    onDismiss: () -> Unit,
    viewModel: ManualUploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
                Modifier
                    .widthIn(max = 560.dp)
                    .testTag(ManualUploadDialogTags.ROOT),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.manual_upload_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag(ManualUploadDialogTags.REFRESH),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.manual_upload_refresh_cd),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    when {
                        state.loading ->
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            }
                        state.rows.isEmpty() ->
                            Text(
                                text = stringResource(R.string.manual_upload_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        else ->
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(items = state.rows, key = { it.fileName }) { row ->
                                    UploadRow(
                                        row = row,
                                        onUpload = { viewModel.upload(row.fileName) },
                                    )
                                }
                            }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.manual_upload_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadRow(
    row: FileRow,
    onUpload: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                StatusLine(status = row.status)
            }
            Spacer(Modifier.width(12.dp))
            UploadButton(status = row.status, fileName = row.fileName, onClick = onUpload)
        }
    }
}

@Composable
private fun StatusLine(status: FileStatus) {
    val (text, color) =
        when (status) {
            FileStatus.Idle -> Pair(null, MaterialTheme.colorScheme.onSurfaceVariant)
            is FileStatus.Uploading ->
                Pair(
                    stringResource(R.string.manual_upload_status_uploading, status.attempt, status.max),
                    MaterialTheme.colorScheme.primary,
                )
            FileStatus.Success ->
                Pair(stringResource(R.string.manual_upload_status_success), MaterialTheme.colorScheme.primary)
            is FileStatus.Failed ->
                Pair(
                    if (status.reason.isNullOrBlank()) {
                        stringResource(R.string.manual_upload_status_failed)
                    } else {
                        stringResource(R.string.manual_upload_status_failed_with_reason, status.reason)
                    },
                    MaterialTheme.colorScheme.error,
                )
        }
    if (text != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status is FileStatus.Uploading) {
                CircularProgressIndicator(
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
private fun UploadButton(
    status: FileStatus,
    fileName: String,
    onClick: () -> Unit,
) {
    val labelRes =
        when (status) {
            FileStatus.Success -> R.string.manual_upload_btn_again
            is FileStatus.Failed -> R.string.manual_upload_btn_retry
            is FileStatus.Uploading -> R.string.manual_upload_btn_uploading
            FileStatus.Idle -> R.string.manual_upload_btn_upload
        }
    val enabled = status !is FileStatus.Uploading
    if (status is FileStatus.Success) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(ManualUploadDialogTags.UPLOAD_PREFIX + fileName),
        ) {
            Text(stringResource(labelRes))
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(ManualUploadDialogTags.UPLOAD_PREFIX + fileName),
        ) {
            Text(stringResource(labelRes))
        }
    }
}

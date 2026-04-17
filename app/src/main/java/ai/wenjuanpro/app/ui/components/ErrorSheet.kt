package ai.wenjuanpro.app.ui.components

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.data.parser.ParseError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

object ErrorSheetTags {
    const val ROOT = "error_sheet_root"
    const val CLOSE_BUTTON = "error_sheet_close_button"
    const val ROW_PREFIX = "error_sheet_row_"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorSheet(
    errors: List<ParseError>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldPlaceholder = stringResource(R.string.config_list_error_field_placeholder)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(ErrorSheetTags.ROOT),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.config_list_error_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(ErrorSheetTags.CLOSE_BUTTON),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.config_list_error_sheet_close_cd),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(
                    items = errors,
                    key = { err -> "${err.line}-${err.field ?: "-"}-${err.code.name}-${err.message.hashCode()}" },
                ) { error ->
                    val rowText =
                        stringResource(
                            R.string.config_list_error_row_format,
                            error.line,
                            error.field ?: fieldPlaceholder,
                            error.message,
                        )
                    Text(
                        text = rowText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag(ErrorSheetTags.ROW_PREFIX + error.line),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

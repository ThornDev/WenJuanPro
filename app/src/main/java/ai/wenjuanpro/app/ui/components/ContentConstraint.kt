package ai.wenjuanpro.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Constrains content to a maximum width (default 600dp) and centers it
 * horizontally. On phones this has no visual effect; on tablets it prevents
 * content from stretching across the full 10"+ screen.
 */
@Composable
fun ContentConstraint(
    modifier: Modifier = Modifier,
    maxWidth: Int = 600,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth.dp)) {
            content()
        }
    }
}

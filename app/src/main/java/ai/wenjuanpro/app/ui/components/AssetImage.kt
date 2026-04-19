package ai.wenjuanpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.io.File

object AssetImageDefaults {
    const val ASSET_DIR = "/sdcard/WenJuanPro/assets/"
}

private val PlaceholderGray = Color(0xFFE0E0E0)
private val ErrorPink = Color(0xFFFCE4EC)

@Composable
fun AssetImage(
    fileName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val file = File(AssetImageDefaults.ASSET_DIR + fileName)
    SubcomposeAsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(file)
                .crossfade(true)
                .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize().background(PlaceholderGray),
            )
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize().background(ErrorPink),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "图片缺失：$fileName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }
        },
    )
}

package ai.wenjuanpro.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.File

object StemAudioPlayerTags {
    const val ROOT = "stem_audio_player_root"
    const val PLAY_BUTTON = "stem_audio_player_play_button"
    const val ERROR_TEXT = "stem_audio_player_error"
}

private const val ASSET_DIR = "/sdcard/WenJuanPro/assets/"

@Composable
fun StemAudioPlayer(
    fileName: String,
    autoPlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val file = File(ASSET_DIR + fileName)
    var error by remember(fileName) { mutableStateOf<String?>(null) }
    var prepared by remember(fileName) { mutableStateOf(false) }
    var isPlaying by remember(fileName) { mutableStateOf(false) }
    var ended by remember(fileName) { mutableStateOf(false) }
    var durationMs by remember(fileName) { mutableIntStateOf(0) }
    var positionMs by remember(fileName) { mutableIntStateOf(0) }

    val player =
        remember(fileName) {
            if (!file.exists()) {
                error = "音频文件缺失：$fileName"
                null
            } else {
                runCatching {
                    MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        setOnPreparedListener { mp ->
                            durationMs = mp.duration
                            prepared = true
                            if (autoPlay) {
                                runCatching { mp.start() }
                                    .onSuccess { isPlaying = true }
                                    .onFailure { Timber.w(it, "audio start failed") }
                            }
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            ended = true
                            positionMs = durationMs
                        }
                        setOnErrorListener { _, what, extra ->
                            Timber.w("audio error what=$what extra=$extra file=$fileName")
                            error = "音频播放出错"
                            isPlaying = false
                            true
                        }
                        prepareAsync()
                    }
                }.onFailure { e ->
                    Timber.w(e, "audio init failed file=$fileName")
                    error = "音频加载失败"
                }.getOrNull()
            }
        }

    DisposableEffect(player) {
        onDispose {
            runCatching { player?.stop() }
            runCatching { player?.release() }
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (player == null || !isPlaying) return@LaunchedEffect
        while (isPlaying) {
            runCatching { positionMs = player.currentPosition }
            delay(200L)
        }
    }

    val progress =
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(StemAudioPlayerTags.ROOT),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onClick@{
                    val mp = player ?: return@onClick
                    if (!prepared) return@onClick
                    runCatching {
                        when {
                            ended -> {
                                mp.seekTo(0)
                                mp.start()
                                positionMs = 0
                                ended = false
                                isPlaying = true
                            }
                            isPlaying -> {
                                mp.pause()
                                isPlaying = false
                            }
                            else -> {
                                mp.start()
                                isPlaying = true
                            }
                        }
                    }.onFailure { Timber.w(it, "audio toggle failed") }
                },
                enabled = error == null && prepared,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = Modifier.size(40.dp).testTag(StemAudioPlayerTags.PLAY_BUTTON),
            ) {
                val icon =
                    when {
                        ended -> Icons.Filled.Replay
                        isPlaying -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    }
                Icon(imageVector = icon, contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(StemAudioPlayerTags.ERROR_TEXT),
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = progress)
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatTime(positionMs) + " / " + formatTime(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Unspecified,
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

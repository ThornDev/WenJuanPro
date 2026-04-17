package ai.wenjuanpro.app.ui.components

import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag

object HiddenLongPressAreaTags {
    const val ROOT = "hidden_long_press_area"
}

@Composable
fun HiddenLongPressArea(
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    thresholdMs: Long = 5_000L,
) {
    val currentOnTriggered = rememberUpdatedState(onTriggered)
    Box(
        modifier =
            modifier
                .testTag(HiddenLongPressAreaTags.ROOT)
                .pointerInput(thresholdMs) {
                    detectTapGestures(
                        onPress = {
                            val down = SystemClock.elapsedRealtime()
                            val released = tryAwaitRelease()
                            val held = SystemClock.elapsedRealtime() - down
                            if (released && held >= thresholdMs) {
                                currentOnTriggered.value.invoke()
                            }
                        },
                    )
                },
    )
}

package ai.wenjuanpro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp

object OptionCardTags {
    const val PREFIX = "option_card_"
    const val BADGE_PREFIX = "option_card_badge_"
    const val LABEL_PREFIX = "option_card_label_"
    const val IMAGE_PLACEHOLDER_PREFIX = "option_card_image_placeholder_"
    const val CHECK_CIRCLE_PREFIX = "option_card_check_circle_"
}

private val PrimaryBlue = Color(0xFF1976D2)
private val UnselectedGray = Color(0xFFBDBDBD)

private fun optionLetter(index: Int): String =
    if (index in 1..26) ('A' + (index - 1)).toString() else index.toString()

@Composable
fun OptionCard(
    index: Int,
    text: String?,
    imageAssetName: String?,
    imageWidthDp: Int? = null,
    imageHeightDp: Int? = null,
    isSelected: Boolean,
    isMulti: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else Color.Transparent,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "option_border",
    )
    val containerColor by animateColorAsState(
        targetValue =
            if (isSelected) PrimaryBlue.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "option_bg",
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else UnselectedGray,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "option_badge",
    )

    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 2.dp, color = borderColor),
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = containerColor,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .testTag(OptionCardTags.PREFIX + index)
                .clickable(onClick = onClick),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(badgeColor, shape = CircleShape)
                            .testTag(OptionCardTags.BADGE_PREFIX + index),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = optionLetter(index),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(OptionCardTags.LABEL_PREFIX + index),
                    )
                }
                if (imageAssetName != null) {
                    Spacer(Modifier.width(8.dp))
                    val imgW = (imageWidthDp ?: 48).dp
                    val imgH = (imageHeightDp ?: 48).dp
                    Box(
                        modifier =
                            Modifier
                                .width(imgW)
                                .height(imgH)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag(OptionCardTags.IMAGE_PLACEHOLDER_PREFIX + index),
                    ) {
                        AssetImage(
                            fileName = imageAssetName,
                            modifier = Modifier.width(imgW).height(imgH),
                        )
                    }
                }
            }
            if (isMulti && isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .testTag(OptionCardTags.CHECK_CIRCLE_PREFIX + index),
                )
            }
        }
    }
}

package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.ui.components.OptionCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object QuestionContentTags {
    const val STEM_TEXT = "question_stem_text"
    const val STEM_IMAGE_PLACEHOLDER = "question_stem_image_placeholder"
    const val OPTIONS_GRID = "question_options_grid"
}

private val PlaceholderGray = Color(0xFFE0E0E0)

internal fun optionColumnsFor(count: Int): Int = if (count <= 4) 2 else 3

@Composable
fun StemBlock(
    stem: StemContent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        renderStem(stem)
    }
}

@Composable
private fun renderStem(stem: StemContent) {
    when (stem) {
        is StemContent.Text ->
            Text(
                text = stem.text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag(QuestionContentTags.STEM_TEXT),
            )
        is StemContent.Image ->
            ImagePlaceholder(
                label = stem.fileName,
                widthDp = stem.widthDp,
                heightDp = stem.heightDp,
            )
        is StemContent.Mixed -> {
            stem.parts.forEachIndexed { idx, part ->
                if (idx > 0) Spacer(Modifier.height(8.dp))
                renderStem(part)
            }
        }
    }
}

@Composable
private fun ImagePlaceholder(
    label: String,
    widthDp: Int? = null,
    heightDp: Int? = null,
) {
    val widthMod = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier.fillMaxWidth()
    val heightMod = Modifier.height((heightDp ?: 160).dp)
    Box(
        modifier =
            widthMod
                .then(heightMod)
                .background(PlaceholderGray, shape = RoundedCornerShape(8.dp))
                .testTag(QuestionContentTags.STEM_IMAGE_PLACEHOLDER),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "图片占位：$label",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
        )
    }
}

@Composable
fun OptionsGrid(
    options: List<OptionContent>,
    selectedIndex: Int?,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = optionColumnsFor(options.size)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxSize()
                .testTag(QuestionContentTags.OPTIONS_GRID)
                .padding(horizontal = 4.dp),
    ) {
        itemsIndexed(options) { index, option ->
            val display = displayOption(option)
            OptionCard(
                index = index + 1,
                text = display.text,
                imageAssetName = display.imageAssetName,
                imageWidthDp = display.imageWidthDp,
                imageHeightDp = display.imageHeightDp,
                isSelected = selectedIndex == index + 1,
                isMulti = false,
                onClick = { onOptionClick(index + 1) },
            )
        }
    }
}

@Composable
fun MultiOptionsGrid(
    options: List<OptionContent>,
    selectedIndices: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = optionColumnsFor(options.size)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxSize()
                .testTag(QuestionContentTags.OPTIONS_GRID)
                .padding(horizontal = 4.dp),
    ) {
        itemsIndexed(options) { index, option ->
            val display = displayOption(option)
            OptionCard(
                index = index + 1,
                text = display.text,
                imageAssetName = display.imageAssetName,
                imageWidthDp = display.imageWidthDp,
                imageHeightDp = display.imageHeightDp,
                isSelected = (index + 1) in selectedIndices,
                isMulti = true,
                onClick = { onToggle(index + 1) },
            )
        }
    }
}

private data class OptionDisplay(
    val text: String?,
    val imageAssetName: String?,
    val imageWidthDp: Int? = null,
    val imageHeightDp: Int? = null,
)

private fun displayOption(content: OptionContent): OptionDisplay =
    when (content) {
        is OptionContent.Text -> OptionDisplay(text = content.text, imageAssetName = null)
        is OptionContent.Image -> OptionDisplay(
            text = null,
            imageAssetName = content.fileName,
            imageWidthDp = content.widthDp,
            imageHeightDp = content.heightDp,
        )
        is OptionContent.Mixed -> {
            val text =
                content.parts.firstNotNullOfOrNull { (it as? OptionContent.Text)?.text }
            val image =
                content.parts.firstNotNullOfOrNull { it as? OptionContent.Image }
            OptionDisplay(
                text = text,
                imageAssetName = image?.fileName,
                imageWidthDp = image?.widthDp,
                imageHeightDp = image?.heightDp,
            )
        }
    }

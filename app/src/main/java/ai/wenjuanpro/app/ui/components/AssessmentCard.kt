package ai.wenjuanpro.app.ui.components

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.configlist.ConfigCardUiModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object AssessmentCardTags {
    const val VALID_CARD_PREFIX = "assessment_card_valid_"
    const val INVALID_CARD_PREFIX = "assessment_card_invalid_"
    const val VIEW_ERRORS_BUTTON_PREFIX = "assessment_card_view_errors_"
}

@Composable
fun AssessmentCard(
    model: ConfigCardUiModel,
    onClick: () -> Unit,
    onViewErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusValid = stringResource(R.string.config_list_status_valid)
    val statusInvalid = stringResource(R.string.config_list_status_invalid)
    val cardTestTag =
        if (model.isValid) {
            AssessmentCardTags.VALID_CARD_PREFIX + model.sourceFileName
        } else {
            AssessmentCardTags.INVALID_CARD_PREFIX + model.sourceFileName
        }

    val baseModifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .testTag(cardTestTag)
            .semantics {
                contentDescription = if (model.isValid) statusValid else statusInvalid
            }

    if (model.isValid) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = baseModifier,
        ) {
            CardBody(model = model, onViewErrors = onViewErrors)
        }
    } else {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = baseModifier.alpha(0.5f),
        ) {
            CardBody(model = model, onViewErrors = onViewErrors)
        }
    }
}

@Composable
private fun CardBody(
    model: ConfigCardUiModel,
    onViewErrors: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = model.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuestionCountBadge(count = model.questionCount)
            if (!model.isValid) {
                TextButton(
                    onClick = onViewErrors,
                    modifier =
                        Modifier.testTag(
                            AssessmentCardTags.VIEW_ERRORS_BUTTON_PREFIX + model.sourceFileName,
                        ),
                ) {
                    Text(stringResource(R.string.config_list_view_errors))
                }
            }
        }
    }
}

@Composable
private fun QuestionCountBadge(count: Int?) {
    val text =
        if (count == null) {
            stringResource(R.string.config_list_question_count_unknown)
        } else {
            stringResource(R.string.config_list_question_count, count)
        }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

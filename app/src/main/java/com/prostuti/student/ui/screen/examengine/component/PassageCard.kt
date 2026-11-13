package com.prostuti.student.ui.screen.examengine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.ui.unit.sp
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * Wraps one or more MCQs that share a common passage: renders the passage
 * text/image once at the top, then [content] (the grouped MCQs) below it,
 * all inside a single card.
 */
@Composable
fun PassageCard(
    passage: FetchPassageData?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (passage != null && (!passage.passageText.isNullOrBlank() || !passage.passageImage.isNullOrBlank())) {
            PassageBlock(passage = passage)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        content()
    }
}

/** A passage's [label], then its image and/or text — whichever it has. Shows nothing for an empty passage. */
@Composable
fun PassageBlock(passage: FetchPassageData, modifier: Modifier = Modifier, label: String = "অনুচ্ছেদ") {
    if (passage.passageText.isNullOrBlank() && passage.passageImage.isNullOrBlank()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = ProstutiTypography.enLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!passage.passageImage.isNullOrBlank()) {
            PassageImage(imageUrl = passage.passageImage)
        }
        if (!passage.passageText.isNullOrBlank()) {
            MathText(
                text = passage.passageText,
                style = ProstutiTypography.bnBodyMedium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** The passage picture at full card width and its own aspect ratio; a status box while it loads or if it fails. */
@Composable
private fun PassageImage(imageUrl: String) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = "অনুচ্ছেদের ছবি",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .testTag(PASSAGE_IMAGE_TAG),
        loading = { PassageImageStatus(text = "ছবি লোড হচ্ছে…") },
        error = { PassageImageStatus(text = "ছবি লোড করা যায়নি") }
    )
}

@Composable
private fun PassageImageStatus(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

const val PASSAGE_IMAGE_TAG = "passage_image"

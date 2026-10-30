package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * The title + subtitle block repeated at the top of every auth screen. [compact] matches the
 * smaller 23/30px title used on the OTP/reset screens (vs. 25/32px on Start/Login/Signup).
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val titleStyle = if (compact) {
        ProstutiTypography.bnHeadlineMedium.copy(fontSize = 23.sp, lineHeight = 30.sp)
    } else {
        ProstutiTypography.bnHeadlineMedium.copy(fontSize = 25.sp, lineHeight = 32.sp)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = titleStyle, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = subtitle,
            style = ProstutiTypography.bnBodyMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

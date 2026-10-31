package com.prostuti.student.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * Round profile picture. Loads [imageUrl] with Coil; while it loads, when it fails or when
 * there is no URL, shows the first letter of [name] (or a person icon when there's no name).
 */
@Composable
fun UserAvatar(
    imageUrl: String?,
    name: String?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    initialStyle: TextStyle = ProstutiTypography.bnLabelLarge
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val fallback: @Composable () -> Unit = {
            AvatarFallback(name = name, contentDescription = contentDescription, color = contentColor, style = initialStyle)
        }
        if (imageUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { fallback() },
                error = { fallback() }
            )
        }
    }
}

@Composable
private fun AvatarFallback(name: String?, contentDescription: String?, color: Color, style: TextStyle) {
    // First code point, so a Bangla initial isn't cut in half.
    val initial = name?.trim()?.takeIf { it.isNotEmpty() }?.let { String(Character.toChars(it.codePointAt(0))) }
    if (initial != null) {
        Text(text = initial.uppercase(), style = style, color = color)
    } else {
        Icon(Icons.Filled.Person, contentDescription = contentDescription, tint = color)
    }
}

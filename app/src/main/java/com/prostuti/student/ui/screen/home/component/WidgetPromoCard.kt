package com.prostuti.student.ui.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

const val WIDGET_PROMO_TAG = "home_widget_promo"
const val WIDGET_PROMO_ADD_TAG = "home_widget_promo_add"
const val WIDGET_PROMO_CLOSE_TAG = "home_widget_promo_close"

/** A small card on Home asking to put the Prostuti widget on the home screen. */
@Composable
fun WidgetPromoCard(onAdd: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .testTag(WIDGET_PROMO_TAG)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .clickable(onClick = onAdd)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.widget_promo_title),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.widget_promo_body),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Button(
            onClick = onAdd,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.testTag(WIDGET_PROMO_ADD_TAG)
        ) {
            Text(text = stringResource(R.string.widget_promo_add), style = ProstutiTypography.bnLabelMedium)
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag(WIDGET_PROMO_CLOSE_TAG)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.widget_promo_close),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

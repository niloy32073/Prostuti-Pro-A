package com.prostuti.student.ui.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.UserAvatar
import com.prostuti.student.ui.screen.home.HomeUiState
import com.prostuti.student.ui.screen.home.LeaderboardDemoEntry
import com.prostuti.student.ui.theme.ProstutiTypography

@Composable
fun LeaderboardSection(state: HomeUiState, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_leaderboard_title),
                style = ProstutiTypography.bnTitleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_see_all),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 16.dp, end = 16.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            state.leaderboardTop3.forEach { entry ->
                PodiumColumn(entry, modifier = Modifier.padding(horizontal = 5.dp))
            }
        }

        if (state.showMyRankCard) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(
                    imageUrl = state.profile?.profileImage,
                    name = state.profile?.firstName,
                    contentDescription = null,
                    size = 36.dp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.profile?.firstName?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.home_my_rank_label, it) }
                            ?: stringResource(R.string.home_my_rank_label_no_name),
                        style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.home_my_rating_demo),
                        style = ProstutiTypography.enLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.home_my_rank_number_demo),
                    style = ProstutiTypography.enTitleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(entry: LeaderboardDemoEntry, modifier: Modifier = Modifier) {
    val isFirst = entry.rank == 1
    val avatarSize = if (isFirst) 52.dp else 44.dp
    val barHeight = when (entry.rank) { 1 -> 68.dp; 2 -> 44.dp; else -> 32.dp }
    val avatarBg = if (isFirst) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val avatarOn = if (isFirst) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val barBg = if (isFirst) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val barOn = if (isFirst) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isFirst) {
            Text(text = "🏆", style = ProstutiTypography.bnLabelLarge)
        }
        Box(
            modifier = Modifier.size(avatarSize).background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(entry.initialRes), style = ProstutiTypography.bnTitleMedium, color = avatarOn)
        }
        Text(
            text = stringResource(entry.nameRes),
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(68.dp)
                .heightIn(min = barHeight)
                .background(barBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = entry.rank.toString(), style = ProstutiTypography.enTitleMedium, color = barOn)
        }
    }
}

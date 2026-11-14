package com.prostuti.student.ui.screen.group.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography

/** API limits (GroupCreateRequest / JoinByCodeRequest). */
private val NAME_LENGTH = 2..255
private const val DESCRIPTION_MAX = 1024
private val CODE_LENGTH = 4..20

/**
 * GroupList's + button: create a group (you become its admin) or join one by its code. The content of
 * the GroupCreateJoin bottom-sheet destination (the scene makes the sheet).
 */
@Composable
fun GroupCreateJoinSheet(
    isSubmitting: Boolean,
    error: String?,
    onCreate: (name: String, description: String, isPublic: Boolean) -> Unit,
    onJoin: (code: String) -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        PrimaryTabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text(stringResource(R.string.group_create_tab), style = ProstutiTypography.bnLabelLarge) })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text(stringResource(R.string.group_join_tab), style = ProstutiTypography.bnLabelLarge) })
        }

        Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tabIndex == 0) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= NAME_LENGTH.last) name = it },
                    label = { Text(stringResource(R.string.group_name_label), style = ProstutiTypography.bnBodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag(TAG_NAME)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= DESCRIPTION_MAX) description = it },
                    label = { Text(stringResource(R.string.group_description_label), style = ProstutiTypography.bnBodyMedium) },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.group_public_label), style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.group_public_hint), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it }, modifier = Modifier.testTag(TAG_PUBLIC))
                }
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { !it.isWhitespace() }.uppercase().take(CODE_LENGTH.last) },
                    label = { Text(stringResource(R.string.group_code_label), style = ProstutiTypography.bnBodyMedium) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag(TAG_CODE)
                )
            }

            if (error != null) {
                Text(text = error, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (tabIndex == 0) {
                PrimaryButton(
                    text = stringResource(R.string.group_create_button),
                    enabled = name.trim().length in NAME_LENGTH && !isSubmitting,
                    loading = isSubmitting,
                    onClick = { onCreate(name, description, isPublic) }
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.group_join_button),
                    enabled = code.length in CODE_LENGTH && !isSubmitting,
                    loading = isSubmitting,
                    onClick = { onJoin(code) }
                )
            }
        }
    }
}

const val TAG_NAME = "group_create_name"
const val TAG_PUBLIC = "group_create_public"
const val TAG_CODE = "group_join_code"

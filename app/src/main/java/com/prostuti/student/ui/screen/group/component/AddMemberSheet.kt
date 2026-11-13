package com.prostuti.student.ui.screen.group.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography

/** API limits of DirectAddRequest.identifier. */
private val IDENTIFIER_LENGTH = 5..255

/** Admin's "add a member" sheet: a registered user's email or phone (01XXXXXXXXX is fine). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberSheet(
    isSubmitting: Boolean,
    error: String?,
    onAdd: (identifier: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.group_add_member_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.group_add_member_hint), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = identifier,
                onValueChange = { if (it.length <= IDENTIFIER_LENGTH.last) identifier = it },
                label = { Text(stringResource(R.string.group_add_member_field), style = ProstutiTypography.bnBodyMedium) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag(TAG_ADD_MEMBER_FIELD)
            )
            if (error != null) {
                Text(text = error, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.error)
            }
            PrimaryButton(
                text = stringResource(R.string.group_add_member_button),
                enabled = identifier.trim().length in IDENTIFIER_LENGTH && !isSubmitting,
                loading = isSubmitting,
                onClick = { onAdd(identifier) }
            )
        }
    }
}

const val TAG_ADD_MEMBER_FIELD = "group_add_member_field"

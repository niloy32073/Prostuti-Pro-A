package com.prostuti.student.ui.screen.group.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography

/** The + button's bottom sheet on GroupList — create a new group (you become its admin) or join
 * an existing one by its 6-character code. Not in the original design; built for this round per
 * the plan's "ডিজাইন-ছাড়া অংশ" section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateJoinSheet(
    sheetState: SheetState,
    isSubmitting: Boolean,
    errorBn: String?,
    onCreate: (nameBn: String) -> Unit,
    onJoin: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("নতুন গ্রুপ তৈরি করো", style = ProstutiTypography.bnLabelLarge) })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("কোড দিয়ে জয়েন করো", style = ProstutiTypography.bnLabelLarge) })
            }

            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tabIndex == 0) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("গ্রুপের নাম", style = ProstutiTypography.bnBodyMedium) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("৬-অক্ষরের কোড", style = ProstutiTypography.bnBodyMedium) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorBn != null) {
                    Text(text = errorBn, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.error)
                }

                if (tabIndex == 0) {
                    PrimaryButton(
                        text = "গ্রুপ তৈরি করো",
                        enabled = name.trim().length in 3..40 && !isSubmitting,
                        loading = isSubmitting,
                        onClick = { onCreate(name.trim()) }
                    )
                } else {
                    PrimaryButton(
                        text = "জয়েন করো",
                        enabled = code.trim().length == 6 && !isSubmitting,
                        loading = isSubmitting,
                        onClick = { onJoin(code.trim()) }
                    )
                }
            }
        }
    }
}

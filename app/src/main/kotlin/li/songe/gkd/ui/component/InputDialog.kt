package li.songe.gkd.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun InputDialog(rowValue: String, dismiss: (() -> Unit)? = null, confirm: ((newValue: String) -> Unit)? = null) {
    var newInput by remember {
        mutableStateOf(rowValue)
    }
    val maxCharLen = 32
    AlertDialog(title = { Text(text = "请输入提示文字") }, text = {
        OutlinedTextField(
            value = newInput,
            onValueChange = {
                newInput = it.take(maxCharLen)
            },
            singleLine = true,
            supportingText = {
                Text(
                    text = "${newInput.length} / $maxCharLen",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
        )
    }, onDismissRequest = { dismiss?.invoke() }, confirmButton = {
        TextButton(
            enabled = newInput.isNotEmpty(),
            onClick = { confirm?.invoke(newInput) }
        ) {
            Text(
                text = "确认",
            )
        }
    }, dismissButton = {
        TextButton(onClick = { dismiss?.invoke() }) {
            Text(
                text = "取消",
            )
        }
    })
}
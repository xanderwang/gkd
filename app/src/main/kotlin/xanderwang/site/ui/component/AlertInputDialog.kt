package xanderwang.site.ui.component

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
fun AlertInputDialog(
    title: String = "请输入内容：",
    content: String,
    maxCharLen: Int = 64,
    dismiss: (() -> Unit)? = null,
    confirm: ((newContent: String) -> Unit)? = null
) {
    var contentState by remember {
        mutableStateOf(content)
    }
    AlertDialog(
        title = {
            Text(text = title)
        },
        text = {
            OutlinedTextField(
                value = contentState,
                onValueChange = {
                    contentState = it.take(maxCharLen)
                },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${contentState.length} / $maxCharLen",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        },
        onDismissRequest = {
            dismiss?.invoke()
        },
        confirmButton = {
            TextButton(
                enabled = contentState.isNotEmpty(),
                onClick = {
                    confirm?.invoke(contentState)
                    dismiss?.invoke()
                }
            ) {
                Text(
                    text = "确认",
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { dismiss?.invoke() }) {
                Text(
                    text = "取消",
                )
            }
        })
}
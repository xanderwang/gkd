package xanderwang.site.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.gkTopAppBarScroll
import li.gkd.app.ui.component.rememberColumnScrollState
import li.gkd.app.ui.home.BottomNavItem
import li.gkd.app.ui.home.HomeViewModel
import li.gkd.app.ui.home.ResetPageScrollOnRequest
import li.gkd.app.ui.home.ScaffoldExt
import li.gkd.app.ui.platform.LocalUiHost
import li.gkd.app.ui.style.itemPadding
import li.gkd.app.ui.style.titleItemPadding
import li.gkd.app.util.ToastUtils
import org.jetbrains.compose.resources.stringResource
import xanderwang.site.service.overlayTimeAvailable
import xanderwang.site.service.setOverlayTimeEnabled
import xanderwang.site.service.setSmsWatchEnabled
import xanderwang.site.service.smsWatchSupported

private const val MAX_KEYWORD_LENGTH = 64

@Composable
fun xPage(vm: HomeViewModel): ScaffoldExt {
    val host = LocalUiHost.current
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val overlayAvailable = overlayTimeAvailable()
    val smsSupported = smsWatchSupported()
    var editingKeyword by remember { mutableStateOf<String?>(null) }
    val scroll = rememberColumnScrollState()
    ResetPageScrollOnRequest(vm.homeState, BottomNavItem.SmsWatch, scroll::resetScrollAndAwait)
    val page =
        ScaffoldExt(
            navItem = BottomNavItem.SmsWatch,
            modifier = Modifier.gkTopAppBarScroll(scroll.scrollBehavior),
            topBar = {
                GkTopAppBar(
                    scrollBehavior = scroll.scrollBehavior,
                    title = { Text(BottomNavItem.SmsWatch.label) },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(contentPadding).verticalScroll(scroll.scrollState),
            ) {
                Text(
                    text = stringResource(Res.string.sms_watch_section),
                    modifier = Modifier.titleItemPadding(showTop = false),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                GkTextSwitch(
                    title = stringResource(Res.string.sms_watch_enable),
                    subtitle =
                        if (smsSupported) store.smsWatchKey
                        else stringResource(Res.string.sms_watch_android_only),
                    checked = store.enableWatchSms,
                    enabled = smsSupported,
                    onClick = { editingKeyword = store.smsWatchKey },
                    onClickLabel = stringResource(Res.string.sms_watch_edit_keyword),
                    onCheckedChange = { enabled -> host.setSmsWatchEnabled(enabled) },
                )
                if (overlayAvailable) {
                    GkTextSwitch(
                        title = stringResource(Res.string.overlay_time_service),
                        subtitle = stringResource(Res.string.overlay_time_description),
                        checked = store.enableTimeOverlay,
                        onCheckedChange = { enabled -> setOverlayTimeEnabled(enabled) },
                    )
                    AnimatedVisibility(visible = store.enableTimeOverlay) {
                        Column {
                            var alphaDraft by
                                remember(store.overlayBgAlpha) {
                                    mutableFloatStateOf(store.overlayBgAlpha)
                                }
                            OverlaySliderRow(
                                title = stringResource(Res.string.overlay_bg_alpha),
                                value = "${(alphaDraft * 100).toInt()}%",
                            ) {
                                Slider(
                                    value = alphaDraft,
                                    onValueChange = { alphaDraft = it },
                                    onValueChangeFinished = {
                                        SettingsRepository.updateSettings {
                                            it.copy(overlayBgAlpha = alphaDraft)
                                        }
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            var fontSizeDraft by
                                remember(store.overlayFontSize) {
                                    mutableFloatStateOf(store.overlayFontSize)
                                }
                            OverlaySliderRow(
                                title = stringResource(Res.string.overlay_font_size),
                                value = "${fontSizeDraft.toInt()}sp",
                            ) {
                                Slider(
                                    value = fontSizeDraft,
                                    onValueChange = { fontSizeDraft = it },
                                    onValueChangeFinished = {
                                        SettingsRepository.updateSettings {
                                            it.copy(overlayFontSize = fontSizeDraft)
                                        }
                                    },
                                    valueRange = 10f..32f,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
                GkPageBottomSpace()
            }
        }
    editingKeyword?.let { keyword ->
        KeywordDialog(
            keyword = keyword,
            onDismiss = { editingKeyword = null },
            onConfirm = { newKeyword ->
                SettingsRepository.updateSettings { it.copy(smsWatchKey = newKeyword) }
                editingKeyword = null
                ToastUtils.show(Res.string.update_success.getSync())
            },
        )
    }
    return page
}

@Composable
private fun KeywordDialog(
    keyword: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(keyword) }
    AlertDialog(
        title = {
            Text(text = stringResource(Res.string.sms_watch_keyword_title))
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(MAX_KEYWORD_LENGTH) },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${value.length} / $MAX_KEYWORD_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = value.isNotEmpty(),
                onClick = { onConfirm(value) },
            ) {
                Text(text = stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun OverlaySliderRow(
    title: String,
    value: String,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().itemPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    slider()
}

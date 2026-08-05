package xanderwang.site.ui.page

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.store.xPageStoreFlow
import li.songe.gkd.ui.component.PerfCustomIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.home.BottomNavItem
import li.songe.gkd.ui.home.HomeVm
import li.songe.gkd.ui.home.ScaffoldExt
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.toast
import xanderwang.site.service.OverlayTimeService
import xanderwang.site.service.XManageService
import xanderwang.site.ui.component.AlertInputDialog

@Composable
fun useXPage(): ScaffoldExt {
    val mainVm = LocalMainViewModel.current
    val context = LocalActivity.current as MainActivity
    val store by xPageStoreFlow.collectAsState()
    val vm = viewModel<HomeVm>()

    var msgKeyDlgFlow by vm.showXMsgKeyDlgFlow.asMutableState()
    if (msgKeyDlgFlow) {
        AlertInputDialog(
            title = "需要监听的关键字",
            content = store.msgKey,
            dismiss = { msgKeyDlgFlow = false },
            confirm = { msgKey ->
                xPageStoreFlow.update { it.copy(msgKey = msgKey) }
                toast("更新成功")
            }
        )
    }

    var showToastSettingsDlg by vm.showToastSettingsDlgFlow.asMutableState()

    var showTimeOverlaySettings by vm.showTimeOverlaySettingsFlow.asMutableState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberScrollState()
    return ScaffoldExt(
        navItem = BottomNavItem.Xpages,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = BottomNavItem.Xpages.label,
                    )
                },
            )
        },
    ) { contentPadding ->


        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            Text(
                text = "常规",
                modifier = Modifier.titleItemPadding(showTop = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            TextSwitch(
                title = "短信内容监控",
                subtitle = store.msgKey,
                checked = store.enableWatchSMS,
                onClickLabel = "打开触发提示弹窗",
                onClick = {
                    msgKeyDlgFlow = true
                },
                // suffixIcon = {
                //     CustomIconButton(
                //         size = 32.dp,
                //         onClickLabel = "打开提示设置弹窗",
                //         onClick = throttle { showToastSettingsDlg = true },
                //     ) {
                //         PerfIcon(
                //             modifier = Modifier.size(20.dp),
                //             id = SafeR.ic_page_info,
                //             contentDescription = "提示设置",
                //         )
                //     }
                // },
                onCheckedChange = {
                    xPageStoreFlow.value = store.copy(
                        enableWatchSMS = it
                    )
                    if (it) {
                        XManageService.startWatch()
                        toast("已打开短信监听")
                    } else {
                        XManageService.stopWatch()
                        toast("已关闭短信监听")
                    }
                })

            TextSwitch(
                title = "时间悬浮窗",
                subtitle = "在所有界面显示当前时间",
                checked = store.enableTimeOverlay,
                suffixIcon = {
                    PerfCustomIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = "展开或收起时间悬浮窗设置",
                        onClick = { vm.showTimeOverlaySettingsFlow.update { !it } },
                        id = R.drawable.ic_page_info,
                        contentDescription = "时间悬浮窗设置",
                        tint = if (showTimeOverlaySettings) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                },
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        xPageStoreFlow.value = store.copy(enableTimeOverlay = true)
                        OverlayTimeService.start()
                        toast("已开启时间悬浮窗")
                    } else {
                        xPageStoreFlow.value = store.copy(enableTimeOverlay = false)
                        OverlayTimeService.stop()
                    }
                })

            AnimatedVisibility(visible = showTimeOverlaySettings) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .itemPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "背景透明度",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${(store.overlayBgAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = store.overlayBgAlpha,
                        onValueChange = {
                            xPageStoreFlow.value = store.copy(overlayBgAlpha = it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .itemPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "字体大小",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${store.overlayFontSize.toInt()}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = store.overlayFontSize,
                        onValueChange = {
                            xPageStoreFlow.value = store.copy(overlayFontSize = it)
                        },
                        valueRange = 10f..32f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
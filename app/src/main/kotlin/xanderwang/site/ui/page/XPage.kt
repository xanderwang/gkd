package xanderwang.site.ui.page

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.store.xPageStoreFlow
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.home.BottomNavItem
import li.songe.gkd.ui.home.HomeVm
import li.songe.gkd.ui.home.ScaffoldExt
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.toast
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
                    if (it) toast("已打开短信监听") else toast("已关闭短信监听")
                })
        }
    }
}
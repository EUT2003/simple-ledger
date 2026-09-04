package com.example.simpleledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.simpleledger.data.ThemePreference
import com.example.simpleledger.data.AccentPreference
import com.example.simpleledger.data.LanguagePreference
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.example.simpleledger.state.AppState
import com.example.simpleledger.BuildConfig

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    val dialog = remember { mutableStateOf("") }
    val clearConfirmation = remember { mutableStateOf(false) }
    val dayExpanded = remember { mutableStateOf(false) }
    val versionInfo = remember { mutableStateOf(false) }
    val english = state.settings.language == LanguagePreference.ENGLISH
    fun t(zh:String,en:String)=if(english) en else zh
    fun themeName()=when(state.settings.theme){ThemePreference.SYSTEM->t("跟随系统","System default");ThemePreference.LIGHT->t("亮色","Light");ThemePreference.DARK->t("暗色","Dark")}
    fun accentName()=when(state.settings.accent){AccentPreference.RED->t("红","Red");AccentPreference.ORANGE->t("橙","Orange");AccentPreference.YELLOW->t("黄","Yellow");AccentPreference.BLUE->t("蓝","Blue");AccentPreference.GREEN->t("绿","Green");AccentPreference.PINK->t("粉","Pink");AccentPreference.PURPLE->t("紫","Purple")}
    fun languageName()=if(english) "English" else "中文"
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick=onBack){Text(t("← 返回","← Back"))}
        Text(t("设置","Settings"), style = MaterialTheme.typography.headlineMedium)
        ListItem(modifier=Modifier.clickable { dialog.value="theme" }, headlineContent = { Text(t("主题","Theme")) }, supportingContent = { Text(themeName()) })
        ListItem(modifier=Modifier.clickable { dialog.value="accent" }, headlineContent = { Text(t("主题颜色","Theme color")) }, supportingContent = { Text(accentName()) })
        ListItem(modifier=Modifier.clickable { dialog.value="language" }, headlineContent = { Text(if(english) "Language" else "语言/Language") }, supportingContent = { Text(languageName()) })
        ListItem(modifier=Modifier.clickable { dialog.value="currency" }, headlineContent = { Text(t("默认币种","Default currency")) }, supportingContent = { Text(state.settings.defaultCurrency) })
        Box {
            ListItem(modifier=Modifier.clickable { dayExpanded.value=true }, headlineContent = { Text(t("每月起始日","Monthly start day")) }, supportingContent = { Text("${state.settings.monthStartDay} ${t("日","day")}") })
            DropdownMenu(expanded=dayExpanded.value, onDismissRequest={dayExpanded.value=false}, offset=DpOffset(120.dp, 0.dp)) {
                Column(Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                    (1..28).forEach { day ->
                        DropdownMenuItem(text={Text("$day ${t("日","day")}")}, onClick={state.updateSettings(state.settings.copy(monthStartDay=day));dayExpanded.value=false})
                    }
                }
            }
        }
        ListItem(modifier=Modifier.clickable { versionInfo.value=true }, headlineContent = { Text(t("版本信息", "Version information")) }, supportingContent = { Text("v${BuildConfig.VERSION_NAME}") })
        ListItem(modifier=Modifier.clickable { clearConfirmation.value=true }, headlineContent = { Text(t("清空所有数据","Clear all data"), color = MaterialTheme.colorScheme.error) }, supportingContent = { Text(t("恢复为默认状态","Restore default state")) })
    } }
    val options=when(dialog.value){"theme"->if(english) listOf("System default","Light","Dark") else listOf("跟随系统","亮色","暗色");"accent"->if(english) listOf("Red","Orange","Yellow","Blue","Green","Pink","Purple") else listOf("红","橙","黄","蓝","绿","粉","紫");"language"->if(english) listOf("Chinese","English") else listOf("中文","English");"currency"->listOf("CNY","USD","EUR","GBP","JPY","HKD","TWD");else->emptyList()}
    if(options.isNotEmpty()) AlertDialog(onDismissRequest={dialog.value=""},title={Text(t("选择","Select"))},text={LazyColumn(Modifier.heightIn(max=420.dp)){itemsIndexed(options){index,option->TextButton(onClick={when(dialog.value){"theme"->state.updateSettings(state.settings.copy(theme=listOf(ThemePreference.SYSTEM,ThemePreference.LIGHT,ThemePreference.DARK)[index]));"accent"->state.updateSettings(state.settings.copy(accent=AccentPreference.entries[index]));"language"->state.updateSettings(state.settings.copy(language=listOf(LanguagePreference.CHINESE,LanguagePreference.ENGLISH)[index]));"currency"->state.updateSettings(state.settings.copy(defaultCurrency=option))};dialog.value=""}){Text(option)}}}},confirmButton={})
    if(versionInfo.value) AlertDialog(onDismissRequest={versionInfo.value=false},title={Text(t("版本信息", "Version information"))},text={Text("${t("轻记账", "Simple Ledger")}\n${t("当前版本", "Current version")}: ${BuildConfig.VERSION_NAME}")},confirmButton={TextButton(onClick={versionInfo.value=false}){Text(t("确定", "OK"))}})
    if(clearConfirmation.value) AlertDialog(onDismissRequest={clearConfirmation.value=false},title={Text(t("清空所有数据？","Clear all data?"))},text={Text(t("此操作会删除所有账单和自建账户，并恢复默认设置，且无法撤销。","This deletes all transactions and custom accounts, restores defaults, and cannot be undone."))},confirmButton={TextButton(onClick={state.clearAllData();clearConfirmation.value=false}){Text(t("确认清空","Clear"))}},dismissButton={TextButton(onClick={clearConfirmation.value=false}){Text(t("取消","Cancel"))}})
}

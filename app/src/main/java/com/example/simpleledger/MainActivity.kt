package com.example.simpleledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.simpleledger.state.AppState
import com.example.simpleledger.ui.SettingsScreen
import com.example.simpleledger.data.ThemePreference
import com.example.simpleledger.data.AccentPreference
import com.example.simpleledger.data.LanguagePreference
import com.example.simpleledger.data.Transaction
import com.example.simpleledger.data.LedgerStorage
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { LedgerApp() } }
}

@Composable
fun LedgerApp() {
    var page by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appState = remember { AppState(LedgerStorage(context)) }
    val useDark = when (appState.settings.theme) { ThemePreference.DARK -> true; ThemePreference.LIGHT -> false; ThemePreference.SYSTEM -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = ledgerColorScheme(useDark, state = appState)) {
        if (showSettings) SettingsScreen(appState) { showSettings = false }
        else Scaffold(bottomBar = { NavigationBar { listOf(tr(appState,"主页","Home"), tr(appState,"账户","Accounts"), tr(appState,"统计","Statistics")).forEachIndexed { index, label -> NavigationBarItem(selected = page == index, onClick = { page = index }, icon = {}, label = { Text(label, style = MaterialTheme.typography.titleSmall) }) } } }) { padding ->
            when (page) { 0 -> HomeScreen(Modifier.padding(padding), appState, onSettings = { showSettings = true }); 1 -> AccountsScreen(Modifier.padding(padding), appState); else -> StatisticsScreen(Modifier.padding(padding), appState) }
        }
    }
}

private fun tr(state: AppState, chinese: String, english: String) = if (state.settings.language == LanguagePreference.ENGLISH) english else chinese

private fun ledgerColorScheme(dark: Boolean, state: AppState) = if (dark) {
    val accent = accentColor(state.settings.accent)
    val onAccent = if (state.settings.accent == AccentPreference.YELLOW || state.settings.accent == AccentPreference.ORANGE) Color(0xFF1B1B1B) else Color.White
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        background = Color(0xFF252A33),
        onBackground = Color(0xFFF0F2F6),
        surface = Color(0xFF2D333D),
        onSurface = Color(0xFFF0F2F6),
        surfaceVariant = Color(0xFF3A424F),
        onSurfaceVariant = Color(0xFFD9E1EB),
        outline = Color(0xFFABB6C5)
    )
} else {
    val accent = accentColor(state.settings.accent)
    val onAccent = if (state.settings.accent == AccentPreference.YELLOW || state.settings.accent == AccentPreference.ORANGE) Color(0xFF1B1B1B) else Color.White
    lightColorScheme(primary = accent, secondary = accent, onPrimary = onAccent, onSecondary = onAccent)
}

private fun accentColor(accent: AccentPreference) = when (accent) {
    AccentPreference.RED -> Color(0xFFD94343)
    AccentPreference.ORANGE -> Color(0xFFE87920)
    AccentPreference.YELLOW -> Color(0xFFF0A500)
    AccentPreference.BLUE -> Color(0xFF2563EB)
    AccentPreference.GREEN -> Color(0xFF159B68)
    AccentPreference.PINK -> Color(0xFFD94C8A)
    AccentPreference.PURPLE -> Color(0xFF7C4DCC)
}

private fun currencySymbol(currency: String) = when (currency.uppercase()) {
    "CNY", "RMB" -> "¥"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"
    "HKD" -> "HK$"
    "TWD" -> "NT$"
    else -> currency
}

private val IncomeColor = Color(0xFF28B487)
private val ExpenseColor = Color(0xFFE85D75)

private fun accountingMonthStart(monthStartDay: Int): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        if (get(java.util.Calendar.DAY_OF_MONTH) < monthStartDay) add(java.util.Calendar.MONTH, -1)
        set(java.util.Calendar.DAY_OF_MONTH, monthStartDay)
    }
    return calendar.timeInMillis
}

@Composable
private fun StatisticsScreen(modifier: Modifier = Modifier, state: AppState) {
    var range by remember { mutableStateOf(6) }
    val availableCurrencies = (state.accounts.map { it.currency } + state.transactions.map { it.currency } + state.settings.defaultCurrency).distinct()
    var selectedCurrency by remember { mutableStateOf(state.settings.defaultCurrency) }
    val statisticsCurrency = selectedCurrency.takeIf { it in availableCurrencies } ?: state.settings.defaultCurrency
    val statisticsTransactions = state.transactions.filter { it.currency == statisticsCurrency }
    val now = java.util.Calendar.getInstance()
    fun monthKey(timeMillis: Long): Int {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timeMillis }
        return calendar.get(java.util.Calendar.YEAR) * 12 + calendar.get(java.util.Calendar.MONTH)
    }
    val currentMonthKey = now.get(java.util.Calendar.YEAR) * 12 + now.get(java.util.Calendar.MONTH)
    var selectedMonthKey by remember { mutableStateOf(currentMonthKey) }
    val availableMonthKeys = statisticsTransactions.map { monthKey(it.dateMillis) }.distinct().sorted()
    val displayedMonthKey = selectedMonthKey.takeIf { it in availableMonthKeys }
        ?: availableMonthKeys.lastOrNull()
        ?: currentMonthKey
    val displayedMonthIndex = availableMonthKeys.indexOf(displayedMonthKey)
    val displayedCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, displayedMonthKey / 12)
        set(java.util.Calendar.MONTH, displayedMonthKey % 12)
    }
    val shownMonthTransactions = statisticsTransactions.filter { monthKey(it.dateMillis) == displayedMonthKey && !it.isTransfer }
    val incomeCategories = shownMonthTransactions.filter { it.isIncome }.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { it.amount } }
    val expenseCategories = shownMonthTransactions.filterNot { it.isIncome }.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { it.amount } }
    val earliest = statisticsTransactions.minOfOrNull { it.dateMillis }
    val allMonths = if (range == 0 && earliest != null) {
        val first = java.util.Calendar.getInstance().apply { timeInMillis = earliest }
        max(1, (now.get(java.util.Calendar.YEAR) - first.get(java.util.Calendar.YEAR)) * 12 + now.get(java.util.Calendar.MONTH) - first.get(java.util.Calendar.MONTH) + 1)
    } else range
    val monthlyData = (allMonths - 1 downTo 0).map { offset ->
        val calendar = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -offset) }
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val values = statisticsTransactions.filter {
            if (it.isTransfer) false else {
                val date = java.util.Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                date.get(java.util.Calendar.YEAR) == year && date.get(java.util.Calendar.MONTH) == month
            }
        }
        MonthlyValue(
            label = "${month + 1}/${year.toString().takeLast(2)}",
            income = values.filter { it.isIncome }.sumOf { it.amount },
            expense = values.filterNot { it.isIncome }.sumOf { it.amount }
        )
    }
    Column(modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr(state,"统计","Statistics"), style = MaterialTheme.typography.headlineLarge)
        Text(tr(state, "统计币种", "Statistics currency"), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableCurrencies.forEach { currency ->
                FilterChip(
                    selected = statisticsCurrency == currency,
                    onClick = { selectedCurrency = currency },
                    label = { Text("${currencySymbol(currency)} $currency") }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                if (state.settings.language == LanguagePreference.ENGLISH) {
                    "${displayedCalendar.get(java.util.Calendar.MONTH) + 1}/${displayedCalendar.get(java.util.Calendar.YEAR)} category share"
                } else {
                    "${displayedCalendar.get(java.util.Calendar.MONTH) + 1}月分类占比"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                TextButton(
                    enabled = displayedMonthIndex > 0,
                    onClick = { selectedMonthKey = availableMonthKeys[displayedMonthIndex - 1] }
                ) { Text("‹ ${tr(state, "上个月", "Previous")}") }
                TextButton(
                    enabled = displayedMonthIndex >= 0 && displayedMonthIndex < availableMonthKeys.lastIndex,
                    onClick = { selectedMonthKey = availableMonthKeys[displayedMonthIndex + 1] }
                ) { Text("${tr(state, "下个月", "Next")} ›") }
            }
        }
        if (incomeCategories.isEmpty() && expenseCategories.isEmpty()) {
            Card { Text(tr(state,"该月暂无账单。","No transactions this month."), Modifier.padding(24.dp)) }
        } else {
            if (expenseCategories.isNotEmpty()) CategoryDonut(tr(state, "支出分类", "Expense categories"), expenseCategories, false, statisticsCurrency, state)
            if (incomeCategories.isNotEmpty()) CategoryDonut(tr(state, "收入分类", "Income categories"), incomeCategories, true, statisticsCurrency, state)
        }
        Text(tr(state,"收入与支出变动","Income and expense trend"), style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = range == 6, onClick = { range = 6 }, label = { Text(tr(state, "近 6 个月", "6 months")) })
            FilterChip(selected = range == 12, onClick = { range = 12 }, label = { Text(tr(state, "近 12 个月", "12 months")) })
            FilterChip(selected = range == 0, onClick = { range = 0 }, label = { Text(tr(state, "全部时间", "All time")) })
        }
        TrendChart(monthlyData, statisticsCurrency, state)
    }
}

private data class MonthlyValue(val label: String, val income: Double, val expense: Double)
private data class CurrencyMonthlyTotal(val currency: String, val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

@Composable
private fun CategoryDonut(title: String, values: Map<String, Double>, income: Boolean, currency: String, state: AppState) {
    val colors = listOf(Color(0xFF4F8EF7), Color(0xFFFF9F43), Color(0xFF28B487), Color(0xFFB26BDE), Color(0xFFE85D75), Color(0xFF4BB3C7))
    val total = values.values.sum().takeIf { it > 0.0 } ?: return
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Canvas(Modifier.size(170.dp)) {
                    var startAngle = -90f
                    values.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                        val sweep = (entry.value / total * 360).toFloat()
                        drawArc(colors[index % colors.size], startAngle, sweep, false, style = Stroke(width = 34f))
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(tr(state, if (income) "收入" else "支出", if (income) "Income" else "Expense"))
                    Text(
                        "${currencySymbol(currency)}${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (income) IncomeColor else ExpenseColor
                    )
                }
            }
            values.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("●  ${entry.key}", color = colors[index % colors.size], style = MaterialTheme.typography.titleMedium)
                    Text(String.format("%.1f%%", entry.value / total * 100), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun TrendChart(values: List<MonthlyValue>, currency: String, state: AppState) {
    val maxValue = max(1.0, values.maxOfOrNull { max(it.income, it.expense) } ?: 1.0)
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.fillMaxWidth().height(190.dp)) {
                val bottom = size.height - 20f
                val top = 12f
                val step = if (values.size <= 1) size.width else size.width / (values.size - 1)
                drawLine(axisColor, Offset(0f, bottom), Offset(size.width, bottom), 1f)
                fun drawSeries(series: (MonthlyValue) -> Double, color: Color) {
                    values.zipWithNext().forEachIndexed { index, pair ->
                        val first = Offset(index * step, bottom - ((pair.first.let(series) / maxValue) * (bottom - top)).toFloat())
                        val second = Offset((index + 1) * step, bottom - ((pair.second.let(series) / maxValue) * (bottom - top)).toFloat())
                        drawLine(color, first, second, 5f)
                    }
                    values.forEachIndexed { index, value ->
                        drawCircle(color, 5f, Offset(index * step, bottom - ((series(value) / maxValue) * (bottom - top)).toFloat()))
                    }
                }
                drawSeries({ it.income }, IncomeColor)
                drawSeries({ it.expense }, ExpenseColor)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("● ${tr(state, "收入", "Income")} (${currencySymbol(currency)})", color = IncomeColor, style = MaterialTheme.typography.titleMedium)
                Text("● ${tr(state, "支出", "Expense")} (${currencySymbol(currency)})", color = ExpenseColor, style = MaterialTheme.typography.titleMedium)
            }
            Text(values.joinToString("   ") { it.label }, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AccountsScreen(modifier: Modifier = Modifier, state: AppState) {
    var search by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<com.example.simpleledger.data.Account?>(null) }
    var pendingEdit by remember { mutableStateOf<com.example.simpleledger.data.Account?>(null) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    val selectedId = selectedAccountId?.takeIf { id -> state.accounts.any { it.id == id } }
    val selectedAccount = state.accounts.find { it.id == selectedId }
    val visibleTransactions = state.transactions.filter { selectedId == null || it.accountId == selectedId }
    val monthStart = accountingMonthStart(state.settings.monthStartDay)
    val monthlyTransactions = visibleTransactions.filter { it.dateMillis >= monthStart }
    val accountsInScope = selectedAccount?.let { listOf(it) } ?: state.accounts.toList()
    val accountCurrencies = accountsInScope.map { it.currency }.distinct()
        .sortedBy { it != state.settings.defaultCurrency }
    fun balanceFor(currency: String) = accountsInScope.sumOf { account ->
        (if (account.currency == currency) account.initialBalance else 0.0) + state.transactions
            .filter { it.accountId == account.id && it.currency == currency }
            .sumOf { if (it.isIncome) it.amount else -it.amount }
    }
    fun incomeFor(currency: String) = monthlyTransactions.filter { it.currency == currency && it.isIncome && !it.isTransfer }.sumOf { it.amount }
    fun expenseFor(currency: String) = monthlyTransactions.filter { it.currency == currency && !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val query = search.trim().lowercase()
    val filteredTransactions = visibleTransactions
        .sortedByDescending { it.dateMillis }
        .filter { transaction ->
            if (query.isBlank()) true else {
                val matchesAmount = query.toDoubleOrNull()?.let { it == transaction.amount } ?: false
                matchesAmount || listOf(
                    transaction.category,
                    transaction.note,
                    currencySymbol(transaction.currency),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.dateMillis))
                ).any { it.lowercase().contains(query) }
            }
        }
    Column(modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr(state,"账户","Accounts"), style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedId == null,
                onClick = { selectedAccountId = null },
                label = { Text(tr(state, "总账户", "All accounts")) }
            )
            state.accounts.forEach { account ->
                FilterChip(
                    selected = selectedId == account.id,
                    onClick = { selectedAccountId = account.id },
                    label = { Text(account.name) }
                )
            }
        }
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedAccount?.name ?: tr(state, "总账户", "All accounts"), style = MaterialTheme.typography.titleLarge)
                    if (selectedAccount != null) Row {
                        TextButton(onClick = { pendingEdit = selectedAccount }) { Text(tr(state, "修改", "Edit")) }
                        TextButton(onClick = { pendingDelete = selectedAccount }) { Text(tr(state, "删除", "Delete")) }
                    }
                }
                accountCurrencies.forEach { accountCurrency ->
                    Text("${currencySymbol(accountCurrency)}${String.format("%.2f", balanceFor(accountCurrency))}", style = MaterialTheme.typography.headlineLarge)
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(state, "当月收入", "Monthly income"), style = MaterialTheme.typography.bodyMedium)
                            Text("+${currencySymbol(accountCurrency)}${String.format("%.2f", incomeFor(accountCurrency))}", color = IncomeColor)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tr(state, "当月支出", "Monthly expense"), style = MaterialTheme.typography.bodyMedium)
                            Text("-${currencySymbol(accountCurrency)}${String.format("%.2f", expenseFor(accountCurrency))}", color = ExpenseColor)
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr(state, "搜索账单", "Search transactions")) },
            supportingText = { Text(tr(state, "可搜索分类、备注、日期和金额", "Search category, note, date, or amount")) }
        )
        Text(tr(state,"账单","Transactions"), style = MaterialTheme.typography.titleLarge)
        if (filteredTransactions.isEmpty()) {
            Text(if (search.isBlank()) tr(state,"该账户还没有账单。","No transactions in this account.") else tr(state,"没有匹配的账单。","No matching transactions."))
        } else {
            filteredTransactions.forEach { item ->
                val accountName = state.accounts.find { it.id == item.accountId }?.name.orEmpty()
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.note.ifBlank { item.category }, style = MaterialTheme.typography.titleMedium)
                            Text("${item.category} · $accountName · ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.dateMillis))}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            (if (item.isIncome) "+" else "-") + currencySymbol(item.currency) + String.format("%.2f", item.amount),
                            color = if (item.isTransfer) MaterialTheme.colorScheme.onSurface else if (item.isIncome) IncomeColor else ExpenseColor
                        )
                    }
                }
            }
        }
    }
    if (pendingDelete != null) {
        val account = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr(state, "删除账户", "Delete account")) },
            text = { Text(tr(state, "删除“${account.name}”也会删除该账户内的所有账单。", "Deleting “${account.name}” also deletes all of its transactions.")) },
            confirmButton = {
                TextButton(onClick = { state.deleteAccount(account.id); pendingDelete = null }) {
                    Text(tr(state, "删除", "Delete"))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(tr(state, "取消", "Cancel")) } }
        )
    }
    if (pendingEdit != null) {
        EditAccountDialog(state = state, account = pendingEdit!!, onDismiss = { pendingEdit = null })
    }
}

@Composable
private fun AddAccountDialog(state: AppState, onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    val english = state.settings.language == LanguagePreference.ENGLISH
    fun t(chinese: String, englishText: String) = if (english) englishText else chinese
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(state.settings.defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val currencies = listOf("CNY", "USD", "EUR", "GBP", "JPY", "HKD", "TWD")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("新增账户", "Add account")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("账户名称", "Account name"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text(t("账户名称", "Account name")) })
                Text(t("初始余额", "Initial balance"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(balance, { value -> if (value.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) balance = value }, label = { Text(t("初始余额", "Initial balance")) }, singleLine = true)
                Text(t("币种", "Currency"), style = MaterialTheme.typography.titleMedium)
                Box {
                    OutlinedButton(
                        onClick = { currencyExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("${currencySymbol(currency)}  $currency") }
                    DropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                        currencies.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${currencySymbol(item)}  $item") },
                                onClick = { currency = item; currencyExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, balance.toDoubleOrNull() ?: 0.0, currency) }) { Text(t("保存", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("取消", "Cancel")) } }
    )
}

@Composable
private fun EditAccountDialog(state: AppState, account: com.example.simpleledger.data.Account, onDismiss: () -> Unit) {
    val english = state.settings.language == LanguagePreference.ENGLISH
    fun t(chinese: String, englishText: String) = if (english) englishText else chinese
    var name by remember(account.id) { mutableStateOf(account.name) }
    var balance by remember(account.id) { mutableStateOf(account.initialBalance.toString()) }
    var currency by remember(account.id) { mutableStateOf(account.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val hasTransactions = state.transactions.any { it.accountId == account.id }
    val currencies = listOf("CNY", "USD", "EUR", "GBP", "JPY", "HKD", "TWD")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("修改账户", "Edit account")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("账户名称", "Account name"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(t("账户名称", "Account name")) })
                if (hasTransactions) {
                    Text(t("该账户已有账单，因此只能修改名称。历史账单会自动显示新名称。", "This account already has transactions, so only its name can be changed. Existing transactions will use the new name automatically."), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(t("起始金额", "Initial balance"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = balance, onValueChange = { value -> if (value.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) balance = value }, label = { Text(t("起始金额", "Initial balance")) }, singleLine = true)
                    Text(t("币种", "Currency"), style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(onClick = { currencyExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("${currencySymbol(currency)}  $currency") }
                        DropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                            currencies.forEach { item -> DropdownMenuItem(text = { Text("${currencySymbol(item)}  $item") }, onClick = { currency = item; currencyExpanded = false }) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (hasTransactions) state.updateAccount(account.id, name)
                else state.updateAccount(account.id, name, balance.toDoubleOrNull() ?: 0.0, currency)
                onDismiss()
            }) { Text(t("保存", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("取消", "Cancel")) } }
    )
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, state: AppState, onSettings: () -> Unit) {
    var showEntry by remember { mutableStateOf(false) }
    var showAddAccount by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    val monthStart = accountingMonthStart(state.settings.monthStartDay)
    val calculatedMonthlyTotals = state.transactions
        .filter { it.dateMillis >= monthStart && !it.isTransfer }
        .groupBy { it.currency }
        .map { (currency, items) ->
            CurrencyMonthlyTotal(
                currency = currency,
                income = items.filter { it.isIncome }.sumOf { it.amount },
                expense = items.filterNot { it.isIncome }.sumOf { it.amount }
            )
        }
        .sortedBy { it.currency != state.settings.defaultCurrency }
    val monthlyTotals = if (calculatedMonthlyTotals.isEmpty()) {
        listOf(CurrencyMonthlyTotal(state.settings.defaultCurrency, 0.0, 0.0))
    } else calculatedMonthlyTotals
    Column(modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr(state,"轻记账","Simple Ledger"), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onSettings) { Text("⚙") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(state,"本月结余","Monthly balance"))
                monthlyTotals.forEach { total ->
                    Text(
                        "${if (total.balance < 0) "-" else ""}${currencySymbol(total.currency)}${String.format("%.2f", kotlin.math.abs(total.balance))}",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Row {
                        Text(tr(state,"收入","Income") + " +${currencySymbol(total.currency)}" + String.format("%.2f", total.income), Modifier.weight(1f), color = IncomeColor)
                        Text(tr(state,"支出","Expense") + " -${currencySymbol(total.currency)}" + String.format("%.2f", total.expense), color = ExpenseColor)
                    }
                }
            }
        }
        Button(onClick = { showEntry = true }, modifier = Modifier.fillMaxWidth()) { Text(tr(state,"记账","Add record")) }
        Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(tr(state,"账户余额","Account balance"), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick={showAddAccount=true}) { Text(tr(state,"新增账户","Add account")) }
        }
        state.accounts.forEach { account ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    val balance = account.initialBalance + state.transactions.filter { it.accountId == account.id && it.currency == account.currency }
                        .sumOf { if (it.isIncome) it.amount else -it.amount }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(account.currency)
                        Text("${currencySymbol(account.currency)}${String.format("%.2f", balance)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Text(tr(state,"最近账单","Recent transactions"), style = MaterialTheme.typography.titleLarge)
        val recent = state.transactions.sortedByDescending { it.dateMillis }.take(8)
        if (recent.isEmpty()) Text(tr(state,"还没有账目，点击上方按钮开始记账。","No transactions yet."))
        else recent.forEach { item ->
            val account = state.accounts.find { it.id == item.accountId }?.name ?: ""
            Card(modifier=Modifier.clickable { selectedTransaction=item }) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(item.note.ifBlank { item.category }); Text("${item.category} · $account · ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.dateMillis))}", style=MaterialTheme.typography.bodySmall) }; Text((if(item.isIncome) "+" else "-") + currencySymbol(item.currency) + String.format("%.2f", item.amount), color = if (item.isTransfer) MaterialTheme.colorScheme.onSurface else if (item.isIncome) IncomeColor else ExpenseColor) } }
        }
    }
    if (showEntry) EntryDialog(state, onDismiss = { showEntry = false })
    if (editingTransaction != null) {
        EntryDialog(
            state = state,
            existingTransaction = editingTransaction!!,
            onDismiss = { editingTransaction = null }
        )
    }
    if(showAddAccount) AddAccountDialog(state = state, onDismiss={showAddAccount=false},onSave={name,balance,currency->state.addAccount(name,balance,currency);showAddAccount=false})
    if (selectedTransaction != null) {
        val item = selectedTransaction!!
        AlertDialog(
            onDismissRequest = { selectedTransaction = null },
            title = { Text("账单操作") },
            text = { Text("可修改或删除这笔账单。") },
            confirmButton = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { state.deleteTransaction(item.id); selectedTransaction = null }) { Text("删除") }
                    TextButton(onClick = {
                        selectedTransaction = null
                        editingTransaction = item
                    }) { Text("修改") }
                    TextButton(onClick = { selectedTransaction = null }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val preferences = LocalContext.current.getSharedPreferences("ledger_settings", android.content.Context.MODE_PRIVATE)
    var theme by remember { mutableStateOf(preferences.getString("theme", "跟随系统")!!) }; var language by remember { mutableStateOf(preferences.getString("language", "中文")!!) }; var currency by remember { mutableStateOf(preferences.getString("currency", "人民币（¥）")!!) }; var day by remember { mutableStateOf(preferences.getInt("month_start_day", 1)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("设置") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { theme = if (theme == "跟随系统") "亮色" else if (theme == "亮色") "暗色" else "跟随系统"; preferences.edit().putString("theme", theme).apply() }) { Text("主题：$theme") }
        TextButton(onClick = { language = if (language == "中文") "English" else "中文"; preferences.edit().putString("language", language).apply() }) { Text("语言：$language") }
        TextButton(onClick = { currency = if (currency.startsWith("人民币")) "美元（$）" else "人民币（¥）"; preferences.edit().putString("currency", currency).apply() }) { Text("默认币种：$currency") }
        TextButton(onClick = { day = if (day == 28) 1 else day + 1; preferences.edit().putInt("month_start_day", day).apply() }) { Text("每月起始日：$day 日") }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}

@Composable
private fun EntryDialog(
    state: AppState,
    existingTransaction: Transaction? = null,
    onDismiss: () -> Unit
) {
    val accounts = state.accounts.sortedWith(compareBy { it.currency != state.settings.defaultCurrency })
    val context = LocalContext.current
    val english = state.settings.language == LanguagePreference.ENGLISH
    fun t(chinese: String, englishText: String) = if (english) englishText else chinese
    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString().orEmpty()) }
    var category by remember { mutableStateOf(existingTransaction?.category.orEmpty()) }
    var note by remember { mutableStateOf(existingTransaction?.note.orEmpty()) }
    var accountId by remember { mutableStateOf(existingTransaction?.accountId ?: accounts.firstOrNull()?.id ?: "cash") }
    var income by remember { mutableStateOf(existingTransaction?.isIncome ?: false) }
    var isTransfer by remember { mutableStateOf(existingTransaction?.isTransfer ?: false) }
    var toAccountId by remember { mutableStateOf(accounts.firstOrNull { it.id != accountId }?.id.orEmpty()) }
    var dateMillis by remember { mutableStateOf(existingTransaction?.dateMillis ?: System.currentTimeMillis()) }
    var showCalculator by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf(existingTransaction?.currency ?: accounts.find { it.id == accountId }?.currency ?: state.settings.defaultCurrency) }
    val categories = if (income) {
        if (english) listOf("Salary", "Bonus", "Freelance", "Refund", "Investment", "Foreign currency received", "Other income")
        else listOf("工资", "奖金", "兼职", "退款", "投资收益", "购入外汇", "其他收入")
    } else {
        if (english) listOf("Food", "Shopping", "Transport", "Travel", "Bills", "Entertainment", "Medical", "Buy foreign currency", "Other expense")
        else listOf("餐饮", "购物", "交通", "旅行", "固定账单", "娱乐", "医疗", "购买外汇", "其他支出")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toDoubleOrNull() ?: return@TextButton
                if (value <= 0.0) return@TextButton
                if (isTransfer) {
                    if (toAccountId.isBlank() || toAccountId == accountId) return@TextButton
                    state.addTransfer(accountId, toAccountId, value, currency, dateMillis)
                    onDismiss()
                    return@TextButton
                }
                val transaction = Transaction(
                        id = existingTransaction?.id ?: UUID.randomUUID().toString(),
                        accountId = accountId,
                        amount = value,
                        isIncome = income,
                        category = category.ifBlank { if (income) t("其他收入", "Other income") else t("其他支出", "Other expense") },
                        note = note,
                        currency = currency,
                        dateMillis = dateMillis
                    )
                if (existingTransaction == null) state.addTransaction(transaction) else state.updateTransaction(transaction)
                onDismiss()
            }) { Text(t("保存", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("取消", "Cancel")) } },
        title = { Text(if (existingTransaction == null) t("新增账单", "Add transaction") else t("修改账单", "Edit transaction")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(t("类型", "Type"), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !income && !isTransfer, onClick = { income = false; isTransfer = false }, label = { Text(t("支出", "Expense")) })
                    FilterChip(selected = income && !isTransfer, onClick = { income = true; isTransfer = false }, label = { Text(t("收入", "Income")) })
                    FilterChip(selected = isTransfer, onClick = { isTransfer = true }, label = { Text(t("转账", "Transfer")) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(t("金额", "Amount"), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showCalculator = true }) { Text(t("计算器", "Calculator")) }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> if (value.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) amount = value },
                    singleLine = true
                )
                Text(t("币种", "Currency"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = "${currencySymbol(currency)}  $currency", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())
                Text(if (isTransfer) t("转出账户", "From account") else t("账户", "Account"), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = account.id == accountId,
                            onClick = {
                                accountId = account.id
                                currency = account.currency
                                val currentTarget = accounts.find { it.id == toAccountId }
                                if (toAccountId == account.id || currentTarget?.currency != account.currency) {
                                    toAccountId = accounts.firstOrNull { it.id != account.id && it.currency == account.currency }?.id.orEmpty()
                                }
                            },
                            label = { Text(account.name) }
                        )
                    }
                }
                if (isTransfer) {
                    Text(t("转入账户", "To account"), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        accounts.filter { it.id != accountId && it.currency == currency }.forEach { account ->
                            FilterChip(
                                selected = account.id == toAccountId,
                                onClick = { toAccountId = account.id },
                                label = { Text(account.name) }
                            )
                        }
                    }
                } else {
                    Text(t("分类", "Category"), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        categories.forEach { item ->
                            FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                        }
                    }
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(t("自定义分类", "Custom category")) })
                }
                Text(t("备注", "Note"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(t("可选", "Optional")) })
                Text(t("日期", "Date"), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            dateMillis = calendar.timeInMillis
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text(
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateMillis)),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    )
    if (showCalculator) {
        CalculatorDialog(
            initialValue = amount,
            english = english,
            onResult = { result -> amount = result; showCalculator = false },
            onDismiss = { showCalculator = false }
        )
    }
}

@Composable
private fun CalculatorDialog(initialValue: String, english: Boolean, onResult: (String) -> Unit, onDismiss: () -> Unit) {
    var expression by remember { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf(false) }
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "−"),
        listOf(".", "0", "⌫", "+")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (english) "Calculator" else "计算器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                if (error) Text(if (english) "Enter a valid expression" else "请输入有效算式", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { expression = ""; error = false }) { Text(if (english) "Clear" else "清除") }
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    error = false
                                    expression = when (key) {
                                        "⌫" -> expression.dropLast(1)
                                        "÷" -> expression + "/"
                                        "×" -> expression + "*"
                                        "−" -> expression + "-"
                                        "." -> {
                                            val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
                                            if (currentNumber.contains('.')) expression
                                            else if (currentNumber.isBlank()) expression + "0."
                                            else expression + "."
                                        }
                                        else -> {
                                            val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
                                            val fraction = currentNumber.substringAfter('.', "")
                                            if (currentNumber.contains('.') && fraction.length >= 2) expression else expression + key
                                        }
                                    }
                                }
                            ) { Text(key) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = calculateExpression(expression)
                if (result == null) error = true else onResult(result)
            }) { Text("＝") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (english) "Cancel" else "取消") } }
    )
}

private fun calculateExpression(expression: String): String? {
    val cleaned = expression.replace(" ", "")
    if (cleaned.isBlank() || !cleaned.matches(Regex("[0-9.+\\-*/]+"))) return null
    val tokens = Regex("\\d+(?:\\.\\d+)?|[+\\-*/]").findAll(cleaned).map { it.value }.toList()
    if (tokens.joinToString("") != cleaned || tokens.isEmpty() || tokens.size % 2 == 0) return null
    if (tokens.indices.any { index -> if (index % 2 == 0) tokens[index].toDoubleOrNull() == null else tokens[index] !in listOf("+", "-", "*", "/") }) return null
    return try {
        // Calculate multiplication and division first, then addition and subtraction.
        val reducedValues = mutableListOf(java.math.BigDecimal(tokens[0]))
        val reducedOperators = mutableListOf<String>()
        var index = 1
        while (index < tokens.size) {
            val operator = tokens[index]
            val next = java.math.BigDecimal(tokens[index + 1])
            if (operator == "*") reducedValues[reducedValues.lastIndex] = reducedValues.last() * next
            else if (operator == "/") {
                if (next.compareTo(java.math.BigDecimal.ZERO) == 0) return null
                reducedValues[reducedValues.lastIndex] = reducedValues.last().divide(next, 2, java.math.RoundingMode.HALF_UP)
            } else {
                reducedOperators += operator
                reducedValues += next
            }
            index += 2
        }
        var result = reducedValues[0]
        reducedOperators.forEachIndexed { operatorIndex, operator ->
            result = if (operator == "+") result + reducedValues[operatorIndex + 1] else result - reducedValues[operatorIndex + 1]
        }
        result.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    } catch (_: Exception) { null }
}

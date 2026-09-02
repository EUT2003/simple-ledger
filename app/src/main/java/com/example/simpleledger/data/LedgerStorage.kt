package com.example.simpleledger.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LedgerStorage(context: Context) {
    private val prefs = context.getSharedPreferences("simple_ledger_data", Context.MODE_PRIVATE)

    fun saveAccounts(accounts: List<Account>) { prefs.edit().putString("accounts", JSONArray().apply { accounts.forEach { account -> put(JSONObject().put("id",account.id).put("name",account.name).put("balance",account.initialBalance).put("currency",account.currency)) } }.toString()).apply() }
    fun loadAccounts(): List<Account> = try { val data=JSONArray(prefs.getString("accounts","[]")); List(data.length()){i->data.getJSONObject(i).let{ item -> Account(item.getString("id"),item.getString("name"),item.optDouble("balance"),item.optString("currency","CNY"))}} } catch(_:Exception){ emptyList() }
    fun saveTransactions(items: List<Transaction>) { prefs.edit().putString("transactions", JSONArray().apply { items.forEach { put(JSONObject().put("id",it.id).put("accountId",it.accountId).put("amount",it.amount).put("income",it.isIncome).put("category",it.category).put("note",it.note).put("currency",it.currency).put("date",it.dateMillis).put("transfer",it.isTransfer).put("transferGroupId",it.transferGroupId)) } }.toString()).apply() }
    fun loadTransactions(): List<Transaction> = try { val data=JSONArray(prefs.getString("transactions","[]")); List(data.length()){i->data.getJSONObject(i).let{Transaction(it.getString("id"),it.getString("accountId"),it.getDouble("amount"),it.getBoolean("income"),it.getString("category"),it.optString("note"),it.optString("currency","CNY"),it.getLong("date"),it.optBoolean("transfer",false),it.optString("transferGroupId").ifBlank { null })}} } catch(_:Exception){ emptyList() }
    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString("settings", JSONObject()
            .put("theme", settings.theme.name)
            .put("accent", settings.accent.name)
            .put("language", settings.language.name)
            .put("currency", settings.defaultCurrency)
            .put("monthStartDay", settings.monthStartDay)
            .toString()).apply()
    }
    fun loadSettings(): AppSettings = try {
        val data = JSONObject(prefs.getString("settings", "{}") ?: "{}")
        AppSettings(
            theme = ThemePreference.valueOf(data.optString("theme", ThemePreference.SYSTEM.name)),
            accent = AccentPreference.valueOf(data.optString("accent", AccentPreference.BLUE.name)),
            language = LanguagePreference.valueOf(data.optString("language", LanguagePreference.CHINESE.name)),
            defaultCurrency = data.optString("currency", "CNY"),
            monthStartDay = data.optInt("monthStartDay", 1).coerceIn(1, 28)
        )
    } catch (_: Exception) { AppSettings() }
    fun clearAll() { prefs.edit().clear().apply() }
}

package com.example.simpleledger.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.simpleledger.data.Account
import com.example.simpleledger.data.AppSettings
import com.example.simpleledger.data.Transaction
import com.example.simpleledger.data.LedgerStorage
import java.util.UUID

class AppState(private val storage: LedgerStorage) {
    var settings by mutableStateOf(storage.loadSettings())
        private set

    val accounts = mutableStateListOf<Account>()
    val transactions = mutableStateListOf<Transaction>()

    init { accounts += storage.loadAccounts().ifEmpty { listOf(Account(id = "cash", name = "现金")) }; transactions += storage.loadTransactions(); storage.saveAccounts(accounts) }

    fun updateSettings(value: AppSettings) { settings = value; storage.saveSettings(value) }

    fun addAccount(name: String, initialBalance: Double, currency: String) {
        if (name.isNotBlank()) { accounts += Account(UUID.randomUUID().toString(), name.trim(), initialBalance, currency); storage.saveAccounts(accounts) }
    }

    fun updateAccount(id: String, name: String, initialBalance: Double? = null, currency: String? = null) {
        val index = accounts.indexOfFirst { it.id == id }
        if (index < 0 || name.isBlank()) return
        val previous = accounts[index]
        accounts[index] = previous.copy(
            name = name.trim(),
            initialBalance = initialBalance ?: previous.initialBalance,
            currency = currency ?: previous.currency
        )
        storage.saveAccounts(accounts)
    }

    /** Returns false when the user tries to remove the only remaining account. */
    fun deleteAccount(id: String): Boolean {
        if (accounts.size <= 1) return false
        accounts.removeAll { it.id == id }
        transactions.removeAll { it.accountId == id }
        storage.saveAccounts(accounts)
        storage.saveTransactions(transactions)
        return true
    }

    fun clearAllData() {
        storage.clearAll()
        accounts.clear()
        accounts += Account(id = "cash", name = "现金")
        transactions.clear()
        settings = AppSettings()
        storage.saveAccounts(accounts)
        storage.saveSettings(settings)
    }

    fun addTransaction(value: Transaction) { transactions += value; storage.saveTransactions(transactions) }
    fun addTransfer(fromAccountId: String, toAccountId: String, amount: Double, currency: String, dateMillis: Long) {
        if (fromAccountId == toAccountId || amount <= 0.0) return
        if (accounts.find { it.id == fromAccountId }?.currency != currency || accounts.find { it.id == toAccountId }?.currency != currency) return
        val groupId = UUID.randomUUID().toString()
        transactions += Transaction(UUID.randomUUID().toString(), fromAccountId, amount, false, "转账", currency = currency, dateMillis = dateMillis, isTransfer = true, transferGroupId = groupId)
        transactions += Transaction(UUID.randomUUID().toString(), toAccountId, amount, true, "转账", currency = currency, dateMillis = dateMillis, isTransfer = true, transferGroupId = groupId)
        storage.saveTransactions(transactions)
    }
    fun deleteTransaction(id: String) {
        val item = transactions.find { it.id == id }
        if (item?.isTransfer == true && item.transferGroupId != null) {
            transactions.removeAll { it.transferGroupId == item.transferGroupId }
        } else {
            transactions.removeAll { it.id == id }
        }
        storage.saveTransactions(transactions)
    }
    fun updateTransaction(value: Transaction) { val index=transactions.indexOfFirst { it.id==value.id }; if(index>=0) { transactions[index]=value; storage.saveTransactions(transactions) } }
}

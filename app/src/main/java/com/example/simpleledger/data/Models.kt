package com.example.simpleledger.data

data class Account(
    val id: String,
    val name: String,
    val initialBalance: Double = 0.0,
    val currency: String = "CNY"
)

data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val note: String = "",
    val currency: String = "CNY",
    val dateMillis: Long = System.currentTimeMillis(),
    val isTransfer: Boolean = false,
    val transferGroupId: String? = null
)

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class AccentPreference { RED, ORANGE, YELLOW, BLUE, GREEN, PINK, PURPLE }
enum class LanguagePreference { SYSTEM, CHINESE, ENGLISH }

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val accent: AccentPreference = AccentPreference.BLUE,
    val language: LanguagePreference = LanguagePreference.CHINESE,
    val defaultCurrency: String = "CNY",
    val monthStartDay: Int = 1
)

package com.tippool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.math.floor

data class TierConfig(
    val pct: Int,
    val label: String,
    val defaultCount: Int = 0,
)

val TIERS = listOf(
    TierConfig(100, "100%", defaultCount = 7),
    TierConfig(75,  "75%"),
    TierConfig(50,  "50%"),
    TierConfig(25,  "25%"),
)

data class TierResult(
    val pct: Int,
    val label: String,
    val count: Int,
    val flooredPayout: Int,
    val subtotal: Int,
)

data class CalculationResult(
    val pool: Int,
    val cardTips: Double,
    val cashTips: Double,
    val totalShares: Double,
    val valuePerShare: Double,
    val tierResults: List<TierResult>,
    val totalPaidOut: Int,
    val extraDollars: Int,
)

class TipPoolViewModel : ViewModel() {

    var counts by mutableStateOf(TIERS.associate { it.pct to it.defaultCount })
        private set

    var cardTips by mutableStateOf("")
    var cashTips  by mutableStateOf("")

    var result by mutableStateOf<CalculationResult?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val totalEmployees get() = counts.values.sum()

    val liveTotal: Double get() {
        val c = cardTips.toDoubleOrNull() ?: 0.0
        val ca = cashTips.toDoubleOrNull() ?: 0.0
        return c + ca
    }

    fun setCount(pct: Int, value: Int) {
        counts = counts.toMutableMap().apply { this[pct] = maxOf(0, value) }
    }

    fun calculate() {
        error = null
        result = null

        val card = cardTips.toDoubleOrNull() ?: 0.0
        val cash  = cashTips.toDoubleOrNull()  ?: 0.0
        val total = card + cash

        val cents = Math.round(total * 100) % 100
        if (cents != 0L) {
            error = "$${String.format("%.2f", card)} + $${String.format("%.2f", cash)} " +
                    "= $${String.format("%.2f", total)} — total must be a whole dollar amount."
            return
        }

        val pool = total.toInt()
        if (pool == 0)            { error = "Total tip pool is \$0."; return }
        if (totalEmployees == 0)  { error = "Add at least one employee."; return }

        val totalShares = TIERS.sumOf { counts[it.pct]!! * (it.pct / 100.0) }
        val valuePerShare = pool / totalShares

        val tierResults = TIERS
            .filter { (counts[it.pct] ?: 0) > 0 }
            .map { tier ->
                val raw     = valuePerShare * (tier.pct / 100.0)
                val floored = floor(raw).toInt()
                val count   = counts[tier.pct]!!
                TierResult(
                    pct            = tier.pct,
                    label          = tier.label,
                    count          = count,
                    flooredPayout  = floored,
                    subtotal       = floored * count,
                )
            }

        val totalPaidOut = tierResults.sumOf { it.subtotal }
        val extras = pool - totalPaidOut

        result = CalculationResult(
            pool           = pool,
            cardTips       = card,
            cashTips       = cash,
            totalShares    = totalShares,
            valuePerShare  = valuePerShare,
            tierResults    = tierResults,
            totalPaidOut   = totalPaidOut,
            extraDollars   = extras,
        )
    }

    fun reset() {
        result = null
        error  = null
        cardTips = ""
        cashTips  = ""
        counts = TIERS.associate { it.pct to it.defaultCount }
    }
}
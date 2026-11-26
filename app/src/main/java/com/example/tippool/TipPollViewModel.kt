package com.tippool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
    /** Cents below the whole-dollar pool; shown as "Leave for next pool", never stored. */
    val leftoverCents: Int = 0,
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
        // Same parser as calculate(). If either box holds something calculate() would refuse, show no
        // total (0.0 hides the line) rather than a total that quietly leaves that box out.
        val c = parseCents(cardTips) ?: return 0.0
        val ca = parseCents(cashTips) ?: return 0.0
        return (c + ca) / 100.0
    }

    fun setCount(pct: Int, value: Int) {
        counts = counts.toMutableMap().apply { this[pct] = maxOf(0, value) }
    }

    /** The math lives in TipPoolMath.kt (pure, integer cents, unit-tested); this only maps it to the screen. */
    fun calculate() {
        error = null
        result = null

        val cardAmount = parseAmount(cardTips)
        val cashAmount = parseAmount(cashTips)
        if (cardAmount is Amount.Problem) { error = amountMessage("Card tips", cardAmount.kind); return }
        if (cashAmount is Amount.Problem) { error = amountMessage("Cash tips", cashAmount.kind); return }
        val card = (cardAmount as Amount.Cents).value
        val cash = (cashAmount as Amount.Cents).value

        when (val outcome = calculatePool(card, cash, counts)) {
            is PoolOutcome.Rejected -> error = rejectionMessage(outcome.reason)
            is PoolOutcome.Split -> {
                val tierResults = outcome.tiers.map { t ->
                    TierResult(
                        pct           = t.pct,
                        label         = TIERS.first { it.pct == t.pct }.label,
                        count         = t.count,
                        flooredPayout = t.payout,
                        subtotal      = t.subtotal,
                    )
                }
                // Shown in the "Math breakdown" only; the payouts above come from the integer math.
                val totalShares = outcome.totalSharesPct / 100.0
                result = CalculationResult(
                    pool           = outcome.poolDollars,
                    cardTips       = card / 100.0,
                    cashTips       = cash / 100.0,
                    totalShares    = totalShares,
                    valuePerShare  = outcome.poolDollars / totalShares,
                    tierResults    = tierResults,
                    totalPaidOut   = outcome.totalPaidOut,
                    extraDollars   = outcome.extraDollars,
                    leftoverCents  = outcome.leftoverCents,
                )
            }
        }
    }

    fun reset() {
        result = null
        error  = null
        cardTips = ""
        cashTips  = ""
        counts = TIERS.associate { it.pct to it.defaultCount }
    }
}
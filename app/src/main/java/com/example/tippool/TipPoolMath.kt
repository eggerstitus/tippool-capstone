package com.tippool

/** One tier's result. Payouts and subtotals are whole dollars. */
data class TierSplit(val pct: Int, val count: Int, val payout: Int, val subtotal: Int)

enum class Rejection { NEGATIVE_AMOUNT, LESS_THAN_ONE_DOLLAR, NO_EMPLOYEES }

sealed interface PoolOutcome {
    data class Split(
        val poolDollars: Int,
        /** Cents below the whole-dollar pool. Display only; never stored or carried. */
        val leftoverCents: Int,
        val tiers: List<TierSplit>,
        val totalPaidOut: Int,
        val extraDollars: Int,
        /** Sum of count * pct over all tiers (pct units, so 100% = 100). */
        val totalSharesPct: Int,
    ) : PoolOutcome

    data class Rejected(val reason: Rejection) : PoolOutcome
}

/** Biggest amount a person can type: $9,999,999.99 (seven dollar digits). Keeps every sum inside Int dollars. */
private const val MAX_DOLLAR_DIGITS = 7
private val MONEY = Regex("""(\d*)(?:\.(\d{0,2}))?""")

/** Parses what a person typed into whole cents, or null if it is not a valid money amount. Blank = 0. */
fun parseCents(text: String): Long? {
    val t = text.trim()
    if (t.isEmpty()) return 0L
    val m = MONEY.matchEntire(t) ?: return null
    val dollars = m.groupValues[1]
    val frac = m.groupValues[2]
    if (dollars.isEmpty() && frac.isEmpty()) return null // just "." (or nothing usable)
    if (dollars.length > MAX_DOLLAR_DIGITS) return null
    val d = if (dollars.isEmpty()) 0L else dollars.toLong()
    return d * 100 + frac.padEnd(2, '0').toLong()
}

/**
 * Splits the whole dollars of card + cash across tiers. All money arguments are in cents, and all
 * arithmetic is integer: payout = floor(pool * pct / sum(count * pct)). Leftover cents are only reported.
 */
fun calculatePool(cardCents: Long, cashCents: Long, counts: Map<Int, Int>): PoolOutcome {
    if (cardCents < 0 || cashCents < 0) return PoolOutcome.Rejected(Rejection.NEGATIVE_AMOUNT)

    val totalCents = cardCents + cashCents
    val poolDollars = totalCents / 100
    if (poolDollars < 1) return PoolOutcome.Rejected(Rejection.LESS_THAN_ONE_DOLLAR)

    val tiers = counts.filterValues { it > 0 }.toSortedMap(compareByDescending { it })
    if (tiers.isEmpty()) return PoolOutcome.Rejected(Rejection.NO_EMPLOYEES)

    val totalSharesPct = tiers.entries.sumOf { it.key.toLong() * it.value }
    val splits = tiers.map { (pct, count) ->
        val payout = (poolDollars * pct / totalSharesPct).toInt()
        TierSplit(pct = pct, count = count, payout = payout, subtotal = payout * count)
    }
    val paidOut = splits.sumOf { it.subtotal }

    return PoolOutcome.Split(
        poolDollars = poolDollars.toInt(),
        leftoverCents = (totalCents % 100).toInt(),
        tiers = splits,
        totalPaidOut = paidOut,
        extraDollars = poolDollars.toInt() - paidOut,
        totalSharesPct = totalSharesPct.toInt(),
    )
}

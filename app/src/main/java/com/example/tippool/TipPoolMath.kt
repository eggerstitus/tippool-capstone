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

/** Parses what a person typed into whole cents, or null if it is not a valid money amount. Blank = 0. */
fun parseCents(text: String): Long? = TODO("not implemented yet")

/** Splits the whole dollars of card + cash across tiers. All money arguments are in cents. */
fun calculatePool(cardCents: Long, cashCents: Long, counts: Map<Int, Int>): PoolOutcome =
    TODO("not implemented yet")

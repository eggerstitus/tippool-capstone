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

/** Digits, then optionally a "." or "," and at most two more digits. Nothing else (no sign, no thousands marks). */
private val MONEY = Regex("""(\d*)(?:[.,](\d{0,2}))?""")

/**
 * Parses what a person typed into whole cents, or says why not. Blank = $0. A single "," counts as the
 * decimal point (some keyboards only offer it), but "1,000" and "1,234.50" are refused, not guessed at.
 */
fun parseAmount(text: String): Amount {
    val t = text.trim()
    if (t.isEmpty()) return Amount.Cents(0L)
    val negative = t.startsWith("-")
    val m = MONEY.matchEntire(if (negative) t.drop(1) else t)
    if (m == null || (m.groupValues[1].isEmpty() && m.groupValues[2].isEmpty())) {
        return Amount.Problem(AmountProblem.NOT_A_NUMBER)
    }
    if (negative) return Amount.Problem(AmountProblem.NEGATIVE)
    val dollars = m.groupValues[1].trimStart('0')
    if (dollars.length > MAX_DOLLAR_DIGITS) return Amount.Problem(AmountProblem.TOO_LARGE)
    val d = if (dollars.isEmpty()) 0L else dollars.toLong()
    return Amount.Cents(d * 100 + m.groupValues[2].padEnd(2, '0').toLong())
}

/** The cents, or null if the text is not a usable amount. Use [parseAmount] when the reason matters. */
fun parseCents(text: String): Long? = (parseAmount(text) as? Amount.Cents)?.value

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

// ---- Words and formatting (kept here so they can be unit-tested) ---------------------------------

enum class AmountProblem { NOT_A_NUMBER, NEGATIVE, TOO_LARGE }

sealed interface Amount {
    data class Cents(val value: Long) : Amount
    data class Problem(val kind: AmountProblem) : Amount
}

/** Plain words for a refused amount typed into [field] (e.g. "Card tips"). */
fun amountMessage(field: String, problem: AmountProblem): String = when (problem) {
    AmountProblem.NOT_A_NUMBER -> "$field isn't a valid amount. Use numbers like 123.45."
    AmountProblem.NEGATIVE     -> "$field can't be negative."
    AmountProblem.TOO_LARGE    -> "$field is too large (the most is \$9,999,999.99)."
}

/** Plain words for a pool that could not be split. */
fun rejectionMessage(reason: Rejection): String = when (reason) {
    Rejection.NEGATIVE_AMOUNT      -> "Tips can't be negative."
    Rejection.LESS_THAN_ONE_DOLLAR -> "Total tip pool is under \$1."
    Rejection.NO_EMPLOYEES         -> "Add at least one employee."
}

/** Integer-only money text such as "$212.40", independent of the phone's language settings. */
fun formatCents(cents: Long): String = "$" + (cents / 100) + "." + (cents % 100).toString().padStart(2, '0')

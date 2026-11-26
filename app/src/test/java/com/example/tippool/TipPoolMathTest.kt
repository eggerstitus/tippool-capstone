package com.tippool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TipPoolMathTest {

    private fun counts(c100: Int = 0, c75: Int = 0, c50: Int = 0, c25: Int = 0) =
        mapOf(100 to c100, 75 to c75, 50 to c50, 25 to c25)

    private fun split(card: Long, cash: Long, counts: Map<Int, Int>): PoolOutcome.Split {
        val outcome = calculatePool(card, cash, counts)
        assertTrue("expected a split but got $outcome", outcome is PoolOutcome.Split)
        return outcome as PoolOutcome.Split
    }

    // ---- parseCents -------------------------------------------------------------------------

    @Test fun parseCents_wholeDollars() = assertEquals(1200L, parseCents("12"))
    @Test fun parseCents_twoDecimals() = assertEquals(1240L, parseCents("12.40"))
    @Test fun parseCents_oneDecimalMeansTens() = assertEquals(1240L, parseCents("12.4"))
    @Test fun parseCents_leadingPoint() = assertEquals(50L, parseCents(".5"))
    @Test fun parseCents_trailingPoint() = assertEquals(1200L, parseCents("12."))
    @Test fun parseCents_blankIsZero() {
        assertEquals(0L, parseCents(""))
        assertEquals(0L, parseCents("   "))
    }
    @Test fun parseCents_trimsSpaces() = assertEquals(750L, parseCents(" 7.50 "))

    @Test fun parseCents_rejectsNegative() = assertNull(parseCents("-5"))
    @Test fun parseCents_rejectsLetters() = assertNull(parseCents("abc"))
    @Test fun parseCents_rejectsNaNAndInfinity() {
        assertNull(parseCents("NaN"))
        assertNull(parseCents("Infinity"))
    }
    @Test fun parseCents_rejectsExponent() = assertNull(parseCents("1e3"))
    @Test fun parseCents_rejectsThousandsComma() = assertNull(parseCents("1,000"))
    @Test fun parseCents_rejectsThreeDecimals() = assertNull(parseCents("12.345"))
    @Test fun parseCents_rejectsTwoPoints() = assertNull(parseCents("1.2.3"))
    @Test fun parseCents_rejectsHugeAmount() = assertNull(parseCents("99999999999999999999"))

    // ---- calculatePool: the feature ---------------------------------------------------------

    @Test fun cents_areLeftOverNotAnError() {
        // card $100.50 + cash $111.90 = $212.40, seven people at 100%
        val r = split(10050, 11190, counts(c100 = 7))
        assertEquals(212, r.poolDollars)
        assertEquals(40, r.leftoverCents)
        assertEquals(30, r.tiers.single().payout) // floor(212 / 7)
        assertEquals(210, r.totalPaidOut)
        assertEquals(2, r.extraDollars)
    }

    @Test fun wholeDollarPool_hasNoLeftoverCents() {
        val r = split(30000, 40000, counts(c100 = 7))
        assertEquals(700, r.poolDollars)
        assertEquals(0, r.leftoverCents)
        assertEquals(100, r.tiers.single().payout)
        assertEquals(0, r.extraDollars)
    }

    @Test fun centsOnlyInCash_orOnlyInCard_giveSameResult() {
        val a = split(21200, 40, counts(c100 = 7))
        val b = split(40, 21200, counts(c100 = 7))
        assertEquals(a, b)
        assertEquals(40, a.leftoverCents)
    }

    @Test fun mixedTiers_floorEachTier_andReportExtraDollars() {
        // $500, 2 x 100%, 2 x 75%, 1 x 50%, 1 x 25%: shares = 425 (pct units)
        val r = split(50000, 0, counts(2, 2, 1, 1))
        assertEquals(425, r.totalSharesPct)
        assertEquals(listOf(117, 88, 58, 29), r.tiers.map { it.payout })
        assertEquals(listOf(234, 176, 58, 29), r.tiers.map { it.subtotal })
        assertEquals(497, r.totalPaidOut)
        assertEquals(3, r.extraDollars)
    }

    @Test fun tiersWithNoEmployees_areLeftOut_andOrderIsHighestFirst() {
        val r = split(10000, 0, counts(c100 = 1, c25 = 1))
        assertEquals(listOf(100, 25), r.tiers.map { it.pct })
    }

    @Test fun paidOutPlusExtra_alwaysEqualsPoolDollars() {
        val pools = listOf(100L, 101L, 9999L, 21240L, 123456L, 100000L)
        val shapes = listOf(counts(1), counts(7), counts(3, 2, 1, 1), counts(0, 0, 0, 5), counts(1, 1, 1, 1))
        for (p in pools) for (c in shapes) {
            val r = split(p, 0, c)
            assertEquals("pool $p shape $c", r.poolDollars, r.totalPaidOut + r.extraDollars)
            assertTrue("pool $p shape $c", r.extraDollars >= 0)
        }
    }

    // ---- calculatePool: unhappy paths -------------------------------------------------------

    @Test fun negativeCard_isRejected() =
        assertEquals(PoolOutcome.Rejected(Rejection.NEGATIVE_AMOUNT), calculatePool(-100, 5000, counts(c100 = 1)))

    @Test fun negativeCash_isRejected_evenIfTotalIsPositive() =
        assertEquals(PoolOutcome.Rejected(Rejection.NEGATIVE_AMOUNT), calculatePool(9000, -1, counts(c100 = 1)))

    @Test fun zeroPool_isRejected() =
        assertEquals(PoolOutcome.Rejected(Rejection.LESS_THAN_ONE_DOLLAR), calculatePool(0, 0, counts(c100 = 1)))

    @Test fun poolUnderOneDollar_isRejected() =
        assertEquals(PoolOutcome.Rejected(Rejection.LESS_THAN_ONE_DOLLAR), calculatePool(60, 30, counts(c100 = 1)))

    @Test fun noEmployees_isRejected() =
        assertEquals(PoolOutcome.Rejected(Rejection.NO_EMPLOYEES), calculatePool(10000, 0, counts()))

    @Test fun negativeEmployeeCount_isTreatedAsNobody() =
        assertEquals(PoolOutcome.Rejected(Rejection.NO_EMPLOYEES), calculatePool(10000, 0, counts(c100 = -3)))

    // ---- Review fixes: amounts with a reason ------------------------------------------------

    private fun problemOf(text: String): AmountProblem? = (parseAmount(text) as? Amount.Problem)?.kind
    private fun centsOf(text: String): Long? = (parseAmount(text) as? Amount.Cents)?.value

    @Test fun amount_commaIsADecimalPointWhenOneToTwoDigitsFollow() {
        assertEquals(10050L, centsOf("100,50"))
        assertEquals(150L, centsOf("1,5"))
        assertEquals(1200L, centsOf("12,"))
    }
    @Test fun amount_commaAsThousandsOrMixedIsNotANumber() {
        assertEquals(AmountProblem.NOT_A_NUMBER, problemOf("1,000"))
        assertEquals(AmountProblem.NOT_A_NUMBER, problemOf("1,234.50"))
        assertEquals(AmountProblem.NOT_A_NUMBER, problemOf("1.234,50"))
        assertEquals(AmountProblem.NOT_A_NUMBER, problemOf("1,2,3"))
    }
    @Test fun amount_negativeHasItsOwnReason() {
        assertEquals(AmountProblem.NEGATIVE, problemOf("-5"))
        assertEquals(AmountProblem.NEGATIVE, problemOf("-20.50"))
        assertEquals(AmountProblem.NOT_A_NUMBER, problemOf("-abc"))
    }
    @Test fun amount_tooLargeHasItsOwnReason() {
        assertEquals(AmountProblem.TOO_LARGE, problemOf("10000000"))
        assertEquals(AmountProblem.TOO_LARGE, problemOf("10000000.00"))
        assertEquals(AmountProblem.TOO_LARGE, problemOf("99999999999999999999"))
    }
    @Test fun amount_limitBoundary_isAccepted() = assertEquals(999999999L, centsOf("9999999.99"))
    @Test fun amount_leadingZerosDoNotCountTowardTheLimit() {
        assertEquals(100L, centsOf("00000001"))
        assertEquals(0L, centsOf("0000000000000"))
    }
    @Test fun amount_nonAsciiAndDecoration_isNotANumber() {
        for (bad in listOf("+5", "$5", "١٢", "１２", "5​", "5 0")) {
            assertEquals("input [$bad]", AmountProblem.NOT_A_NUMBER, problemOf(bad))
        }
        assertEquals(500L, centsOf(" 5 "))
    }
    @Test fun parseCents_agreesWithParseAmount() {
        assertEquals(10050L, parseCents("100,50"))
        assertNull(parseCents("-5"))
        assertNull(parseCents("10000000"))
    }

    // ---- Review fixes: words and formatting -------------------------------------------------

    @Test fun amountMessages_sayWhatIsWrong() {
        assertEquals("Card tips isn't a valid amount. Use numbers like 123.45.", amountMessage("Card tips", AmountProblem.NOT_A_NUMBER))
        assertEquals("Cash tips can't be negative.", amountMessage("Cash tips", AmountProblem.NEGATIVE))
        assertEquals("Card tips is too large (the most is $9,999,999.99).", amountMessage("Card tips", AmountProblem.TOO_LARGE))
    }
    @Test fun rejectionMessages_areTheOnesTheScreenShows() {
        assertEquals("Tips can't be negative.", rejectionMessage(Rejection.NEGATIVE_AMOUNT))
        assertEquals("Total tip pool is under $1.", rejectionMessage(Rejection.LESS_THAN_ONE_DOLLAR))
        assertEquals("Add at least one employee.", rejectionMessage(Rejection.NO_EMPLOYEES))
    }
    @Test fun formatCents_isIntegerOnly() {
        assertEquals("$0.40", formatCents(40))
        assertEquals("$0.05", formatCents(5))
        assertEquals("$1.00", formatCents(100))
        assertEquals("$212.40", formatCents(21240))
    }

    // ---- Review fixes: stronger guards on the split ------------------------------------------

    @Test fun centsFromBothFields_carryIntoADollar() {
        // 0.60 + 0.60 on top of 100.00 + 111.60: cents add up before splitting.
        val r = split(10060, 11160, counts(c100 = 7))
        assertEquals(212, r.poolDollars)
        assertEquals(20, r.leftoverCents)
    }
    @Test fun bothFieldsAtTheMaximum_stillSplitWithoutOverflow() {
        val r = split(999999999, 999999999, counts(c100 = 7))
        assertEquals(19999999, r.poolDollars)
        assertEquals(98, r.leftoverCents)
        assertEquals(r.poolDollars, r.totalPaidOut + r.extraDollars)
    }
    @Test fun everyPayout_isTheExactFloorOfItsFairShare() {
        val pools = listOf(100L, 21240L, 99999L, 123456L, 250000L)
        val shapes = listOf(counts(1), counts(7), counts(3, 2, 1, 1), counts(0, 0, 0, 5), counts(8, 8, 8, 8))
        for (p in pools) for (c in shapes) {
            val r = split(p, 0, c)
            for (t in r.tiers) {
                val exact = r.poolDollars.toLong() * t.pct
                assertTrue("pool $p $c pct ${t.pct}: payout too big", t.payout.toLong() * r.totalSharesPct <= exact)
                assertTrue("pool $p $c pct ${t.pct}: payout too small", (t.payout + 1L) * r.totalSharesPct > exact)
            }
        }
    }
    @Test fun payoutsMatchTheOldAppsKnownOutputs() {
        // Produced by the pre-change Double algorithm (docs/evidence/compare-old-vs-new.py).
        val pinned = listOf(
            Triple(212L, counts(7), listOf(30)),
            Triple(500L, counts(2, 2, 1, 1), listOf(117, 88, 58, 29)),
            Triple(1L, counts(1), listOf(1)),
            Triple(137L, counts(3, 1, 2), listOf(28, 21, 14)),
            Triple(999L, counts(5, 3, 2, 4), listOf(108, 81, 54, 27)),
            Triple(1500L, counts(8, 8, 8, 8), listOf(75, 56, 37, 18)),
            Triple(86L, counts(c25 = 4), listOf(21)),
            Triple(1234L, counts(1, 1, 1, 1), listOf(493, 370, 246, 123)),
        )
        for ((pool, c, expected) in pinned) {
            assertEquals("pool $pool $c", expected, split(pool * 100, 0, c).tiers.map { it.payout })
        }
    }
}

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
}

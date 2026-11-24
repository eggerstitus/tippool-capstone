package com.tippool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CateringLogicTest {

    @Test
    fun cookieTotals_includesDessertsBoxLunchesAndExtras() {
        val items = listOf(
            CateringItem.Dessert(DessertType.COOKIE_PLATTER, count = 1),
            CateringItem.BoxLunch(4, SandwichTrayType.CLASSIC, drinkChoice = DrinkChoice.PEPSI),
            CateringItem.Extra(ExtraType.COOKIES, 10, CookieFlavor.CHOCOLATE),
        )
        val totals = cookieTotalsForItems(items)
        assertEquals(12 + 2 + 10, totals.chocolate)
        assertEquals(12 + 2, totals.macadamia)
    }

    @Test
    fun sandwichBoxes_calculatedPerOrderNotGlobally() {
        val orderA = CateringOrder(items = listOf(CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.THIRD, 18)))
        val orderB = CateringOrder(items = listOf(CateringItem.Sandwiches(SandwichTrayType.SIGNATURE, SandwichPortionSize.THIRD, 18)))
        assertEquals(1, orderA.sandwichBoxesForOrder()[SandwichPortionSize.THIRD])
        assertEquals(1, orderB.sandwichBoxesForOrder()[SandwichPortionSize.THIRD])
        // 36 sandwiches globally would be 2 boxes if calculated globally; per-order stays 1+1
        assertEquals(2, orderA.sandwichBoxesForOrder()[SandwichPortionSize.THIRD]!! + orderB.sandwichBoxesForOrder()[SandwichPortionSize.THIRD]!!)
    }

    @Test
    fun dessertPlatterSupplyLabel_usesCountPrefix() {
        val dessert = CateringItem.Dessert(DessertType.BROWNIE_PLATTER, count = 2)
        assertEquals("2× Brownie platter (10 brownies per platter)", dessert.platterSupplyLabel())
    }

    @Test
    fun saladDressingSupplyLines_mergesCarusosFeeds15AndFeeds7() {
        val salads = listOf(
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, quantity = 1),
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_7, quantity = 2),
        )
        val merged = saladDressingSupplyLines(salads)
        assertEquals("2× Blue cheese dressing", merged.single { it.contains("Blue cheese") })
        assertEquals("2× Vinegar & Oil (VnO)", merged.single { it.contains("VnO") })
    }

    @Test
    fun saladDressingSupplyLines_mergesPierceAndGardenPoppyseed() {
        val salads = listOf(
            CateringItem.Salad(SaladType.PIERCE, SaladSize.FEEDS_15, quantity = 1),
            CateringItem.Salad(
                SaladType.GARDEN, SaladSize.FEEDS_7, quantity = 1,
                gardenDressings = setOf(GardenDressing.POPPYSEED),
            ),
        )
        val merged = saladDressingSupplyLines(salads)
        assertEquals("3× Poppyseed bottles", merged.single { it.contains("Poppyseed") })
    }

    @Test
    fun saladDressingSupplyLines_mergesGardenBlueCheeseCounts() {
        val salads = listOf(
            CateringItem.Salad(
                SaladType.GARDEN, SaladSize.FEEDS_15, quantity = 1,
                gardenDressings = setOf(GardenDressing.BLUE_CHEESE),
            ),
            CateringItem.Salad(
                SaladType.GARDEN, SaladSize.FEEDS_7, quantity = 1,
                gardenDressings = setOf(GardenDressing.BLUE_CHEESE),
            ),
        )
        val merged = saladDressingSupplyLines(salads)
        assertEquals("3× Blue Cheese bottles", merged.single { it.contains("Blue Cheese bottles") })
    }

    @Test
    fun sandwichCondimentSideLines_onePerSandwich() {
        val sandwiches = listOf(
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.THIRD, 30, condimentsOnSide = true),
        )
        assertEquals(listOf("30× Mayo & mustard on the side"), sandwichCondimentSideLines(sandwiches))
    }

    @Test
    fun sandwichCondimentSideLines_combinedAcrossPortions() {
        val sandwiches = listOf(
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.THIRD, 65, condimentsOnSide = true),
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.QUARTER, 40, condimentsOnSide = true),
        )
        assertEquals(listOf("105× Mayo & mustard on the side"), sandwichCondimentSideLines(sandwiches, combinePortions = true))
    }

    @Test
    fun totalBrowniesFromDesserts_countsPlatterBrowniesOnly() {
        val desserts = listOf(
            CateringItem.Dessert(DessertType.BROWNIE_COOKIE, count = 2),
            CateringItem.Dessert(DessertType.BROWNIE_PLATTER, count = 3),
            CateringItem.Dessert(DessertType.COOKIE_PLATTER, count = 1),
        )
        assertEquals(2 * 3 + 3 * 10, totalBrowniesFromDesserts(desserts))
    }

    @Test
    fun dessertPlatterSupplyLines_groupsByType() {
        val desserts = listOf(
            CateringItem.Dessert(DessertType.COOKIE_PLATTER, count = 1),
            CateringItem.Dessert(DessertType.COOKIE_PLATTER, count = 1),
            CateringItem.Dessert(DessertType.BROWNIE_PLATTER, count = 3),
        )
        val lines = dessertPlatterSupplyLines(desserts)
        assertEquals("2× Cookie platter to build (12 choc + 12 mac each)", lines.single { it.contains("Cookie platter") })
        assertEquals("3× Brownie platter to build (10 brownies each)", lines.single { it.contains("Brownie platter to build") })
    }

    @Test
    fun totalCateringBoxes_sumsPerOrder() {
        val order = CateringOrder(items = listOf(
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.THIRD, 18),
            CateringItem.Wraps(12),
        ))
        assertEquals(1 + 1, totalCateringBoxes(listOf(order))) // 1 sandwich box + 1 wrap box
    }

    @Test
    fun buildCondensedSupplyLines_omitsSaladNamesAndSoup() {
        val items = listOf(
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, quantity = 1),
            CateringItem.Extra(ExtraType.SOUP, 12),
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.THIRD, 10, condimentsOnSide = true),
        )
        val lines = buildCondensedSupplyLines(items, 0, 0, 0, ServingUtensilTally(), cateringBoxCount = 1)
        assertTrue(lines.none { it.contains("Caruso's House") })
        assertTrue(lines.none { it.contains("soup") })
        assertTrue(lines.any { it.contains("Blue cheese dressing") })
        assertTrue(lines.any { it.contains("Mayo & mustard") })
        assertEquals("1× catering box", lines.single { it.contains("catering box") })
    }

    @Test
    fun consolidateSupplyLines_alwaysPrefixesSinglePlainLine() {
        val merged = consolidateSupplyLines(listOf("Feeds 15 Caruso's House"))
        assertEquals("1× Feeds 15 Caruso's House", merged.single())
    }

    @Test
    fun tallyServingUtensils_skipsSaladTongsWhenForksToggleOff() {
        val order = CateringOrder(
            needsServingUtensils = false,
            items = listOf(
                CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, quantity = 1),
                CateringItem.Salad(SaladType.CAESAR, SaladSize.FEEDS_7, quantity = 2),
            ),
        )
        assertEquals(ServingUtensilTally(), order.tallyServingUtensils())
    }

    @Test
    fun tallyServingUtensils_countsSaladTongsWhenForksToggleOn() {
        val order = CateringOrder(
            needsServingUtensils = true,
            items = listOf(
                CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, quantity = 1),
                CateringItem.Salad(SaladType.CAESAR, SaladSize.FEEDS_7, quantity = 2),
            ),
        )
        assertEquals(3, order.tallyServingUtensils().largeTongs)
    }

    @Test
    fun tallyServingUtensils_alwaysCountsExplicitExtraTongs() {
        val order = CateringOrder(
            needsServingUtensils = false,
            items = listOf(
                CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15),
                CateringItem.Extra(ExtraType.LARGE_TONG, 2),
            ),
        )
        assertEquals(2, order.tallyServingUtensils().largeTongs)
    }

    @Test
    fun supplyNoteLinesForOrder_includesOnlySupplyListNotes() {
        val order = CateringOrder(
            customerName = "Acme",
            note = "Call on arrival",
            noteInHandoff = true,
            noteInSupplyList = true,
            items = listOf(
                CateringItem.Wraps(5, note = "No onions", noteInHandoff = false, noteInSupplyList = true),
                CateringItem.Extra(ExtraType.SOUP, 2, note = "Kitchen only", noteInHandoff = true, noteInSupplyList = false),
            ),
        )
        val lines = supplyNoteLinesForOrder(order, prefixCustomer = true)
        assertEquals(2, lines.size)
        assertTrue(lines.any { it == "Note — Acme: Call on arrival" })
        assertTrue(lines.any { it.contains("5 Wraps") && it.contains("No onions") })
        assertTrue(lines.none { it.contains("Kitchen only") })
    }

    @Test
    fun totalLettuceScoops_countsWrapsSaladsAndWrapBoxLunches() {
        val items = listOf(
            CateringItem.Wraps(10),
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_7, quantity = 2),
            CateringItem.Salad(SaladType.PIERCE, SaladSize.FEEDS_15, quantity = 1),
            CateringItem.BoxLunch(4, SandwichTrayType.CLASSIC, isWrap = true, drinkChoice = DrinkChoice.WATER),
            CateringItem.BoxLunch(2, SandwichTrayType.CLASSIC, isWrap = false, drinkChoice = DrinkChoice.PEPSI),
        )
        assertEquals(10 + 8 + 8 + 4, totalLettuceScoops(items))
    }

    @Test
    fun lettuceHandoffLine_hiddenWhenRoundedBelowHalfBox() {
        assertEquals(null, lettuceHandoffLine(listOf(CateringItem.Wraps(40))))
        assertEquals(null, lettuceHandoffLine(listOf(
            CateringItem.Wraps(36),
            CateringItem.Salad(SaladType.GARDEN, SaladSize.FEEDS_7, quantity = 1),
        )))
    }

    @Test
    fun lettuceHandoffLine_roundsToNearestQuarterBox() {
        assertEquals(
            "We will need around half a box of salad lettuce cut for catering",
            lettuceHandoffLine(listOf(CateringItem.Wraps(46))),
        )
        assertEquals(
            "We will need around half a box of salad lettuce cut for catering",
            lettuceHandoffLine(listOf(CateringItem.Wraps(56))),
        )
        assertEquals(
            "We will need around a full box of salad lettuce cut for catering",
            lettuceHandoffLine(listOf(CateringItem.Wraps(112))),
        )
        assertEquals(
            "We will need around a full box of salad lettuce cut for catering",
            lettuceHandoffLine(listOf(CateringItem.Wraps(113))),
        )
        assertEquals(
            "We will use around 1¼ boxes of salad lettuce for catering",
            lettuceHandoffLine(listOf(CateringItem.Wraps(126))),
        )
    }

    @Test
    fun buildHandoffSections_groupsSandwichBoxLunchesAndOmitsSupplyDetails() {
        val items = listOf(
            CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.QUARTER, 36),
            CateringItem.Sandwiches(SandwichTrayType.SIGNATURE, SandwichPortionSize.THIRD, 24, condimentsOnSide = true),
            CateringItem.BoxLunch(53, SandwichTrayType.SIGNATURE, drinkChoice = DrinkChoice.PEPSI),
            CateringItem.BoxLunch(50, SandwichTrayType.CLASSIC, drinkChoice = DrinkChoice.PEPSI),
            CateringItem.BoxLunch(50, SandwichTrayType.SIGNATURE, drinkChoice = DrinkChoice.SPLIT),
            CateringItem.Dessert(DessertType.COOKIE_PLATTER, 3),
            CateringItem.Extra(ExtraType.COOKIES, 10, CookieFlavor.CHOCOLATE),
            CateringItem.Extra(ExtraType.PEPSI_CANS, 70),
        )
        val sections = buildHandoffSections(items)
        assertEquals(listOf("36x 1/4 (3\") sandwiches", "24x 1/3 (4\") sandwiches", "153x Sandwich boxed lunches"), sections.sandwiches)
        assertTrue(sections.salads.isEmpty())
        assertTrue(sections.pastas.isEmpty())
        assertTrue(sections.other.isEmpty())
    }

    @Test
    fun buildHandoffSections_saladsSectionConsolidatesWrapsAndGardenDressings() {
        val items = listOf(
            CateringItem.Wraps(11),
            CateringItem.BoxLunch(51, SandwichTrayType.CLASSIC, isWrap = true, drinkChoice = DrinkChoice.WATER),
            CateringItem.BoxLunch(50, SandwichTrayType.SIGNATURE, isWrap = true, drinkChoice = DrinkChoice.SPLIT),
            CateringItem.Salad(SaladType.CAESAR, SaladSize.FEEDS_15),
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, addChicken = true),
            CateringItem.Salad(SaladType.CARUSOS, SaladSize.FEEDS_15, quantity = 8),
            CateringItem.Salad(SaladType.GARDEN, SaladSize.FEEDS_15, gardenDressings = setOf(GardenDressing.THOUSAND_ISLAND)),
            CateringItem.Salad(SaladType.GARDEN, SaladSize.FEEDS_15, gardenDressings = setOf(GardenDressing.RANCH, GardenDressing.THOUSAND_ISLAND)),
            CateringItem.Salad(SaladType.PIERCE, SaladSize.FEEDS_15, addChicken = true, quantity = 2),
        )
        val sections = buildHandoffSections(items)
        assertEquals("11x wraps", sections.salads[0])
        assertEquals("101x wrap boxed lunches", sections.salads[1])
        assertTrue(sections.salads.any { it == "1x Feeds 15 Caesar" })
        assertTrue(sections.salads.any { it == "1x Feeds 15 Caruso's House w/ chicken" })
        assertTrue(sections.salads.any { it == "8x Feeds 15 Caruso's House" })
        assertTrue(sections.salads.any { it == "2x Feeds 15 Garden" })
        assertTrue(sections.salads.any { it == "2x Feeds 15 Pierce w/ chicken" })
        assertTrue(sections.salads.none { it.contains("Ranch") })
    }

    @Test
    fun buildHandoffSections_pastaOnlyHighlightsChickenAddOn() {
        val items = listOf(
            CateringItem.PastaTray(PastaType.BYO, PastaSize.FEEDS_15, byoNoodle = BYONoodle.MACARONI, byoSauce = BYOSauce.MARINARA, addOns = listOf("Chicken", "Bacon", "Italian Sausage")),
            CateringItem.PastaTray(PastaType.BYO, PastaSize.FEEDS_15, byoNoodle = BYONoodle.PENNE, byoSauce = BYOSauce.MARINARA),
            CateringItem.PastaTray(PastaType.BYO, PastaSize.FEEDS_15, byoNoodle = BYONoodle.PENNE, byoSauce = BYOSauce.MARINARA, addOns = listOf("Italian Sausage", "Bacon")),
            CateringItem.PastaTray(PastaType.BYO, PastaSize.FEEDS_15, byoNoodle = BYONoodle.PENNE, byoSauce = BYOSauce.MARINARA, addOns = listOf("Italian Sausage", "Bacon", "Chicken")),
        )
        val sections = buildHandoffSections(items)
        assertTrue(sections.pastas.any { it == "1x Feeds 15 BYO Pasta (Macaroni / Marinara) w/ Chicken" })
        assertTrue(sections.pastas.any { it == "2x Feeds 15 BYO Pasta (Penne / Marinara)" })
        assertTrue(sections.pastas.any { it == "1x Feeds 15 BYO Pasta (Penne / Marinara) w/ Chicken" })
        assertTrue(sections.pastas.none { it.contains("Bacon") })
    }

    @Test
    fun buildHandoffSections_otherIncludesPastaSaladsAndOtherExtrasOnly() {
        val items = listOf(
            CateringItem.Pizza(100),
            CateringItem.Extra(ExtraType.SOUP, 60),
            CateringItem.Salad(SaladType.CBR_PASTA, SaladSize.FEEDS_15, quantity = 2),
            CateringItem.Salad(SaladType.SPRING_PEA, SaladSize.FEEDS_15),
            CateringItem.Extra(ExtraType.OTHER, 4, otherNote = "beans"),
            CateringItem.Extra(ExtraType.WATER_BOTTLES, 10),
        )
        val sections = buildHandoffSections(items)
        assertEquals(listOf("100x pizzas", "60x cups of soup"), sections.other.take(2))
        assertTrue(sections.other.any { it == "2x Feeds 15 CBR Pasta Salad" })
        assertTrue(sections.other.any { it == "1x Feeds 15 Spring Pea Salad" })
        assertTrue(sections.other.any { it == "4x Other (beans)" })
    }

    @Test
    fun formatHandoffMessage_usesSectionHeadersAndHeadsUp() {
        val orders = listOf(
            CateringOrder(
                customerName = "Max Daltin",
                items = listOf(
                    CateringItem.Sandwiches(SandwichTrayType.CLASSIC, SandwichPortionSize.QUARTER, 36),
                    CateringItem.Salad(SaladType.GARDEN, SaladSize.FEEDS_15, gardenDressings = setOf(GardenDressing.THOUSAND_ISLAND), note = "Dressing unknown unless Max calls us back", noteInHandoff = true),
                ),
            ),
            CateringOrder(
                customerName = "Everything",
                items = listOf(
                    CateringItem.Extra(ExtraType.OTHER, 4, otherNote = "beans", note = "note", noteInHandoff = true),
                ),
            ),
        )
        val text = formatHandoffMessage(orders)
        assertTrue(text.contains("SANDWICHES"))
        assertTrue(text.contains("SALADS"))
        assertTrue(text.contains("OTHER"))
        assertTrue(text.contains("HEADS UP"))
        assertTrue(text.contains("Notes:"))
        assertTrue(text.contains("Max Daltin"))
        assertFalse(text.contains("Mayo & mustard"))
        assertFalse(text.contains("Cookie Platter"))
    }

    @Test
    fun buildCateringPrintReport_includesOverviewSupplyItemsAndChecklists() {
        val orders = listOf(
            CateringOrder(
                customerName = "Acme",
                time = "11:00 AM",
                items = listOf(
                    CateringItem.Wraps(5),
                    CateringItem.Salad(SaladType.CAESAR, SaladSize.FEEDS_15),
                ),
                note = "Ring doorbell",
            ),
        )
        val report = buildCateringPrintReport(orders)
        assertTrue(report.orderSummaries.any { it.contains("Acme") })
        assertTrue(report.allSupplyLines.isNotEmpty())
        assertTrue(report.allItemSections.any { it.header == "SALADS" })
        assertEquals(1, report.orderChecklists.size)
        assertTrue(report.orderChecklists[0].itemLines.any { it.contains("Wraps") })
        assertTrue(report.orderChecklists[0].noteLines.any { it.contains("Ring doorbell") })
    }
}
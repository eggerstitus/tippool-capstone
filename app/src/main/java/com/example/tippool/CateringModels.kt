package com.tippool

import org.json.JSONArray
import org.json.JSONObject

enum class OrderType { PICKUP, DELIVERY }

enum class SaladType(
    val label: String,
    val isPasta: Boolean,
    val useSpoon: Boolean = false,
    /** Custom picked-dressing salads (Garden, Cobb, Chef) share the same dressing-selection flow. */
    val usesCustomDressing: Boolean = false,
    /** Salads with a fixed default dressing that can be switched to custom picking via a toggle. */
    val hasDefaultDressing: Boolean = false,
) {
    CARUSOS("Caruso's House", false, hasDefaultDressing = true),
    PIERCE("Pierce", false, hasDefaultDressing = true),
    CAESAR("Caesar", false, hasDefaultDressing = true),
    GARDEN("Garden", false, usesCustomDressing = true),
    COBB("Cobb", false, usesCustomDressing = true),
    CHEF("Chef", false, usesCustomDressing = true),
    ITALIAN_PASTA("Italian Pasta Salad", true),
    CBR_PASTA("CBR Pasta Salad", true),
    SPRING_PEA("Spring Pea Salad", false, useSpoon = true),
}

enum class SaladSize(val label: String) { FEEDS_7("Feeds 7"), FEEDS_15("Feeds 15") }

enum class GardenDressing(val label: String) {
    RANCH("Ranch"), BLUE_CHEESE("Blue Cheese"), POPPYSEED("PoppySeed"),
    CAESAR("Caesar"), ITALIAN("Italian"), THOUSAND_ISLAND("1000 Island"),
    VNO("VnO"), UNKNOWN("Unknown"),
}

enum class BYONoodle(val label: String, val usesTong: Boolean) {
    PENNE("Penne", false), MACARONI("Macaroni", false), SPAGHETTI("Spaghetti", true)
}

enum class BYOSauce(val label: String) { MARINARA("Marinara"), ALFREDO("Alfredo") }

enum class PastaType(val label: String, val usesTong: Boolean) {
    BYO("BYO Pasta", false),
    MAC_CHEESE("Mac & Cheese", false),
    SPAGHETTI_MEAT("Spaghetti & Meatballs", true),
    POLLO("Pollo Pasta", false),
    CHICKEN_PARM("Chicken Parmesana", false),
}

enum class PastaSize(val label: String) { FEEDS_7("Feeds 7"), FEEDS_15("Feeds 15") }

enum class DrinkChoice { PEPSI, WATER, SPLIT }

enum class SandwichTrayType(val label: String) { CLASSIC("Classic"), SIGNATURE("Signature") }

enum class SandwichPortionSize(val label: String, val perBox: Int) {
    QUARTER("1/4 (3\")", 24), THIRD("1/3 (4\")", 18)
}

enum class DessertType(val label: String) {
    COOKIE_PLATTER("Cookie Platter"),
    BROWNIE_COOKIE("Brownie & Cookie Platter"),
    BROWNIE_PLATTER("Brownie Platter"),
}

enum class ExtraType(val label: String) {
    WATER_BOTTLES("Water Bottles"),
    PEPSI_CANS("Pepsi Cans"),
    COOKIES("Cookies"),
    CHIPS("Chips"),
    LARGE_TONG("Large Tongs"),
    SMALL_TONG("Small Tongs"),
    SPOON("Spoons"),
    DRESSING_BOTTLE("Dressing Bottle"),
    SOUP("Soup"),
    OTHER("Other"),
}

// Cookie flavor for extras
enum class CookieFlavor(val label: String) {
    CHOCOLATE("Chocolate"), MACADAMIA("Macadamia"), FIFTY_FIFTY("50/50")
}

enum class IndividualItemType(
    val label: String,
    val lettuceScoops: Int,
    /** Loaf fraction — sandwich pieces out of 4 */
    val sandwichFourths: Int,
) {
    LARGE_SALAD("Large Salad", lettuceScoops = 0, sandwichFourths = 0),
    SMALL_SALAD("Small Salad", lettuceScoops = 0, sandwichFourths = 0),
    HALF_SANDWICH("Half Sandwich", lettuceScoops = 0, sandwichFourths = 0),
    FULL_SANDWICH("Full Sandwich", lettuceScoops = 0, sandwichFourths = 0),
}

// ─── Sealed item class ────────────────────────────────────────────────────────

sealed class CateringItem {
    abstract val note: String
    abstract val noteInHandoff: Boolean
    abstract val noteInSupplyList: Boolean
    abstract fun displayName(): String
    abstract fun supplyLines(): List<String>
    abstract fun toJson(): JSONObject

    data class Sandwiches(
        val type: SandwichTrayType,
        val portionSize: SandwichPortionSize,
        val sandwichCount: Int,
        val condimentsOnSide: Boolean = false,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        // Box count is used ONLY per-order-total; individual item doesn't show boxes anymore
        val loafEquivalent get() = when (portionSize) {
            SandwichPortionSize.QUARTER -> sandwichCount / 4.0
            SandwichPortionSize.THIRD   -> sandwichCount / 3.0
        }
        override fun displayName() = "${type.label} Sandwiches · ${portionSize.label} · $sandwichCount"
        override fun supplyLines() = buildList {
            add("$sandwichCount ${portionSize.label} ${type.label.lowercase()} sandwich${if (sandwichCount != 1) "es" else ""}")
            if (condimentsOnSide) add(formatSupplyQty(sandwichCount, "Mayo & mustard on the side"))
        }
        override fun toJson() = JSONObject().apply {
            put("type_key", "sandwiches"); put("tray_type", type.name)
            put("portion", portionSize.name); put("count", sandwichCount)
            put("condiments", condimentsOnSide); put("note", note); put("note_handoff", noteInHandoff)
            put("note_supply", noteInSupplyList)
        }
    }

    data class Wraps(
        val count: Int,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        // Box count is computed per-order-total
        override fun displayName() = "$count Wrap${if (count != 1) "s" else ""}"
        override fun supplyLines() = listOf("$count wrap${if (count != 1) "s" else ""}")
        override fun toJson() = JSONObject().apply {
            put("type_key", "wraps"); put("count", count); put("note", note); put("note_handoff", noteInHandoff)
            put("note_supply", noteInSupplyList)
        }
    }

    data class Salad(
        val saladType: SaladType,
        val saladSize: SaladSize,
        val quantity: Int = 1,
        val addChicken: Boolean = false,
        val gardenDressings: Set<GardenDressing> = emptySet(),
        val croutonsOnSide: Boolean = false,
        /** For types with a fixed default dressing (Pierce/Caesar/Caruso's) — pick custom dressings instead. */
        val customDressing: Boolean = false,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        val dressingBottlesPerSalad get() = if (saladSize == SaladSize.FEEDS_15) 2 else 1
        /** True if this salad should use the picked-dressing flow, whether by type or by the toggle. */
        val usesCustomDressingNow get() = saladType.usesCustomDressing || (saladType.hasDefaultDressing && customDressing)
        val lettuceScoops get() = quantity * when (saladSize) {
            SaladSize.FEEDS_7 -> 4
            SaladSize.FEEDS_15 -> 8
        }
        override fun displayName(): String {
            val chicken  = if (addChicken) " + Chicken" else ""
            val dressing = if (usesCustomDressingNow && gardenDressings.isNotEmpty())
                " (${gardenDressings.joinToString(" + ") { it.label }})" else ""
            val croutons = if (croutonsOnSide) " + croutons on side" else ""
            val qty = if (quantity > 1) "${quantity}x " else ""
            return "$qty${saladType.label}$chicken$dressing$croutons · ${saladSize.label}"
        }
        override fun supplyLines(): List<String> = buildList {
            repeat(quantity) {
                add(saladSupplyHeader())
                addAll(dressingLinesPerUnit())
            }
            if (croutonsOnSide) {
                val feeds15Count = if (saladSize == SaladSize.FEEDS_15) quantity else 0
                val feeds7Count  = if (saladSize == SaladSize.FEEDS_7)  quantity else 0
                val parts = buildList {
                    if (feeds15Count > 0) add("${feeds15Count}x feeds 15")
                    if (feeds7Count  > 0) add("${feeds7Count}x feeds 7")
                }
                add("Side croutons needed for ${parts.joinToString(" and ")}")
            }
        }
        override fun toJson() = JSONObject().apply {
            put("type_key", "salad"); put("salad_type", saladType.name); put("size_salad", saladSize.name)
            put("quantity", quantity); put("add_chicken", addChicken)
            put("garden_dressings", JSONArray(gardenDressings.map { it.name }))
            put("croutons_on_side", croutonsOnSide)
            put("custom_dressing", customDressing)
            put("note", note); put("note_handoff", noteInHandoff); put("note_supply", noteInSupplyList)
        }
    }

    data class PastaTray(
        val pastaType: PastaType,
        val pastaSize: PastaSize,
        val quantity: Int = 1,
        val byoNoodle: BYONoodle? = null,
        val byoSauce: BYOSauce? = null,
        val addOns: List<String> = emptyList(),
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        val effectivelyUsesTong get() = if (pastaType == PastaType.BYO) byoNoodle == BYONoodle.SPAGHETTI else pastaType.usesTong
        val friendlyName get() = if (pastaType == PastaType.BYO && byoNoodle != null && byoSauce != null)
            "BYO Pasta (${byoNoodle.label} / ${byoSauce.label})" else pastaType.label
        /** Raw breadstick count for this tray (Feeds 7 → 7, Feeds 15 → 15), times quantity. */
        val rawBreadsticks get() = when (pastaSize) {
            PastaSize.FEEDS_7  -> 7 * quantity
            PastaSize.FEEDS_15 -> 15 * quantity
        }
        override fun displayName(): String {
            val addOnStr = if (addOns.isNotEmpty()) " + ${addOns.joinToString()}" else ""
            val qty = if (quantity > 1) "${quantity}x " else ""
            return "$qty$friendlyName$addOnStr · ${pastaSize.label}"
        }
        override fun supplyLines() = (1..quantity).map {
            val utensil = if (effectivelyUsesTong) "large tong" else "spoon"
            val addOnStr = if (addOns.isNotEmpty()) " + ${addOns.joinToString()}" else ""
            "$friendlyName$addOnStr · ${pastaSize.label} + 1 $utensil"
        }
        override fun toJson() = JSONObject().apply {
            put("type_key", "pasta"); put("pasta_type", pastaType.name); put("size_pasta", pastaSize.name)
            put("quantity", quantity); put("byo_noodle", byoNoodle?.name ?: "")
            put("byo_sauce", byoSauce?.name ?: ""); put("addons", JSONArray(addOns))
            put("note", note); put("note_handoff", noteInHandoff); put("note_supply", noteInSupplyList)
        }
    }

    data class BoxLunch(
        val count: Int,
        val sandwichType: SandwichTrayType,
        val isWrap: Boolean = false,
        val drinkChoice: DrinkChoice,
        val pepsiCount: Int = 0,
        val waterCount: Int = 0,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        val loafEquivalent get() = count * (2.0 / 4.0)
        override fun displayName(): String {
            val itemType = if (isWrap) "Wrap" else "${sandwichType.label} Sandwich"
            val drinks = when (drinkChoice) {
                DrinkChoice.PEPSI -> "$count Pepsi"; DrinkChoice.WATER -> "$count water"
                DrinkChoice.SPLIT -> "$pepsiCount Pepsi / $waterCount water"
            }
            return "$count $itemType Box Lunch${if (count != 1) "es" else ""} · $drinks"
        }
        override fun supplyLines() = buildList {
            val choc = count / 2 + count % 2; val mac = count / 2
            val itemType = if (isWrap) "wrap" else "${sandwichType.label.lowercase()} sandwich"
            add("$count $itemType box lunch${if (count != 1) "es" else ""}")
            add("$count bags of chips")
            add("$choc chocolate + $mac macadamia catering cookie${if (count != 1) "s" else ""}")
            when (drinkChoice) {
                DrinkChoice.PEPSI -> add("$count Pepsi cans")
                DrinkChoice.WATER -> add("$count water bottles")
                DrinkChoice.SPLIT -> { if (pepsiCount > 0) add("$pepsiCount Pepsi cans"); if (waterCount > 0) add("$waterCount water bottles") }
            }
        }
        override fun toJson() = JSONObject().apply {
            put("type_key", "box_lunch"); put("count", count); put("sandwich_type", sandwichType.name)
            put("is_wrap", isWrap); put("drink", drinkChoice.name)
            put("pepsi_count", pepsiCount); put("water_count", waterCount)
            put("note", note); put("note_handoff", noteInHandoff); put("note_supply", noteInSupplyList)
        }
    }

    data class Pizza(
        val count: Int,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        override fun displayName() = "$count Pizza${if (count != 1) "s" else ""}"
        override fun supplyLines() = listOf("$count pizza${if (count != 1) "s" else ""}")
        override fun toJson() = JSONObject().apply {
            put("type_key", "pizza"); put("count", count); put("note", note)
            put("note_handoff", noteInHandoff); put("note_supply", noteInSupplyList)
        }
    }

    data class Dessert(
        val dessertType: DessertType,
        val count: Int = 1,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        val chocolateCookies get() = when (dessertType) { DessertType.COOKIE_PLATTER -> count * 12; DessertType.BROWNIE_COOKIE -> count * 7; else -> 0 }
        val macadamiaCookies get() = when (dessertType) { DessertType.COOKIE_PLATTER -> count * 12; DessertType.BROWNIE_COOKIE -> count * 7; else -> 0 }
        override fun displayName() = "$count × ${dessertType.label}"
        /** Platter line for supply lists (cookie bake totals and tongs are shown separately). */
        fun platterSupplyLabel(): String = when (dessertType) {
            DessertType.COOKIE_PLATTER  -> "${count}× Cookie platter (12 choc + 12 mac per platter)"
            DessertType.BROWNIE_COOKIE  -> "${count}× Brownie & Cookie platter (3 brownies + 7 choc + 7 mac per platter)"
            DessertType.BROWNIE_PLATTER -> "${count}× Brownie platter (10 brownies per platter)"
        }
        override fun supplyLines() = listOf(platterSupplyLabel())
        override fun toJson() = JSONObject().apply {
            put("type_key", "dessert"); put("dessert_type", dessertType.name)
            put("count", count); put("note", note); put("note_handoff", noteInHandoff)
            put("note_supply", noteInSupplyList)
        }
    }

    data class Extra(
        val extraType: ExtraType,
        val count: Int,
        val cookieFlavor: CookieFlavor? = null,   // only for COOKIES type
        val dressingChoice: GardenDressing? = null, // only for DRESSING_BOTTLE type
        val otherNote: String = "",                 // for OTHER / specifics
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        override fun displayName(): String {
            if (extraType == ExtraType.SOUP) return "Soup · $count cup${if (count != 1) "s" else ""}"
            val detail = when {
                extraType == ExtraType.COOKIES && cookieFlavor != null -> " (${cookieFlavor.label})"
                extraType == ExtraType.DRESSING_BOTTLE && dressingChoice != null -> " (${dressingChoice.label})"
                otherNote.isNotBlank() -> " ($otherNote)"
                else -> ""
            }
            return "$count × ${extraType.label}$detail"
        }
        override fun supplyLines(): List<String> {
            if (extraType == ExtraType.SOUP) return listOf("$count cup${if (count != 1) "s" else ""} of soup + $count breadstick${if (count != 1) "s" else ""}")
            val detail = when {
                extraType == ExtraType.COOKIES && cookieFlavor != null -> " (${cookieFlavor.label})"
                extraType == ExtraType.DRESSING_BOTTLE && dressingChoice != null -> " (${dressingChoice.label})"
                otherNote.isNotBlank() -> " ($otherNote)"
                else -> ""
            }
            return listOf("$count × ${extraType.label}$detail")
        }
        override fun toJson() = JSONObject().apply {
            put("type_key", "extra"); put("extra_type", extraType.name); put("count", count)
            put("cookie_flavor", cookieFlavor?.name ?: ""); put("dressing_choice", dressingChoice?.name ?: "")
            put("other_note", otherNote); put("note", note); put("note_handoff", noteInHandoff)
            put("note_supply", noteInSupplyList)
        }
    }

    data class IndividualItems(
        val itemType: IndividualItemType,
        val count: Int,
        override val note: String = "",
        override val noteInHandoff: Boolean = true,
        override val noteInSupplyList: Boolean = false,
    ) : CateringItem() {
        val lettuceScoops get() = itemType.lettuceScoops * count
        val loafEquivalent get() = itemType.sandwichFourths * count / 4.0
        override fun displayName() = "$count × ${itemType.label}"
        override fun supplyLines() = buildList<String> {
            when (itemType) {
                IndividualItemType.LARGE_SALAD   -> add("$count large salad${if (count != 1) "s" else ""}")
                IndividualItemType.SMALL_SALAD   -> add("$count small salad${if (count != 1) "s" else ""}")
                IndividualItemType.HALF_SANDWICH -> add("$count half sandwich${if (count != 1) "es" else ""}")
                IndividualItemType.FULL_SANDWICH -> add("$count full sandwich${if (count != 1) "es" else ""}")
            }
        }

        override fun toJson() = JSONObject().apply {
            put("type_key", "individual_item")
            put("individual_type", itemType.name)
            put("count", count)
            put("note", note)
            put("note_handoff", noteInHandoff)
            put("note_supply", noteInSupplyList)
        }
    }

    companion object
}

data class CookieTotals(val chocolate: Int, val macadamia: Int) {
    val total get() = chocolate + macadamia
    companion object {
        val ZERO = CookieTotals(0, 0)
    }
}

/** Standard quantity prefix for supply list lines (always includes count, even for 1). */
fun formatSupplyQty(count: Int, description: String): String = "${count}× $description"

/**
 * Plates and napkins are independent toggles now. When an order (or the aggregated total)
 * needs the same count of both, combine into one "Plates & Napkins" line; otherwise show
 * whichever is needed on its own line(s).
 */
fun plateNapkinSupplyLines(plateCount: Int, napkinCount: Int): List<String> = buildList {
    when {
        plateCount > 0 && plateCount == napkinCount -> add(formatSupplyQty(plateCount, "Plates & Napkins"))
        plateCount > 0 && napkinCount > 0 -> {
            add(formatSupplyQty(plateCount, "Plates"))
            add(formatSupplyQty(napkinCount, "Napkins"))
        }
        plateCount > 0  -> add(formatSupplyQty(plateCount, "Plates"))
        napkinCount > 0 -> add(formatSupplyQty(napkinCount, "Napkins"))
    }
}

fun totalCondimentSideCount(sandwiches: List<CateringItem.Sandwiches>): Int =
    sandwiches.filter { it.condimentsOnSide }.sumOf { it.sandwichCount }

/** One mayo/mustard portion per sandwich; per-order view can split by portion when both sizes exist. */
fun sandwichCondimentSideLines(sandwiches: List<CateringItem.Sandwiches>, combinePortions: Boolean = false): List<String> {
    if (combinePortions) {
        val total = totalCondimentSideCount(sandwiches)
        return if (total > 0) listOf(formatSupplyQty(total, "Mayo & mustard on the side")) else emptyList()
    }
    val byPortion = sandwiches
        .filter { it.condimentsOnSide }
        .groupBy { it.portionSize }
        .mapValues { (_, items) -> items.sumOf { it.sandwichCount } }
    if (byPortion.isEmpty()) return emptyList()
    val labelPortion = byPortion.size > 1
    return byPortion.map { (portion, count) ->
        val suffix = if (labelPortion) " (${portion.label})" else ""
        formatSupplyQty(count, "Mayo & mustard on the side$suffix")
    }
}

fun totalBrowniesFromDesserts(desserts: List<CateringItem.Dessert>): Int =
    desserts.sumOf { d ->
        when (d.dessertType) {
            DessertType.BROWNIE_COOKIE -> d.count * 3
            DessertType.BROWNIE_PLATTER -> d.count * 10
            else -> 0
        }
    }

fun totalCateringBoxes(orders: List<CateringOrder>): Int =
    orders.sumOf { order -> orderCateringBoxCount(order) }

fun orderCateringBoxCount(order: CateringOrder): Int =
    order.sandwichBoxesForOrder().values.sum() + order.wrapBoxesForOrder()

fun utensilSupplyLines(tally: ServingUtensilTally): List<String> = buildList {
    if (tally.largeTongs > 0) add(formatSupplyQty(tally.largeTongs, "large tong${if (tally.largeTongs != 1) "s" else ""}"))
    if (tally.smallTongs > 0) add(formatSupplyQty(tally.smallTongs, "small tong${if (tally.smallTongs != 1) "s" else ""}"))
    if (tally.spoons > 0) add(formatSupplyQty(tally.spoons, "spoon${if (tally.spoons != 1) "s" else ""}"))
}

fun boxLunchSupplyLines(boxLunches: List<CateringItem.BoxLunch>): List<String> {
    if (boxLunches.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    val total = boxLunches.sumOf { it.count }
    lines.add(formatSupplyQty(total, "boxed lunch${if (total != 1) "es" else ""}"))
    lines.add("  → $total bags of chips")
    val pepsi = boxLunches.sumOf { when (it.drinkChoice) { DrinkChoice.PEPSI -> it.count; DrinkChoice.SPLIT -> it.pepsiCount; else -> 0 } }
    val water = boxLunches.sumOf { when (it.drinkChoice) { DrinkChoice.WATER -> it.count; DrinkChoice.SPLIT -> it.waterCount; else -> 0 } }
    if (pepsi > 0) lines.add("  → $pepsi Pepsi can${if (pepsi != 1) "s" else ""}")
    if (water > 0) lines.add("  → $water water bottle${if (water != 1) "s" else ""}")
    return lines
}

fun extraSupplyLines(items: List<CateringItem>): List<String> =
    consolidateSupplyLines(
        items.filterIsInstance<CateringItem.Extra>()
            .filter {
                it.extraType != ExtraType.LARGE_TONG && it.extraType != ExtraType.SMALL_TONG &&
                        it.extraType != ExtraType.SPOON &&
                        // Dressing bottle extras are merged into the salad dressing tally instead
                        // (see saladDressingSupplyLines) so they combine rather than list separately.
                        it.extraType != ExtraType.DRESSING_BOTTLE
            }
            .flatMap { it.supplyLines() },
    )

// ─── Breadstick logic ─────────────────────────────────────────────────────────

/**
 * Total raw breadsticks needed across all pasta trays.
 * Feeds 7 = 7 breadsticks, Feeds 15 = 15 breadsticks.
 */
fun totalRawBreadsticks(items: List<CateringItem>): Int =
    items.filterIsInstance<CateringItem.PastaTray>().sumOf { it.rawBreadsticks }

/**
 * Breadsticks the baker should make — raw total rounded UP to the nearest
 * multiple of 3 (since they always bake in multiples of 3).
 * Returns 0 if there are no pasta trays.
 */
fun totalBreadsticksForBaker(items: List<CateringItem>): Int {
    val raw = totalRawBreadsticks(items)
    if (raw == 0) return 0
    return if (raw % 3 == 0) raw else raw + (3 - raw % 3)
}

/** Kitchen grab list — shared by combined and per-order supply views. */
fun buildCondensedSupplyLines(
    items: List<CateringItem>,
    plateCount: Int,
    napkinCount: Int,
    forksNeeded: Int,
    utensilTally: ServingUtensilTally,
    cateringBoxCount: Int,
): List<String> {
    val lines = mutableListOf<String>()
    val sandwiches = items.filterIsInstance<CateringItem.Sandwiches>()
    val salads = items.filterIsInstance<CateringItem.Salad>()
    val desserts = items.filterIsInstance<CateringItem.Dessert>()

    val brownies = totalBrowniesFromDesserts(desserts)
    if (brownies > 0) lines.add(formatSupplyQty(brownies, "brownies"))

    val cookies = cookieTotalsForItems(items)
    if (cookies.total > 0) {
        lines.add(cookieBakeSupplyLine(cookies))
        lines.addAll(cookieSourceBreakdownLines(items))
    }
    lines.addAll(dessertPlatterSupplyLines(desserts))

    // Breadsticks for pasta trays
    val breadsticks = totalBreadsticksForBaker(items)
    if (breadsticks > 0) {
        val raw = totalRawBreadsticks(items)
        val note = if (breadsticks > raw) " (rounded up from $raw)" else ""
        lines.add(formatSupplyQty(breadsticks, "breadstick${if (breadsticks != 1) "s" else ""} for baker$note"))
    }

    lines.addAll(plateNapkinSupplyLines(plateCount, napkinCount))
    if (forksNeeded > 0) lines.add(formatSupplyQty(forksNeeded, "forks"))
    lines.addAll(utensilSupplyLines(utensilTally))

    lines.addAll(sandwichCondimentSideLines(sandwiches, combinePortions = true))
    lines.addAll(boxLunchSupplyLines(items.filterIsInstance<CateringItem.BoxLunch>()))
    lines.addAll(
        saladDressingSupplyLines(
            salads,
            items.filterIsInstance<CateringItem.Extra>().filter { it.extraType == ExtraType.DRESSING_BOTTLE },
        ),
    )

    if (cateringBoxCount > 0) {
        lines.add(formatSupplyQty(cateringBoxCount, "catering box${if (cateringBoxCount != 1) "es" else ""}"))
    }
    lines.addAll(extraSupplyLines(items))

    return lines
}

fun gardenDressingSupplyKey(dressing: GardenDressing): String = when (dressing) {
    GardenDressing.RANCH -> "Ranch bottles"
    GardenDressing.BLUE_CHEESE -> "Blue Cheese bottles"
    GardenDressing.POPPYSEED -> "Poppyseed bottles"
    GardenDressing.CAESAR -> "Caesar dressing bottles"
    GardenDressing.ITALIAN -> "Italian bottles"
    GardenDressing.THOUSAND_ISLAND -> "1000 Island bottles"
    GardenDressing.VNO -> "Vinegar & Oil (VnO)"
    GardenDressing.UNKNOWN -> "Unknown dressing bottles"
}

/** Half-bottle units per salad unit — canonical keys so Pierce + Garden/Cobb/Chef dressings merge correctly. */
fun CateringItem.Salad.dressingHalfUnitsPerUnit(): List<Pair<String, Int>> = when {
    saladType == SaladType.PIERCE && !usesCustomDressingNow -> listOf("Poppyseed bottles" to dressingBottlesPerSalad * 2)
    saladType == SaladType.CAESAR && !usesCustomDressingNow -> listOf("Caesar dressing bottles" to dressingBottlesPerSalad * 2)
    saladType == SaladType.CARUSOS && !usesCustomDressingNow -> if (saladSize == SaladSize.FEEDS_15) {
        listOf("Blue cheese dressing" to 2, "Vinegar & Oil (VnO)" to 2)
    } else {
        listOf("Blue cheese dressing" to 1, "Vinegar & Oil (VnO)" to 1)
    }
    usesCustomDressingNow -> {
        val totalBottles = dressingBottlesPerSalad
        val dressingList = gardenDressings.toList()
        when {
            // Feeds 15, 2 dressings picked → 1 full bottle of each.
            dressingList.size == 2 && totalBottles == 2 ->
                dressingList.map { gardenDressingSupplyKey(it) to 2 }
            // Feeds 7, 2 dressings picked → ½ bottle of each (2 half-dressings total).
            dressingList.size == 2 && totalBottles == 1 ->
                dressingList.map { gardenDressingSupplyKey(it) to 1 }
            dressingList.size == 1 ->
                listOf(gardenDressingSupplyKey(dressingList[0]) to totalBottles * 2)
            else -> listOf("dressing bottles" to totalBottles * 2)
        }
    }
    else -> emptyList()
}

fun saladDressingSupplyLines(
    salads: List<CateringItem.Salad>,
    extraDressings: List<CateringItem.Extra> = emptyList(),
): List<String> {
    val bottleHalfUnits = linkedMapOf<String, Int>()
    salads.forEach { salad ->
        repeat(salad.quantity) {
            salad.dressingHalfUnitsPerUnit().forEach { (key, halves) ->
                bottleHalfUnits[key] = (bottleHalfUnits[key] ?: 0) + halves
            }
        }
    }
    // Extra "Dressing Bottle" entries combine into the same tally instead of listing separately.
    extraDressings.forEach { extra ->
        val key = extra.dressingChoice?.let { gardenDressingSupplyKey(it) } ?: "Dressing bottles"
        bottleHalfUnits[key] = (bottleHalfUnits[key] ?: 0) + extra.count * 2
    }
    return bottleHalfUnits.map { (desc, halves) -> formatBottleHalfUnits(desc, halves) }.sorted()
}

fun cookieBakeSupplyLine(cookies: CookieTotals): String =
    "Bake ${cookies.chocolate} choc + ${cookies.macadamia} mac cookies (${cookies.total} total)"

/** Sub-lines under the bake total — where cookies come from. */
fun cookieSourceBreakdownLines(items: List<CateringItem>): List<String> {
    val lines = mutableListOf<String>()
    val boxLunches = items.filterIsInstance<CateringItem.BoxLunch>()
    if (boxLunches.isNotEmpty()) {
        var choc = 0
        var mac = 0
        boxLunches.forEach { bl ->
            choc += bl.count / 2 + bl.count % 2
            mac += bl.count / 2
        }
        val total = boxLunches.sumOf { it.count }
        lines.add("  → $choc choc + $mac mac from $total box lunch${if (total != 1) "es" else ""}")
    }
    val desserts = items.filterIsInstance<CateringItem.Dessert>()
    val dessertChoc = desserts.sumOf { it.chocolateCookies }
    val dessertMac = desserts.sumOf { it.macadamiaCookies }
    if (dessertChoc + dessertMac > 0) {
        lines.add("  → $dessertChoc choc + $dessertMac mac for dessert platters")
    }
    val extraCookies = items.filterIsInstance<CateringItem.Extra>().filter { it.extraType == ExtraType.COOKIES }
    if (extraCookies.isNotEmpty()) {
        var choc = 0
        var mac = 0
        extraCookies.forEach { extra ->
            when (extra.cookieFlavor) {
                CookieFlavor.CHOCOLATE -> choc += extra.count
                CookieFlavor.MACADAMIA -> mac += extra.count
                CookieFlavor.FIFTY_FIFTY, null -> {
                    choc += extra.count / 2 + extra.count % 2
                    mac += extra.count / 2
                }
            }
        }
        lines.add("  → $choc choc + $mac mac from extras")
    }
    return lines
}

/** Which dessert platters to assemble (combined across orders). */
fun dessertPlatterSupplyLines(desserts: List<CateringItem.Dessert>): List<String> =
    desserts.groupBy { it.dessertType }
        .map { (type, items) ->
            val count = items.sumOf { it.count }
            when (type) {
                DessertType.COOKIE_PLATTER ->
                    formatSupplyQty(count, "Cookie platter to build (12 choc + 12 mac each)")
                DessertType.BROWNIE_COOKIE ->
                    formatSupplyQty(count, "Brownie & Cookie platter to build (3 brownies + 7 choc + 7 mac each)")
                DessertType.BROWNIE_PLATTER ->
                    formatSupplyQty(count, "Brownie platter to build (10 brownies each)")
            }
        }
        .sortedBy { it }

fun CateringItem.Salad.handoffDescription(): String {
    val chicken = if (addChicken) " + chicken" else ""
    val dressing = if (usesCustomDressingNow && gardenDressings.isNotEmpty())
        " (${gardenDressings.joinToString(" + ") { it.label }})" else ""
    val croutons = if (croutonsOnSide) " + croutons on side" else ""
    return "${saladSize.label} ${saladType.label}$chicken$dressing$croutons"
}

fun saladBelongsInSaladsSection(salad: CateringItem.Salad): Boolean =
    !salad.saladType.isPasta && salad.saladType != SaladType.SPRING_PEA

fun saladBelongsInOtherSection(salad: CateringItem.Salad): Boolean =
    salad.saladType.isPasta || salad.saladType == SaladType.SPRING_PEA

fun CateringItem.Salad.saladsSectionKey(): String {
    val chicken = if (addChicken) " w/ chicken" else ""
    return "${saladSize.label} ${saladType.label}$chicken"
}

fun CateringItem.PastaTray.pastaSectionKey(): String {
    val base = "${pastaSize.label} ${friendlyName}"
    val hasChicken = addOns.any { it.equals("Chicken", ignoreCase = true) }
    return if (hasChicken) "$base w/ Chicken" else base
}

fun CateringItem.Extra.otherHandoffKey(): String? = when (extraType) {
    ExtraType.OTHER -> if (otherNote.isNotBlank()) "Other ($otherNote)" else "Other"
    // SOUP gets its own dedicated line in the EXTRAS handoff section (see buildHandoffSections),
    // so it's excluded from this generic path. The rest are supply-list only, never in handoff:
    ExtraType.SOUP,
    ExtraType.CHIPS,
    ExtraType.PEPSI_CANS,
    ExtraType.WATER_BOTTLES,
    ExtraType.COOKIES,
    ExtraType.DRESSING_BOTTLE,
    ExtraType.LARGE_TONG,
    ExtraType.SMALL_TONG,
    ExtraType.SPOON -> null
}

/**
 * Returns true if this item should appear in the "ITEMS TO PACK" checklist section
 * of the per-order PDF checklist. Supply-only items (chips, extra drinks, utensils,
 * dressings) are excluded — they're already covered by the SUPPLIES section.
 */
fun CateringItem.isPackingItem(): Boolean = when (this) {
    is CateringItem.Extra -> when (extraType) {
        ExtraType.CHIPS,
        ExtraType.PEPSI_CANS,
        ExtraType.WATER_BOTTLES,
        ExtraType.LARGE_TONG,
        ExtraType.SMALL_TONG,
        ExtraType.SPOON,
        ExtraType.DRESSING_BOTTLE,
        ExtraType.COOKIES -> false
        ExtraType.SOUP,
        ExtraType.OTHER   -> true
    }
    else -> true
}

fun shouldAggressiveHandoffConsolidation(items: List<CateringItem>): Boolean =
    items.size >= 28

private data class SaladHandoffGroupKey(val type: SaladType, val addChicken: Boolean)

private data class PastaHandoffGroupKey(
    val pastaType: PastaType,
    val noodle: BYONoodle?,
    val sauce: BYOSauce?,
    val hasChicken: Boolean,
)

fun consolidatedSaladHandoffLines(salads: List<CateringItem.Salad>, aggressive: Boolean): List<String> {
    if (!aggressive) {
        return consolidatedHandoffLines(salads, { it.saladsSectionKey() }, { it.quantity })
    }
    return salads.groupBy { SaladHandoffGroupKey(it.saladType, it.addChicken) }
        .map { (key, group) ->
            val total = group.sumOf { it.quantity }
            val sizes = group.map { it.saladSize }.distinct()
            val chicken = if (key.addChicken) " w/ chicken" else ""
            val sizeLabel = if (sizes.size == 1) "${sizes.first().label} " else ""
            "${total}x ${sizeLabel}${key.type.label}$chicken".trim()
        }
        .sorted()
}

fun consolidatedPastaHandoffLines(pastas: List<CateringItem.PastaTray>, aggressive: Boolean): List<String> {
    if (!aggressive) {
        return consolidatedHandoffLines(pastas, { it.pastaSectionKey() }, { it.quantity })
    }
    return pastas.groupBy { pasta ->
        PastaHandoffGroupKey(
            pasta.pastaType,
            pasta.byoNoodle,
            pasta.byoSauce,
            pasta.addOns.any { it.equals("Chicken", ignoreCase = true) },
        )
    }.map { (key, group) ->
        val total = group.sumOf { it.quantity }
        val sizes = group.map { it.pastaSize }.distinct()
        val sizeLabel = if (sizes.size == 1) "${sizes.first().label} " else ""
        val name = when {
            key.pastaType == PastaType.BYO && key.noodle != null && key.sauce != null ->
                "BYO Pasta (${key.noodle.label} / ${key.sauce.label})"
            key.pastaType == PastaType.BYO -> group.first().friendlyName
            else -> key.pastaType.label
        }
        val chicken = if (key.hasChicken) " w/ Chicken" else ""
        "${total}x ${sizeLabel}$name$chicken".trim()
    }.sorted()
}

data class HandoffSections(
    val sandwiches: List<String> = emptyList(),
    val salads: List<String> = emptyList(),
    val pastas: List<String> = emptyList(),
    val other: List<String> = emptyList(),
    val individual: List<String> = emptyList(),
    val desserts: List<String> = emptyList(),
    val extras: List<String> = emptyList(),
    val headsUp: List<String> = emptyList(),
)

fun buildHandoffSections(allItems: List<CateringItem>, aggressive: Boolean = false): HandoffSections {
    val sandwiches = buildList {
        val trays = allItems.filterIsInstance<CateringItem.Sandwiches>()
        SandwichPortionSize.values().forEach { portion ->
            val total = trays.filter { it.portionSize == portion }.sumOf { it.sandwichCount }
            if (total > 0) add("${total}x ${portion.label} sandwiches")
        }
        val sandwichBoxes = allItems.filterIsInstance<CateringItem.BoxLunch>().filter { !it.isWrap }.sumOf { it.count }
        if (sandwichBoxes > 0) add("${sandwichBoxes}x Sandwich boxed lunch${if (sandwichBoxes != 1) "es" else ""}")
    }

    val salads = buildList {
        val wrapTrays = allItems.filterIsInstance<CateringItem.Wraps>().sumOf { it.count }
        if (wrapTrays > 0) add("${wrapTrays}x wraps")
        val wrapBoxes = allItems.filterIsInstance<CateringItem.BoxLunch>().filter { it.isWrap }.sumOf { it.count }
        if (wrapBoxes > 0) add("${wrapBoxes}x Wrap boxed lunch${if (wrapBoxes != 1) "es" else ""}")
        addAll(
            consolidatedSaladHandoffLines(
                allItems.filterIsInstance<CateringItem.Salad>().filter { saladBelongsInSaladsSection(it) },
                aggressive,
            ),
        )
    }

    val pastas = buildList {
        addAll(consolidatedPastaHandoffLines(allItems.filterIsInstance<CateringItem.PastaTray>(), aggressive))
        // Pasta-style salads (Italian/CBR pasta salad, Spring Pea) travel with the pasta section.
        addAll(
            consolidatedHandoffLines(
                allItems.filterIsInstance<CateringItem.Salad>().filter { saladBelongsInOtherSection(it) },
                { "${it.saladSize.label} ${it.saladType.label}" },
                { it.quantity },
            ),
        )
    }

    val other = buildList {
        val pizzaCount = allItems.filterIsInstance<CateringItem.Pizza>().sumOf { it.count }
        if (pizzaCount > 0) add("${pizzaCount}x pizza${if (pizzaCount != 1) "s" else ""}")
    }

    val individual = buildList {
        val indivHalves = allItems.filterIsInstance<CateringItem.IndividualItems>()
            .filter { it.itemType == IndividualItemType.HALF_SANDWICH }.sumOf { it.count }
        val indivFull = allItems.filterIsInstance<CateringItem.IndividualItems>()
            .filter { it.itemType == IndividualItemType.FULL_SANDWICH }.sumOf { it.count }
        if (indivHalves > 0) add("${indivHalves}x half sandwich${if (indivHalves != 1) "es" else ""}")
        if (indivFull   > 0) add("${indivFull}x full sandwich${if (indivFull != 1) "es" else ""}")
        val indivLarge = allItems.filterIsInstance<CateringItem.IndividualItems>()
            .filter { it.itemType == IndividualItemType.LARGE_SALAD }.sumOf { it.count }
        val indivSmall = allItems.filterIsInstance<CateringItem.IndividualItems>()
            .filter { it.itemType == IndividualItemType.SMALL_SALAD }.sumOf { it.count }
        if (indivLarge > 0) add("${indivLarge}x large salad${if (indivLarge != 1) "s" else ""}")
        if (indivSmall > 0) add("${indivSmall}x small salad${if (indivSmall != 1) "s" else ""}")
    }

    val desserts = consolidatedHandoffLines(
        allItems.filterIsInstance<CateringItem.Dessert>(),
        { it.dessertType.label },
        { it.count },
    )

    val extras = buildList {
        val soups = allItems.filterIsInstance<CateringItem.Extra>().filter { it.extraType == ExtraType.SOUP }.sumOf { it.count }
        if (soups > 0) add("${soups}x cup${if (soups != 1) "s" else ""} of soup")
        addAll(
            consolidatedHandoffLines(
                allItems.filterIsInstance<CateringItem.Extra>().filter { it.otherHandoffKey() != null },
                { it.otherHandoffKey()!! },
                { it.count },
            ),
        )
    }

    val headsUp = buildList {
        val loaves = totalLoafEquivalents(allItems)
        if (loaves >= 10) add("${Math.ceil(loaves).toInt()} loaves of sourdough needed")
        // Breadstick totals for the baker
        val breadsticks = totalBreadsticksForBaker(allItems)
        if (breadsticks > 0) {
            add("${breadsticks}x catering breadstick${if (breadsticks != 1) "s" else ""} needed")
        }
        lettuceHandoffLine(allItems)?.let { add(it) }
    }

    return HandoffSections(sandwiches, salads, pastas, other, individual, desserts, extras, headsUp)
}

fun totalLoafEquivalents(items: List<CateringItem>): Double {
    var loaves = 0.0
    items.forEach { item ->
        when (item) {
            is CateringItem.Sandwiches      -> loaves += item.loafEquivalent
            is CateringItem.BoxLunch        -> loaves += item.loafEquivalent
            is CateringItem.IndividualItems -> loaves += item.loafEquivalent
            else -> {}
        }
    }
    return loaves
}

fun formatHandoffMessage(orders: List<CateringOrder>): String {
    if (orders.isEmpty()) return ""
    val allItems = orders.flatMap { it.items }
    val aggressive = shouldAggressiveHandoffConsolidation(allItems)
    val sb = StringBuilder()

    val today = java.util.Calendar.getInstance()
    val isSaturday = today.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
    val daysAhead = if (isSaturday) 2 else 1
    val targetCal = today.clone() as java.util.Calendar
    targetCal.add(java.util.Calendar.DAY_OF_YEAR, daysAhead)
    val dayLabel = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.US).format(targetCal.time)
    sb.appendLine("Hello everyone! We have catering on $dayLabel.")
    sb.appendLine()

    fun appendSection(title: String, lines: List<String>) {
        if (lines.isEmpty()) return
        sb.appendLine(title)
        lines.forEach { sb.appendLine(it) }
        sb.appendLine()
    }

    fun appendTimeBlock(blockLabel: String?, blockOrders: List<CateringOrder>) {
        if (blockOrders.isEmpty()) return
        val blockSections = buildHandoffSections(blockOrders.flatMap { it.items }, aggressive)
        blockLabel?.let { sb.appendLine(it) }
        appendSection("SANDWICHES", blockSections.sandwiches)
        appendSection("SALADS", blockSections.salads)
        appendSection("PASTAS", blockSections.pastas)
        appendSection("OTHER", blockSections.other)
        appendSection("INDIVIDUAL", blockSections.individual)
        appendSection("DESSERT", blockSections.desserts)
        appendSection("EXTRAS", blockSections.extras)
    }

    // Split delivery/pickup items by whether they're due before or after 2 PM.
    // Orders with a time we can't parse (blank or free-typed text) land in their own
    // "TIME TBD" block rather than being guessed into one side or the other.
    val beforeTwoPm = orders.filter { it.isBeforeTwoPm() == true }
    val afterTwoPm  = orders.filter { it.isBeforeTwoPm() == false }
    val timeTbd     = orders.filter { it.isBeforeTwoPm() == null }

    // The BEFORE/AFTER 2 PM labels only mean something when there's an actual split —
    // if nothing is due after 2 PM, just render the before-2pm items without the label.
    if (afterTwoPm.isNotEmpty()) {
        appendTimeBlock("BEFORE 2 PM", beforeTwoPm)
        appendTimeBlock("AFTER 2 PM", afterTwoPm)
    } else {
        appendTimeBlock(null, beforeTwoPm)
    }
    appendTimeBlock("TIME TBD", timeTbd)

    // Heads up stays combined across every order regardless of the 2 PM split.
    val combinedSections = buildHandoffSections(allItems, aggressive)
    val headsUpLines = buildList {
        addAll(combinedSections.headsUp)
        val deliveryNotes = deliveryNotesLines(orders)
        if (deliveryNotes.isNotEmpty()) {
            if (isNotEmpty()) add("")
            addAll(deliveryNotes)
        }
    }

    val orderNotes = orders.filter { it.note.isNotBlank() && it.noteInHandoff }
    val itemNotes = orders.flatMap { o ->
        o.items.filter { it.note.isNotBlank() && it.noteInHandoff }.map { o to it }
    }
    val hasNotes = orderNotes.isNotEmpty() || itemNotes.isNotEmpty()

    if (headsUpLines.isNotEmpty() || hasNotes) {
        sb.appendLine("HEADS UP")
        headsUpLines.forEach { sb.appendLine(it) }
        if (hasNotes) {
            if (headsUpLines.isNotEmpty()) sb.appendLine()
            sb.appendLine("Notes:")
            orderNotes.forEach { sb.appendLine("  • ${it.customerName.ifBlank { "Order" }}: ${it.note}") }
            itemNotes.forEach { (o, item) ->
                sb.appendLine("  • ${o.customerName.ifBlank { "Order" }} / ${item.displayName()}: ${item.note}")
            }
        }
        sb.appendLine()
    }

    sb.append("Thank you all!")
    return sb.toString()
}

// ─── Time parsing & delivery-clustering warning ────────────────────────────────

private const val TWO_PM_MINUTES = 14 * 60
const val CLOSE_DELIVERY_WINDOW_MINUTES = 15

/** Parses "10:00 AM" / "12:30 PM" / "14:15" into minutes since midnight. Null if unparseable. */
fun parseTimeToMinutesOfDay(time: String): Int? {
    val trimmed = time.trim()
    if (trimmed.isBlank()) return null
    Regex("""^(\d{1,2}):(\d{2})\s*([AaPp][Mm])$""").matchEntire(trimmed)?.let { m ->
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 1..12 || min !in 0..59) return null
        val isPm = m.groupValues[3].lowercase() == "pm"
        if (h == 12) h = 0
        return (if (isPm) h + 12 else h) * 60 + min
    }
    Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(trimmed)?.let { m ->
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || min !in 0..59) return null
        return h * 60 + min
    }
    return null
}

/** True if before 2 PM, false if 2 PM or later, null if the order's time couldn't be parsed. */
fun CateringOrder.isBeforeTwoPm(): Boolean? = parseTimeToMinutesOfDay(time)?.let { it < TWO_PM_MINUTES }

/**
 * Lists every delivery's time as its own "Delivery Notes" heads-up subsection, with a
 * warning symbol before any delivery scheduled within [CLOSE_DELIVERY_WINDOW_MINUTES]
 * minutes of another one.
 */
fun deliveryNotesLines(orders: List<CateringOrder>): List<String> {
    val deliveries = orders.filter { it.orderType == OrderType.DELIVERY }
    if (deliveries.isEmpty()) return emptyList()

    val parsedTimeById = deliveries.associate { it.id to parseTimeToMinutesOfDay(it.time) }
    val timed = deliveries.mapNotNull { o -> parsedTimeById[o.id]?.let { o to it } }

    val closeIds = mutableSetOf<String>()
    for (i in timed.indices) {
        for (j in i + 1 until timed.size) {
            if (kotlin.math.abs(timed[i].second - timed[j].second) <= CLOSE_DELIVERY_WINDOW_MINUTES) {
                closeIds += timed[i].first.id
                closeIds += timed[j].first.id
            }
        }
    }

    // Parseable times sort chronologically; anything unparseable/blank falls to the end.
    val sorted = deliveries.sortedWith(compareBy { parsedTimeById[it.id] ?: Int.MAX_VALUE })

    return buildList {
        add("Delivery Notes:")
        sorted.forEach { order ->
            val warn = if (order.id in closeIds) "⚠ " else ""
            val name = order.customerName.ifBlank { "Unnamed order" }
            val timeLabel = order.time.ifBlank { "no time set" }
            add("  $warn$name — $timeLabel")
        }
    }
}

// ─── Saved days: duplicate-name detection & display formatting ────────────────

private fun normalizeCustomerName(name: String): String =
    name.trim().lowercase(java.util.Locale.US).replace(Regex("[^a-z0-9]"), "")

private fun levenshteinDistance(a: String, b: String): Int {
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
    }
    return dp[a.length][b.length]
}

/**
 * Groups orders whose customer names look like likely duplicates/typos of each other
 * (exact match after normalizing, or a small edit distance). Used after combining two
 * saved days so the user can review and choose which to delete. Orders with a blank name
 * are never grouped — nothing to compare.
 */
fun findSimilarNameGroups(orders: List<CateringOrder>): List<List<CateringOrder>> {
    val named = orders.filter { it.customerName.isNotBlank() }
    val used = mutableSetOf<String>()
    val groups = mutableListOf<List<CateringOrder>>()
    for (i in named.indices) {
        if (named[i].id in used) continue
        val a = normalizeCustomerName(named[i].customerName)
        val group = mutableListOf(named[i])
        for (j in i + 1 until named.size) {
            if (named[j].id in used) continue
            val b = normalizeCustomerName(named[j].customerName)
            val maxLen = maxOf(a.length, b.length)
            val allowedDistance = if (maxLen <= 4) 1 else 2
            if (maxLen > 0 && (a == b || levenshteinDistance(a, b) <= allowedDistance)) group.add(named[j])
        }
        if (group.size > 1) {
            group.forEach { used += it.id }
            groups.add(group)
        }
    }
    return groups
}

private val DAY_KEY_FORMAT = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
private val DAY_DISPLAY_FORMAT = java.text.SimpleDateFormat("EEEE, MMM d, yyyy", java.util.Locale.US)

/** Formats a "yyyy-MM-dd" saved-day key for display, e.g. "Monday, Jan 12, 2026". */
fun formatDayKeyForDisplay(dateKey: String): String =
    try { DAY_DISPLAY_FORMAT.format(DAY_KEY_FORMAT.parse(dateKey)!!) } catch (e: Exception) { dateKey }

// ─── Export / import a day as JSON (for sharing between shift leads) ──────────

/** Plain-text, shareable snapshot of one day's orders. [dateKey] is null when exported from Current Catering. */
fun exportDayToJson(dateKey: String?, dayOrders: List<CateringOrder>): String {
    val obj = JSONObject()
    obj.put("export_version", 1)
    obj.put("date", dateKey ?: JSONObject.NULL)
    obj.put("orders", JSONArray(dayOrders.map { it.toJson() }))
    return obj.toString(2)
}

data class ImportedDay(val dateKey: String?, val orders: List<CateringOrder>)

/** Parses text produced by [exportDayToJson]. Returns null if it's not recognizable — never throws. */
fun parseExportedDayJson(json: String): ImportedDay? {
    return try {
        val obj = JSONObject(json)
        val dateKey = if (obj.isNull("date")) null else obj.optString("date").ifBlank { null }
        val arr = obj.getJSONArray("orders")
        val orders = (0 until arr.length()).map { parseCateringOrder(arr.getJSONObject(it)) }
        ImportedDay(dateKey, orders)
    } catch (e: Exception) {
        null
    }
}

fun CateringItem.PastaTray.handoffDescription(): String {
    val addOnStr = if (addOns.isNotEmpty()) " + ${addOns.joinToString()}" else ""
    return "${pastaSize.label} ${friendlyName}$addOnStr"
}

fun <T> consolidatedHandoffLines(items: List<T>, description: (T) -> String, quantity: (T) -> Int): List<String> =
    items.groupBy(description)
        .map { (desc, group) -> "${group.sumOf(quantity)}x $desc" }
        .sorted()

const val LETTUCE_SCOOPS_PER_CONTAINER = 112

fun totalLettuceScoops(items: List<CateringItem>): Int {
    var scoops = 0
    items.forEach { item ->
        when (item) {
            is CateringItem.Wraps          -> scoops += item.count
            is CateringItem.Salad          -> scoops += item.lettuceScoops
            is CateringItem.BoxLunch       -> if (item.isWrap) scoops += item.count
            is CateringItem.IndividualItems -> scoops += item.lettuceScoops
            else -> {}
        }
    }
    return scoops
}

/** Round scoop total to the nearest ¼ box (28 scoops). */
fun roundLettuceToNearestQuarterBox(scoops: Int): Double =
    Math.round(scoops / LETTUCE_SCOOPS_PER_CONTAINER.toDouble() * 4) / 4.0

fun formatQuarterBoxAmount(boxes: Double): String {
    val quarters = Math.round(boxes * 4).toInt()
    val whole = quarters / 4
    val frac = quarters % 4
    val fracStr = when (frac) {
        1 -> "¼"
        2 -> "½"
        3 -> "¾"
        else -> ""
    }
    return when {
        whole == 0 -> fracStr
        frac == 0 -> whole.toString()
        else -> "$whole$fracStr"
    }
}

fun formatLettuceBoxEstimate(boxes: Double): String = when {
    boxes <= 1.0 -> when (boxes) {
        0.5 -> "We will need around half a box of salad lettuce cut for catering"
        0.75 -> "We will need around three-quarters of a box of salad lettuce cut for catering"
        1.0 -> "We will need around a full box of salad lettuce cut for catering"
        else -> "We will need around ${formatQuarterBoxAmount(boxes)} of a box of salad lettuce cut for catering"
    }
    else -> "We will use around ${formatQuarterBoxAmount(boxes)} boxes of salad lettuce for catering"
}

/** Returns a handoff estimate when rounded need is at least ½ box. */
fun lettuceHandoffLine(items: List<CateringItem>): String? {
    val scoops = totalLettuceScoops(items)
    if (scoops == 0) return null
    val roundedBoxes = roundLettuceToNearestQuarterBox(scoops)
    if (roundedBoxes < 0.5) return null
    return formatLettuceBoxEstimate(roundedBoxes)
}

fun noteVisibilitySuffix(inHandoff: Boolean, inSupplyList: Boolean): String {
    val excluded = buildList {
        if (!inHandoff) add("handoff")
        if (!inSupplyList) add("supply list")
    }
    return when (excluded.size) {
        0 -> ""
        1 -> " (not in ${excluded[0]})"
        else -> " (not in ${excluded.joinToString(" or ")})"
    }
}

fun supplyNoteLinesForOrder(order: CateringOrder, prefixCustomer: Boolean = false): List<String> {
    val customer = order.customerName.ifBlank { "Order" }
    return buildList {
        if (order.note.isNotBlank() && order.noteInSupplyList) {
            add(if (prefixCustomer) "Note — $customer: ${order.note}" else "Note — ${order.note}")
        }
        order.items.filter { it.note.isNotBlank() && it.noteInSupplyList }.forEach { item ->
            add(
                if (prefixCustomer) "Note — $customer / ${item.displayName()}: ${item.note}"
                else "Note — ${item.displayName()}: ${item.note}",
            )
        }
    }
}

fun sandwichPortionBoxLine(total: Int, portion: SandwichPortionSize, boxes: Int): String =
    formatSupplyQty(
        total,
        "${portion.label} sandwiches → $boxes catering box${if (boxes != 1) "es" else ""}",
    )

/** Merge supply lines; bottle amounts (½ or leading number) sum in half-bottle units. */
fun consolidateSupplyLines(lines: List<String>): List<String> {
    val bottleHalfUnits = linkedMapOf<String, Int>()
    val plainCounts = linkedMapOf<String, Int>()
    val numberRegex = Regex("""^(\d+)\s+(.+)$""")
    val halfRegex = Regex("""^½\s+(.+)$""")
    val qtyPrefixRegex = Regex("""^(\d+)×\s+(.+)$""")

    for (line in lines) {
        val trimmed = line.trim()
        val numeric = numberRegex.matchEntire(trimmed)
        val half = halfRegex.matchEntire(trimmed)
        val qtyPrefixed = qtyPrefixRegex.matchEntire(trimmed)
        when {
            half != null -> {
                val desc = half.groupValues[1]
                bottleHalfUnits[desc] = (bottleHalfUnits[desc] ?: 0) + 1
            }
            numeric != null -> {
                val desc = numeric.groupValues[2]
                bottleHalfUnits[desc] = (bottleHalfUnits[desc] ?: 0) + numeric.groupValues[1].toInt() * 2
            }
            qtyPrefixed != null -> {
                val desc = qtyPrefixed.groupValues[2]
                plainCounts[desc] = (plainCounts[desc] ?: 0) + qtyPrefixed.groupValues[1].toInt()
            }
            else -> plainCounts[trimmed] = (plainCounts[trimmed] ?: 0) + 1
        }
    }

    val bottleLines = bottleHalfUnits.map { (desc, halves) -> formatBottleHalfUnits(desc, halves) }
    val plainLines = plainCounts.map { (key, count) -> formatSupplyQty(count, key) }
    return plainLines + bottleLines
}

private fun formatBottleHalfUnits(description: String, halfUnits: Int): String {
    val full = halfUnits / 2
    val remainder = halfUnits % 2
    return when {
        remainder == 0 -> formatSupplyQty(full, description)
        full == 0 -> "½ $description"
        else -> "$full½ $description"
    }
}

data class ServingUtensilTally(val largeTongs: Int = 0, val smallTongs: Int = 0, val spoons: Int = 0) {
    fun summaryOrNull(): String? {
        val parts = buildList {
            if (largeTongs > 0) add(formatSupplyQty(largeTongs, "large tong${if (largeTongs != 1) "s" else ""}"))
            if (smallTongs > 0) add(formatSupplyQty(smallTongs, "small tong${if (smallTongs != 1) "s" else ""}"))
            if (spoons > 0) add(formatSupplyQty(spoons, "spoon${if (spoons != 1) "s" else ""}"))
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}

fun CateringOrder.tallyServingUtensils(): ServingUtensilTally {
    var largeTongs = 0
    var smallTongs = 0
    var spoons = 0
    items.forEach { item ->
        when (item) {
            is CateringItem.Salad -> if (needsServingUtensils) {
                val utensil = when {
                    item.saladType.isPasta -> "spoon"
                    item.saladType.useSpoon -> "spoon"
                    else -> "large tong"
                }
                repeat(item.quantity) { if (utensil == "large tong") largeTongs++ else spoons++ }
            }
            is CateringItem.PastaTray -> if (needsServingUtensils) {
                repeat(item.quantity) { if (item.effectivelyUsesTong) largeTongs++ else spoons++ }
            }
            is CateringItem.Dessert -> if (needsServingUtensils) smallTongs += item.count
            is CateringItem.Extra -> when (item.extraType) {
                ExtraType.LARGE_TONG -> largeTongs += item.count
                ExtraType.SMALL_TONG -> smallTongs += item.count
                ExtraType.SPOON -> spoons += item.count
                else -> {}
            }
            else -> {}
        }
    }

    // Pasta trays need 1 small tong for breadstick service.
    val hasPasta = items.any { it is CateringItem.PastaTray }
    if (hasPasta) smallTongs++

    // Croutons on side: 1 spoon per every 2 feeds-15 equivalents (feeds-7 = 0.5, feeds-15 = 1.0),
    // rounded up. Examples: 1x feeds-15 → 1 spoon, 2x feeds-15 → 1 spoon, 3x feeds-15 → 2 spoons,
    // 4x feeds-7 → 1 spoon, 5x feeds-7 → 2 spoons.
    // NOTE: This ratio (1 spoon per 2 feeds-15 worth) may need tuning once we see real volume —
    // update the feeds15Equivalents arithmetic below if the threshold changes.
    val croutonSalads = items.filterIsInstance<CateringItem.Salad>().filter { it.croutonsOnSide }
    if (croutonSalads.isNotEmpty()) {
        val feeds15Equivalents = croutonSalads.sumOf { salad ->
            if (salad.saladSize == SaladSize.FEEDS_15) salad.quantity.toDouble() else salad.quantity / 2.0
        }
        val croutonSpoons = Math.ceil(feeds15Equivalents / 2.0).toInt().coerceAtLeast(1)
        spoons += croutonSpoons
    }

    return ServingUtensilTally(largeTongs, smallTongs, spoons)
}

/** Dressing lines for one salad (repeat per quantity in supply builders). */
fun CateringItem.Salad.dressingLinesPerUnit(): List<String> =
    dressingHalfUnitsPerUnit().map { (key, halves) -> formatBottleHalfUnits(key, halves) }

fun CateringItem.Salad.saladSupplyHeader(): String =
    "${saladSize.label} ${saladType.label}${if (addChicken) " + chicken" else ""}"

fun cookieTotalsForItems(items: List<CateringItem>): CookieTotals {
    var choc = 0
    var mac = 0
    items.forEach { item ->
        when (item) {
            is CateringItem.Dessert  -> { choc += item.chocolateCookies; mac += item.macadamiaCookies }
            is CateringItem.BoxLunch -> { choc += item.count / 2 + item.count % 2; mac += item.count / 2 }
            is CateringItem.Extra    -> if (item.extraType == ExtraType.COOKIES) {
                when (item.cookieFlavor) {
                    CookieFlavor.CHOCOLATE   -> choc += item.count
                    CookieFlavor.MACADAMIA   -> mac += item.count
                    CookieFlavor.FIFTY_FIFTY, null -> {
                        choc += item.count / 2 + item.count % 2
                        mac += item.count / 2
                    }
                }
            }
            else -> {}
        }
    }
    return CookieTotals(choc, mac)
}

// ─── Order ────────────────────────────────────────────────────────────────────

data class CateringOrder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String = "",
    val time: String = "",
    val address: String = "",
    val orderType: OrderType = OrderType.PICKUP,
    val headcount: Int = 0,
    val needsPlates: Boolean = false,
    val needsNapkins: Boolean = false,
    val needsForks: Boolean = false,
    val needsServingUtensils: Boolean = false,
    val note: String = "",
    val noteInHandoff: Boolean = true,
    val noteInSupplyList: Boolean = false,
    val items: List<CateringItem> = emptyList(),
) {
    val plateCount  get() = if (needsPlates  && headcount > 0) (headcount * 1.1).toInt() else 0
    val napkinCount get() = if (needsNapkins && headcount > 0) (headcount * 1.1).toInt() else 0
    val forksNeeded get() = if (needsForks && headcount > 0) (headcount * 1.1).toInt() else 0

    // Per-order sandwich box counts (summed across all sandwich entries for this order)
    fun sandwichBoxesForOrder(): Map<SandwichPortionSize, Int> {
        val thirds   = items.filterIsInstance<CateringItem.Sandwiches>().filter { it.portionSize == SandwichPortionSize.THIRD }.sumOf { it.sandwichCount }
        val quarters = items.filterIsInstance<CateringItem.Sandwiches>().filter { it.portionSize == SandwichPortionSize.QUARTER }.sumOf { it.sandwichCount }
        val result = mutableMapOf<SandwichPortionSize, Int>()
        if (thirds   > 0) result[SandwichPortionSize.THIRD]   = Math.ceil(thirds   / 18.0).toInt()
        if (quarters > 0) result[SandwichPortionSize.QUARTER] = Math.ceil(quarters / 24.0).toInt()
        return result
    }

    fun wrapBoxesForOrder(): Int {
        val total = items.filterIsInstance<CateringItem.Wraps>().sumOf { it.count }
        return if (total > 0) Math.ceil(total / 15.0).toInt() else 0
    }

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", customerName); put("time", time); put("address", address)
        put("order_type", orderType.name); put("headcount", headcount)
        put("needs_plates", needsPlates); put("needs_napkins", needsNapkins)
        put("needs_utensils", needsForks); put("needs_serving_utensils", needsServingUtensils)
        put("note", note); put("note_handoff", noteInHandoff); put("note_supply", noteInSupplyList)
        put("items", JSONArray(items.map { it.toJson() }))
    }
    companion object
}

// ─── JSON parsing ─────────────────────────────────────────────────────────────

fun parseCateringItem(obj: JSONObject): CateringItem? = try {
    val handoff = obj.optBoolean("note_handoff", true)
    val supply = obj.optBoolean("note_supply", false)
    when (obj.getString("type_key")) {
        "sandwiches", "sandwich_tray" -> CateringItem.Sandwiches(
            type             = SandwichTrayType.valueOf(obj.getString("tray_type")),
            portionSize      = SandwichPortionSize.valueOf(obj.getString("portion")),
            sandwichCount    = obj.getInt("count"),
            condimentsOnSide = obj.optBoolean("condiments") || obj.optBoolean("mayo") || obj.optBoolean("mustard"),
            note             = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "wraps", "wrap_tray" -> CateringItem.Wraps(
            count = obj.getInt("count"), note = obj.optString("note"),
            noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "salad" -> CateringItem.Salad(
            saladType       = SaladType.valueOf(obj.getString("salad_type")),
            saladSize       = SaladSize.valueOf(obj.optString("size_salad", obj.optString("size", "FEEDS_15"))),
            quantity        = obj.optInt("quantity", 1),
            addChicken      = obj.optBoolean("add_chicken"),
            gardenDressings = obj.optJSONArray("garden_dressings")?.let { arr ->
                (0 until arr.length()).mapNotNull { runCatching { GardenDressing.valueOf(arr.getString(it)) }.getOrNull() }.toSet()
            } ?: obj.optString("garden_dressing").takeIf { it.isNotBlank() }?.let {
                runCatching { setOf(GardenDressing.valueOf(it)) }.getOrNull()
            } ?: emptySet(),
            croutonsOnSide  = obj.optBoolean("croutons_on_side", false),
            customDressing  = obj.optBoolean("custom_dressing", false),
            note = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "pasta" -> CateringItem.PastaTray(
            pastaType = PastaType.valueOf(obj.getString("pasta_type")),
            pastaSize = runCatching { PastaSize.valueOf(obj.optString("size_pasta", obj.optString("size", "FEEDS_15"))) }.getOrDefault(PastaSize.FEEDS_15),
            quantity  = obj.optInt("quantity", 1),
            byoNoodle = obj.optString("byo_noodle").takeIf { it.isNotBlank() }?.let { runCatching { BYONoodle.valueOf(it) }.getOrNull() },
            byoSauce  = obj.optString("byo_sauce").takeIf { it.isNotBlank() }?.let { runCatching { BYOSauce.valueOf(it) }.getOrNull() },
            addOns    = obj.optJSONArray("addons")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
            note = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "box_lunch" -> CateringItem.BoxLunch(
            count        = obj.getInt("count"),
            sandwichType = SandwichTrayType.valueOf(obj.optString("sandwich_type", "CLASSIC")),
            isWrap       = obj.optBoolean("is_wrap"),
            drinkChoice  = DrinkChoice.valueOf(obj.getString("drink")),
            pepsiCount   = obj.optInt("pepsi_count"), waterCount = obj.optInt("water_count"),
            note = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "soup"    -> CateringItem.Extra(   // legacy standalone Soup items now fold into Extras
            extraType = ExtraType.SOUP, count = obj.getInt("cups"), note = obj.optString("note"),
            noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "pizza"   -> CateringItem.Pizza(
            count = obj.getInt("count"), note = obj.optString("note"),
            noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "dessert" -> CateringItem.Dessert(
            dessertType = DessertType.valueOf(obj.getString("dessert_type")),
            count = obj.optInt("count", 1), note = obj.optString("note"),
            noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "extra" -> CateringItem.Extra(
            extraType     = ExtraType.valueOf(obj.getString("extra_type")),
            count         = obj.getInt("count"),
            cookieFlavor  = obj.optString("cookie_flavor").takeIf { it.isNotBlank() }?.let { runCatching { CookieFlavor.valueOf(it) }.getOrNull() },
            dressingChoice= obj.optString("dressing_choice").takeIf { it.isNotBlank() }?.let { runCatching { GardenDressing.valueOf(it) }.getOrNull() },
            otherNote     = obj.optString("other_note"),
            note = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        "individual_item" -> CateringItem.IndividualItems(
            itemType = IndividualItemType.valueOf(obj.getString("individual_type")),
            count    = obj.getInt("count"),
            note     = obj.optString("note"), noteInHandoff = handoff, noteInSupplyList = supply,
        )
        else -> null
    }
} catch (e: Exception) { null }

fun parseCateringOrder(obj: JSONObject): CateringOrder {
    val arr = obj.getJSONArray("items")
    // Legacy orders only had single combined toggles; if the new split key isn't present,
    // fall back to the old combined value for both sides of the split.
    val legacyPlates = obj.optBoolean("needs_plates")
    val legacyUtensils = obj.optBoolean("needs_utensils")
    return CateringOrder(
        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
        customerName = obj.optString("name"), time = obj.optString("time"), address = obj.optString("address"),
        orderType = runCatching { OrderType.valueOf(obj.optString("order_type", "PICKUP")) }.getOrDefault(OrderType.PICKUP),
        headcount = obj.optInt("headcount"),
        needsPlates = legacyPlates,
        needsNapkins = obj.optBoolean("needs_napkins", legacyPlates),
        needsForks = legacyUtensils,
        needsServingUtensils = obj.optBoolean("needs_serving_utensils", legacyUtensils),
        note = obj.optString("note"),
        noteInHandoff = obj.optBoolean("note_handoff", true),
        noteInSupplyList = obj.optBoolean("note_supply", false),
        items = (0 until arr.length()).mapNotNull { parseCateringItem(arr.getJSONObject(it)) },
    )
}
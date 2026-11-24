package com.tippool

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.json.JSONArray

class CateringViewModel : ViewModel() {

    var orders by mutableStateOf<List<CateringOrder>>(emptyList())
        private set

    /** null = the default "Current Catering" workspace; otherwise the saved day currently open. */
    var activeDayKey by mutableStateOf<String?>(null)
        private set

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("catering_orders", Context.MODE_PRIVATE)

    private fun loadCurrentCateringOrders(context: Context): List<CateringOrder> {
        val raw = prefs(context).getString("orders", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { parseCateringOrder(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Loads the default Current Catering workspace. This is always where the app opens to. */
    fun load(context: Context) {
        activeDayKey = null
        orders = loadCurrentCateringOrders(context)
    }

    /** Opens a previously-saved day for editing. Further edits auto-save back to that day. */
    fun openSavedDay(context: Context, dateKey: String) {
        activeDayKey = dateKey
        orders = loadSavedDay(context, dateKey)
    }

    /** Leaves a saved day and returns to the default Current Catering workspace. */
    fun returnToCurrentCatering(context: Context) {
        activeDayKey = null
        orders = loadCurrentCateringOrders(context)
    }

    private fun persist(context: Context) {
        val key = activeDayKey
        if (key == null) {
            prefs(context).edit().putString("orders", JSONArray(orders.map { it.toJson() }).toString()).apply()
        } else {
            saveSavedDay(context, key, orders)
        }
    }

    fun addOrder(context: Context, order: CateringOrder)     { orders = orders + order; persist(context) }
    fun updateOrder(context: Context, updated: CateringOrder) { orders = orders.map { if (it.id == updated.id) updated else it }; persist(context) }
    fun deleteOrder(context: Context, id: String)            { orders = orders.filter { it.id != id }; persist(context) }
    fun clearAll(context: Context)                           { orders = emptyList(); persist(context) }

    /**
     * Archives a COPY of the current-catering orders under [dateKey], leaving this workspace
     * untouched so you can keep adding to it. If [dateKey] already has a saved day, the two
     * order lists are combined (nothing is overwritten or dropped) and any customer names that
     * look like likely duplicates are flagged for the caller to offer cleanup on.
     */
    fun saveCurrentCateringToDay(context: Context, dateKey: String): SaveDayResult =
        mergeOrdersIntoDay(context, dateKey, orders)

    /**
     * Combines [incoming] into whatever's saved as Current Catering, regardless of what's
     * currently open. If Current Catering happens to be the active workspace, the in-memory
     * list is updated too so the screen reflects it immediately.
     */
    fun importIntoCurrentCatering(context: Context, incoming: List<CateringOrder>): Pair<Int, List<List<CateringOrder>>> {
        val merged = loadCurrentCateringOrders(context) + incoming
        val groups = findSimilarNameGroups(merged)
        prefs(context).edit().putString("orders", JSONArray(merged.map { it.toJson() }).toString()).apply()
        if (activeDayKey == null) orders = merged
        return merged.size to groups
    }

    /** Removes specific orders (by id) from Current Catering — used to clean up flagged duplicates. */
    fun deleteOrdersFromCurrentCatering(context: Context, idsToRemove: Set<String>) {
        if (idsToRemove.isEmpty()) return
        val remaining = loadCurrentCateringOrders(context).filter { it.id !in idsToRemove }
        prefs(context).edit().putString("orders", JSONArray(remaining.map { it.toJson() }).toString()).apply()
        if (activeDayKey == null) orders = remaining
    }

    // ── Cookie totals ─────────────────────────────────────────────────────────

    fun totalCookiesNeeded(): CookieTotals = cookieTotalsForItems(orders.flatMap { it.items })

    fun totalLoafEquivalents(): Double = totalLoafEquivalents(orders.flatMap { it.items })

    // ── Per-order supply lines (consolidated within each order) ───────────────

    data class OrderSupplyResult(
        val order: CateringOrder,
        val lines: List<String>,
    )

    fun perOrderSupplyLines(): List<OrderSupplyResult> = orders.map { order ->
        OrderSupplyResult(
            order = order,
            lines = buildCondensedSupplyLines(
                items = order.items,
                plateCount = order.plateCount,
                napkinCount = order.napkinCount,
                forksNeeded = order.forksNeeded,
                utensilTally = order.tallyServingUtensils(),
                cateringBoxCount = orderCateringBoxCount(order),
            ) + supplyNoteLinesForOrder(order),
        )
    }

    // ── Aggregated supply list (combined across all orders) ───────────────────

    fun aggregatedSupplyLines(): List<String> {
        val combinedUtensils = orders.fold(ServingUtensilTally()) { acc, order ->
            val t = order.tallyServingUtensils()
            ServingUtensilTally(
                acc.largeTongs + t.largeTongs,
                acc.smallTongs + t.smallTongs,
                acc.spoons + t.spoons,
            )
        }
        return buildCondensedSupplyLines(
            items = orders.flatMap { it.items },
            plateCount = orders.sumOf { it.plateCount },
            napkinCount = orders.sumOf { it.napkinCount },
            forksNeeded = orders.sumOf { it.forksNeeded },
            utensilTally = combinedUtensils,
            cateringBoxCount = totalCateringBoxes(orders),
        ) + orders.flatMap { supplyNoteLinesForOrder(it, prefixCustomer = true) }
    }

    // ── Handoff text ──────────────────────────────────────────────────────────

    fun generateHandoffText(): String = formatHandoffMessage(orders)
}

// ─── Saved days (dated archives, separate from the Current Catering workspace) ─

data class SaveDayResult(
    val dateKey: String,
    val orderCount: Int,
    val similarGroups: List<List<CateringOrder>>,
)

private const val DAY_ORDERS_PREFIX = "day_orders_"

private fun savedDaysPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("catering_saved_days", Context.MODE_PRIVATE)

/** All saved day keys ("yyyy-MM-dd"), oldest first. */
fun listSavedDayKeys(context: Context): List<String> =
    savedDaysPrefs(context).all.keys
        .filter { it.startsWith(DAY_ORDERS_PREFIX) }
        .map { it.removePrefix(DAY_ORDERS_PREFIX) }
        .sorted()

fun loadSavedDay(context: Context, dateKey: String): List<CateringOrder> {
    val raw = savedDaysPrefs(context).getString(DAY_ORDERS_PREFIX + dateKey, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { parseCateringOrder(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveSavedDay(context: Context, dateKey: String, dayOrders: List<CateringOrder>) {
    savedDaysPrefs(context).edit()
        .putString(DAY_ORDERS_PREFIX + dateKey, JSONArray(dayOrders.map { it.toJson() }).toString())
        .apply()
}

/**
 * Combines [incoming] into whatever's already saved under [dateKey] (creating the day if it
 * doesn't exist yet). Used by both "Save Day" (from Current Catering) and "Import Day" (from
 * a pasted export) — either way, nothing already saved under that date is overwritten or lost.
 */
fun mergeOrdersIntoDay(context: Context, dateKey: String, incoming: List<CateringOrder>): SaveDayResult {
    val existing = loadSavedDay(context, dateKey)
    val merged = existing + incoming
    val groups = findSimilarNameGroups(merged)
    saveSavedDay(context, dateKey, merged)
    return SaveDayResult(dateKey, merged.size, groups)
}

fun deleteSavedDay(context: Context, dateKey: String) {
    savedDaysPrefs(context).edit().remove(DAY_ORDERS_PREFIX + dateKey).apply()
}

/** Removes specific orders (by id) from a saved day — used to clean up flagged duplicates. */
fun deleteOrdersFromSavedDay(context: Context, dateKey: String, idsToRemove: Set<String>) {
    if (idsToRemove.isEmpty()) return
    val remaining = loadSavedDay(context, dateKey).filter { it.id !in idsToRemove }
    saveSavedDay(context, dateKey, remaining)
}

// ─── In-progress order draft (so closing the app mid-edit doesn't lose it) ────

private fun draftPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("catering_order_draft", Context.MODE_PRIVATE)

/**
 * [editingId] is the id of the order being edited, or "" for a brand-new order.
 * Continually overwritten as the user types so a killed/closed app can resume.
 */
fun saveDraftOrder(context: Context, editingId: String, order: CateringOrder) {
    draftPrefs(context).edit()
        .putString("draft_editing_id", editingId)
        .putString("draft_order", order.toJson().toString())
        .apply()
}

/** Returns the saved draft only if it matches [editingId]; otherwise null (stale/unrelated draft). */
fun loadDraftOrder(context: Context, editingId: String): CateringOrder? {
    val p = draftPrefs(context)
    if (p.getString("draft_editing_id", null) != editingId) return null
    val raw = p.getString("draft_order", null) ?: return null
    return try { parseCateringOrder(org.json.JSONObject(raw)) } catch (e: Exception) { null }
}

fun clearDraftOrder(context: Context) {
    draftPrefs(context).edit().clear().apply()
}
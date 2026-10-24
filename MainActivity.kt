package com.example.misahorros

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mis_ahorros_plus", MODE_PRIVATE) }
    private val money = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    private val ink = Color.rgb(24, 49, 45)
    private val muted = Color.rgb(104, 123, 117)
    private val green = Color.rgb(35, 105, 82)
    private val lime = Color.rgb(215, 245, 165)
    private val page = Color.rgb(246, 248, 244)
    private lateinit var balance: TextView
    private lateinit var incomeSummary: TextView
    private lateinit var expenseSummary: TextView
    private lateinit var amount: EditText
    private lateinit var concept: EditText
    private lateinit var history: TextView
    private lateinit var movements: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val current = YearMonth.now().toString()
        val previous = prefs.getString("last_month", current) ?: current
        if (previous != current && prefs.contains("balance_cents")) closeMonth(previous, prefs.getLong("balance_cents", 0))
        prefs.edit().putString("last_month", current).apply()
        drawUi()
        refresh()
    }

    private fun drawUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(page) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "€"; textSize = 28f; setTypeface(null, Typeface.BOLD); setTextColor(ink)
            gravity = Gravity.CENTER; background = circle(lime)
        }, LinearLayout.LayoutParams(dp(54), dp(54)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        titles.addView(txt("AUTOAHORRO", 20f, ink, true))
        titles.addView(txt("Tu dinero, bajo control", 13f, muted, false))
        header.addView(titles)
        root.addView(header, params().apply { bottomMargin = dp(20) })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(22))
            background = rounded(green, 24)
        }
        hero.addView(txt("DINERO DISPONIBLE", 12f, 0xFFDCEEE3.toInt(), true))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        balance = txt("0,00 €", 35f, Color.WHITE, true)
        row.addView(balance, LinearLayout.LayoutParams(0, -2, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })
        row.addView(TextView(this).apply {
            text = "€"; textSize = 25f; setTypeface(null, Typeface.BOLD); setTextColor(ink)
            gravity = Gravity.CENTER; background = circle(lime)
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        hero.addView(row)
        hero.addView(txt("Cada movimiento cuenta.", 14f, 0xFFDCEEE3.toInt(), false))
        root.addView(hero, params().apply { topMargin = dp(14) })

        val summary = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val inc = summaryCard("＋ INGRESOS ESTE MES", green)
        incomeSummary = inc.second
        summary.addView(inc.first, LinearLayout.LayoutParams(0, dp(100), 1f).apply { marginEnd = dp(6); topMargin = dp(12) })
        val exp = summaryCard("− GASTOS ESTE MES", 0xFF9F432F.toInt())
        expenseSummary = exp.second
        summary.addView(exp.first, LinearLayout.LayoutParams(0, dp(100), 1f).apply { marginStart = dp(6); topMargin = dp(12) })
        root.addView(summary)

        root.addView(txt("Añadir movimiento", 20f, ink, true), sectionMargin())
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(16)); background = rounded(Color.WHITE, 20)
        }
        amount = edit("Cantidad en euros (ej. 12,50)", true)
        concept = edit("Concepto (paga, compra, transporte)", false)
        form.addView(amount, params())
        form.addView(concept, params().apply { topMargin = dp(6) })
        form.addView(button("＋  Añadir ingreso", green, Color.WHITE) { addMove(true) }, buttonMargin())
        form.addView(button("−  Añadir gasto", 0xFFFFEFE8.toInt(), 0xFF9F432F.toInt()) { addMove(false) }, buttonMargin())
        root.addView(form)

        root.addView(txt("Cierre mensual", 20f, ink, true), sectionMargin())
        val close = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = rounded(Color.WHITE, 20)
        }
        close.addView(txt("Guarda cuánto dinero tienes al terminar el mes.", 14f, muted, false))
        close.addView(button("Guardar cierre de este mes", ink, Color.WHITE) { saveCurrentMonth() }, buttonMargin())
        root.addView(close)

        root.addView(txt("Historial mensual", 20f, ink, true), sectionMargin())
        history = txt("Todavía no hay cierres guardados.", 14f, ink, false)
        history.setPadding(dp(16), dp(14), dp(16), dp(14)); history.background = rounded(Color.WHITE, 18)
        root.addView(history)
        root.addView(txt("Últimos movimientos", 20f, ink, true), sectionMargin())
        movements = txt("Todavía no hay movimientos.", 14f, ink, false)
        movements.setPadding(dp(16), dp(14), dp(16), dp(14)); movements.background = rounded(Color.WHITE, 18)
        root.addView(movements)
        root.addView(txt("Los datos se guardan en este móvil. Registra tus movimientos para mantener el saldo actualizado.", 12f, muted, false)
            .apply { setPadding(dp(4), dp(18), dp(4), 0) })
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun summaryCard(title: String, color: Int): Pair<LinearLayout, TextView> {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(14), dp(8), dp(10)); background = rounded(Color.WHITE, 18)
        }
        box.addView(txt(title, 10f, muted, true))
        val value = txt("0,00 €", 19f, color, true)
        box.addView(value, params().apply { topMargin = dp(12) })
        return Pair(box, value)
    }

    private fun edit(hintText: String, numeric: Boolean) = EditText(this).apply {
        hint = hintText; textSize = 14f; setTextColor(ink); setHintTextColor(muted); setSingleLine(true)
        setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(page, 12)
        if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun button(title: String, bg: Int, fg: Int, action: () -> Unit) = Button(this).apply {
        text = title; textSize = 14f; isAllCaps = false; setTextColor(fg); background = rounded(bg, 14)
        stateListAnimator = null; setOnClickListener { action() }
    }
    private fun txt(s: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun params() = LinearLayout.LayoutParams(-1, -2)
    private fun sectionMargin() = params().apply { topMargin = dp(24); bottomMargin = dp(10) }
    private fun buttonMargin() = params().apply { topMargin = dp(8) }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun circle(color: Int) = GradientDrawable().apply { setColor(color); shape = GradientDrawable.OVAL }

    private fun parseCents(): Long? {
        val raw = amount.text.toString().trim().replace(" ", "").replace(",", ".")
        val n = raw.toBigDecimalOrNull() ?: return null
        if (n <= BigDecimal.ZERO) return null
        return try { n.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact() } catch (_: Exception) { null }
    }

    private fun addMove(isIncome: Boolean) {
        val cents = parseCents()
        if (cents == null) { Toast.makeText(this, "Introduce una cantidad válida mayor que cero.", Toast.LENGTH_LONG).show(); return }
        val old = prefs.getLong("balance_cents", 0L)
        val description = concept.text.toString().trim().ifEmpty { if (isIncome) "Ingreso" else "Gasto" }
        val item = listOf(System.currentTimeMillis().toString(), if (isIncome) "I" else "G", cents.toString(),
            description.replace("|", "/").replace("\n", " ")).joinToString("|")
        val entries = prefs.getStringSet("transactions", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.add(item)
        prefs.edit().putLong("balance_cents", if (isIncome) old + cents else old - cents)
            .putStringSet("transactions", entries).putString("last_month", YearMonth.now().toString()).apply()
        amount.text.clear(); concept.text.clear(); refresh()
        Toast.makeText(this, if (isIncome) "Ingreso añadido" else "Gasto añadido", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentMonth() {
        closeMonth(YearMonth.now().toString(), prefs.getLong("balance_cents", 0L))
        prefs.edit().putString("last_month", YearMonth.now().toString()).apply()
        refresh()
        Toast.makeText(this, "Cierre mensual guardado", Toast.LENGTH_SHORT).show()
    }

    private fun closeMonth(month: String, cents: Long) {
        val entries = prefs.getStringSet("month_history", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.removeAll { it.substringBefore("|") == month }
        entries.add("$month|$cents")
        prefs.edit().putStringSet("month_history", entries).apply()
    }

    private fun refresh() {
        balance.text = money.format(prefs.getLong("balance_cents", 0L) / 100.0)
        val all = prefs.getStringSet("transactions", emptySet())?.mapNotNull { raw ->
            val p = raw.split("|", limit = 4)
            if (p.size != 4) null else try { Move(p[0].toLong(), p[1], p[2].toLong(), p[3]) } catch (_: Exception) { null }
        }?.sortedByDescending { it.time } ?: emptyList()
        var inc = 0L; var exp = 0L
        all.filter { YearMonth.from(LocalDate.ofEpochDay(it.time / 86400000L)) == YearMonth.now() }.forEach {
            if (it.type == "I") inc += it.cents else exp += it.cents
        }
        incomeSummary.text = money.format(inc / 100.0)
        expenseSummary.text = money.format(exp / 100.0)
        val months = prefs.getStringSet("month_history", emptySet())?.mapNotNull {
            val p = it.split("|", limit = 2)
            if (p.size != 2) null else try { YearMonth.parse(p[0]) to p[1].toLong() } catch (_: Exception) { null }
        }?.sortedByDescending { it.first } ?: emptyList()
        history.text = if (months.isEmpty()) "Todavía no hay cierres guardados.\nPulsa el botón de cierre al terminar el mes."
            else months.joinToString("\n\n") { "●  ${it.first.format(monthFormat).replaceFirstChar { c -> c.uppercase() }}\n    ${money.format(it.second / 100.0)}" }
        movements.text = if (all.isEmpty()) "Todavía no has añadido movimientos."
            else all.take(15).joinToString("\n\n") {
                val date = LocalDate.ofEpochDay(it.time / 86400000L).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                "${if (it.type == "I") "＋" else "−"} ${money.format(it.cents / 100.0)} · ${it.description}\n$date"
            }
    }

    private data class Move(val time: Long, val type: String, val cents: Long, val description: String)
}

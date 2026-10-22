package com.example.misahorros

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mis_ahorros_plus", MODE_PRIVATE) }
    private val euros = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    private lateinit var balanceText: TextView
    private lateinit var amountInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var transactionsText: TextView
    private lateinit var historyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleMonthChange()
        buildScreen()
        refresh()
    }

    private fun handleMonthChange() {
        val current = YearMonth.now().toString()
        val previous = prefs.getString("last_month", current) ?: current
        if (previous != current && prefs.contains("balance_cents")) {
            saveMonthClose(previous, prefs.getLong("balance_cents", 0L))
        }
        prefs.edit().putString("last_month", current).apply()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 18)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(TextView(this).apply {
            text = "Mis ahorros"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF17324D.toInt())
            gravity = Gravity.CENTER
        }, params())
        content.addView(TextView(this).apply {
            text = "Tus cuentas claras, mes a mes"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 22)
        }, params())

        content.addView(TextView(this).apply {
            text = "DINERO DISPONIBLE"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, params())
        balanceText = TextView(this).apply {
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF16804A.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 22)
        }
        content.addView(balanceText, params())

        content.addView(sectionTitle("Añadir movimiento"), params())
        amountInput = EditText(this).apply {
            hint = "Cantidad en euros (ej. 12,50)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 16f
            setSingleLine(true)
        }
        content.addView(amountInput, params())
        descriptionInput = EditText(this).apply {
            hint = "Concepto (ej. compra, paga, regalo)"
            textSize = 16f
            setSingleLine(true)
        }
        content.addView(descriptionInput, params())
        content.addView(makeButton("＋ Añadir ingreso") { addMovement(true) }, buttonParams())
        content.addView(makeButton("− Añadir gasto") { addMovement(false) }, buttonParams())

        content.addView(sectionTitle("Cierre mensual"), sectionParams())
        content.addView(TextView(this).apply {
            text = "Guarda el saldo de este mes en tu historial."
            textSize = 14f
        }, params())
        content.addView(makeButton("Guardar cierre de este mes") { closeCurrentMonth() }, buttonParams())

        content.addView(sectionTitle("Historial de cierres"), sectionParams())
        historyText = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF263238.toInt())
            setPadding(0, 6, 0, 8)
        }
        content.addView(historyText, params())

        content.addView(sectionTitle("Últimos movimientos"), sectionParams())
        transactionsText = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF263238.toInt())
            setPadding(0, 6, 0, 16)
        }
        content.addView(transactionsText, params())

        content.addView(TextView(this).apply {
            text = "Los datos se guardan en este móvil. Actualiza el saldo inicial con un ingreso o gasto si quieres ajustar la cantidad."
            textSize = 12f
            setTextColor(0xFF607080.toInt())
            setPadding(0, 18, 0, 8)
        }, params())

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
    private fun buttonParams() = params().apply { topMargin = 5 }
    private fun sectionParams() = params().apply { topMargin = 24; bottomMargin = 4 }

    private fun sectionTitle(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 21f
        setTypeface(null, Typeface.BOLD)
        setTextColor(0xFF17324D.toInt())
    }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 15f
            setOnClickListener { action() }
        }

    private fun parseCents(): Long? {
        val raw = amountInput.text.toString().trim().replace(" ", "").replace(",", ".")
        val value = raw.toBigDecimalOrNull() ?: return null
        if (value <= java.math.BigDecimal.ZERO) return null
        return try { value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }
        catch (_: Exception) { null }
    }

    private fun addMovement(isIncome: Boolean) {
        val cents = parseCents()
        if (cents == null) {
            Toast.makeText(this, "Introduce una cantidad válida mayor que cero.", Toast.LENGTH_LONG).show()
            return
        }
        val oldBalance = prefs.getLong("balance_cents", 0L)
        val newBalance = if (isIncome) oldBalance + cents else oldBalance - cents
        val description = descriptionInput.text.toString().trim().ifEmpty {
            if (isIncome) "Ingreso" else "Gasto"
        }
        val movement = listOf(
            System.currentTimeMillis().toString(),
            if (isIncome) "I" else "G",
            cents.toString(),
            description.replace("|", "/").replace("\n", " ")
        ).joinToString("|")
        val entries = prefs.getStringSet("transactions", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.add(movement)
        prefs.edit()
            .putLong("balance_cents", newBalance)
            .putStringSet("transactions", entries)
            .putString("last_month", YearMonth.now().toString())
            .apply()
        amountInput.text.clear()
        descriptionInput.text.clear()
        refresh()
        Toast.makeText(this, if (isIncome) "Ingreso añadido" else "Gasto añadido", Toast.LENGTH_SHORT).show()
    }

    private fun closeCurrentMonth() {
        val month = YearMonth.now().toString()
        saveMonthClose(month, prefs.getLong("balance_cents", 0L))
        prefs.edit().putString("last_month", month).apply()
        refresh()
        Toast.makeText(this, "Cierre mensual guardado", Toast.LENGTH_SHORT).show()
    }

    private fun saveMonthClose(month: String, cents: Long) {
        val entries = prefs.getStringSet("month_history", emptySet())?.toMutableSet() ?: mutableSetOf()
        entries.removeAll { it.substringBefore("|") == month }
        entries.add("$month|$cents")
        prefs.edit().putStringSet("month_history", entries).apply()
    }

    private fun refresh() {
        val balance = prefs.getLong("balance_cents", 0L)
        balanceText.text = euros.format(balance / 100.0)

        val monthEntries = prefs.getStringSet("month_history", emptySet())?.toList().orEmpty()
            .mapNotNull {
                val parts = it.split("|", limit = 2)
                if (parts.size != 2) null else try {
                    YearMonth.parse(parts[0]) to parts[1].toLong()
                } catch (_: Exception) { null }
            }
            .sortedByDescending { it.first }
        historyText.text = if (monthEntries.isEmpty()) {
            "Todavía no hay cierres. Pulsa el botón de cierre al terminar el mes."
        } else monthEntries.joinToString("\n\n") { (month, cents) ->
            "• ${month.format(monthFormat).replaceFirstChar { it.uppercase() }}\n  ${euros.format(cents / 100.0)}"
        }

        val transactions = prefs.getStringSet("transactions", emptySet())?.toList().orEmpty()
            .mapNotNull { item ->
                val parts = item.split("|", limit = 4)
                if (parts.size < 4) null else try {
                    listOf(parts[0].toLong(), parts[1], parts[2].toLong(), parts[3])
                } catch (_: Exception) { null }
            }
            .sortedByDescending { it[0] as Long }
            .take(15)
        transactionsText.text = if (transactions.isEmpty()) {
            "Todavía no has añadido movimientos."
        } else transactions.joinToString("\n\n") { row ->
            val date = LocalDate.ofEpochDay((row[0] as Long) / 86_400_000L)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val isIncome = row[1] == "I"
            val amount = (row[2] as Long) / 100.0
            "${if (isIncome) "＋" else "−"} ${euros.format(amount)}  ·  ${row[3]}\n$date"
        }
    }
}

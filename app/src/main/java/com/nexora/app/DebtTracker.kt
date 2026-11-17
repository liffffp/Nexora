package com.nexora.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private const val DEBT_KEY = "debt_tracker_records"
private val DebtPurple = Color(0xFF7C3AED)
private val DebtMuted = Color(0xFF64748B)
private val DebtRed = Color(0xFFDC2626)
private val DebtGreen = Color(0xFF15803D)

private data class NexoraDebt(
    val id: Long,
    val person: String,
    val type: String,
    val principal: Long,
    val paid: Long,
    val dueDate: String,
    val note: String,
    val history: List<Long>
) {
    val remaining: Long get() = (principal - paid).coerceAtLeast(0L)
    val settled: Boolean get() = remaining == 0L
}

private fun debtMoney(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")

private fun loadDebtRecords(context: Context): List<NexoraDebt> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString(DEBT_KEY, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val payments = obj.optJSONArray("history") ?: JSONArray()
            NexoraDebt(
                id = obj.getLong("id"),
                person = obj.getString("person"),
                type = obj.getString("type"),
                principal = obj.getLong("principal"),
                paid = obj.optLong("paid", 0L),
                dueDate = obj.getString("dueDate"),
                note = obj.optString("note"),
                history = List(payments.length()) { index -> payments.getLong(index) }
            )
        }.sortedByDescending { it.id }
    } catch (_: Exception) { emptyList() }
}

private fun saveDebtRecords(context: Context, records: List<NexoraDebt>) {
    val array = JSONArray()
    records.forEach { debt ->
        array.put(JSONObject().apply {
            put("id", debt.id)
            put("person", debt.person)
            put("type", debt.type)
            put("principal", debt.principal)
            put("paid", debt.paid)
            put("dueDate", debt.dueDate)
            put("note", debt.note)
            put("history", JSONArray().apply { debt.history.forEach { put(it) } })
        })
    }
    context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .edit().putString(DEBT_KEY, array.toString()).apply()
}

private fun validDebtDate(value: String): Boolean = try {
    SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).apply { isLenient = false }.parse(value) != null
} catch (_: Exception) { false }

@Composable
internal fun DebtTrackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val records = remember { mutableStateListOf<NexoraDebt>().apply { addAll(loadDebtRecords(context)) } }
    var dialog by remember { mutableStateOf("none") }
    var person by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Utang") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf("") }

    val unpaid = records.filterNot { it.settled }
    val debtTotal = unpaid.filter { it.type == "Utang" }.sumOf { it.remaining }
    val receivableTotal = unpaid.filter { it.type == "Piutang" }.sumOf { it.remaining }

    fun persist() = saveDebtRecords(context, records)
    fun openAdd() {
        person = ""; type = "Utang"; amount = ""; dueDate = ""; note = ""; error = ""
        dialog = "add"
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F5FF)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Kembali") }
                Spacer(Modifier.weight(1f))
                Text("NEXORA", color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Debt Tracker", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Kelola utang, piutang, dan pembayaran cicilan.", color = DebtMuted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DebtSummary("Utang belum lunas", debtMoney(debtTotal), DebtRed, Modifier.weight(1f))
                DebtSummary("Piutang belum diterima", debtMoney(receivableTotal), DebtGreen, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Catatan pinjaman", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(onClick = { openAdd() }, shape = RoundedCornerShape(12.dp)) { Text("+ Tambah") }
            }
            Spacer(Modifier.height(10.dp))
            if (records.isEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada catatan utang atau piutang", fontWeight = FontWeight.SemiBold)
                        Text("Tambahkan catatan untuk mulai melacak pembayaran.", color = DebtMuted, fontSize = 12.sp)
                    }
                }
            }
            records.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.person, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(item.type + " • Jatuh tempo " + item.dueDate, color = DebtMuted, fontSize = 12.sp)
                            }
                            Text(if (item.settled) "Lunas" else "Belum lunas", color = if (item.settled) DebtGreen else DebtRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Total: ${debtMoney(item.principal)}", fontSize = 13.sp)
                        Text("Sudah dibayar: ${debtMoney(item.paid)}", color = DebtGreen, fontSize = 12.sp)
                        Text("Sisa: ${debtMoney(item.remaining)}", color = DebtPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (item.principal == 0L) 1f else item.paid.toFloat() / item.principal.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = DebtGreen
                        )
                        if (item.note.isNotBlank()) Text(item.note, color = DebtMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        if (item.history.isNotEmpty()) {
                            Text("Riwayat cicilan: " + item.history.joinToString(" • ") { debtMoney(it) }, color = DebtMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!item.settled) OutlinedButton(onClick = {
                                selectedId = item.id; payment = ""; error = ""; dialog = "payment"
                            }, modifier = Modifier.weight(1f)) { Text("Bayar cicilan") }
                            TextButton(onClick = {
                                records.removeAll { it.id == item.id }; persist()
                            }) { Text("Hapus", color = DebtRed) }
                        }
                    }
                }
            }
            Text("Catatan Debt Tracker disimpan di perangkat dan tidak otomatis mengubah saldo transaksi.", color = DebtMuted, fontSize = 11.sp)
        }
    }

    if (dialog == "add") {
        AlertDialog(
            onDismissRequest = { dialog = "none" },
            title = { Text("Tambah catatan") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(person, { person = it; error = "" }, label = { Text("Nama orang") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Utang", "Piutang").forEach { option ->
                            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
                        }
                    }
                    OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(15); error = "" }, label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(dueDate, { dueDate = it; error = "" }, label = { Text("Jatuh tempo (DD/MM/YYYY)") }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("Catatan (opsional)") }, singleLine = true)
                    if (error.isNotBlank()) Text(error, color = DebtRed, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = amount.toLongOrNull()
                    when {
                        person.isBlank() -> error = "Nama orang wajib diisi."
                        value == null || value <= 0 -> error = "Nominal harus lebih dari Rp0."
                        !validDebtDate(dueDate.trim()) -> error = "Gunakan tanggal DD/MM/YYYY."
                        else -> {
                            records.add(0, NexoraDebt(System.currentTimeMillis(), person.trim(), type, value, 0L, dueDate.trim(), note.trim(), emptyList()))
                            persist(); dialog = "none"
                        }
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { dialog = "none" }) { Text("Batal") } }
        )
    }
    if (dialog == "payment") {
        AlertDialog(
            onDismissRequest = { dialog = "none" },
            title = { Text("Catat pembayaran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(payment, { payment = it.filter(Char::isDigit); error = "" }, label = { Text("Nominal cicilan (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    if (error.isNotBlank()) Text(error, color = DebtRed, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = payment.toLongOrNull()
                    val index = records.indexOfFirst { it.id == selectedId }
                    if (index < 0) { dialog = "none" }
                    else if (value == null || value <= 0L) error = "Masukkan nominal lebih dari Rp0."
                    else if (value > records[index].remaining) error = "Pembayaran melebihi sisa kewajiban."
                    else {
                        val old = records[index]
                        records[index] = old.copy(paid = old.paid + value, history = old.history + value)
                        persist(); dialog = "none"
                    }
                }) { Text("Simpan pembayaran") }
            },
            dismissButton = { TextButton(onClick = { dialog = "none" }) { Text("Batal") } }
        )
    }
}

@Composable
private fun DebtSummary(title: String, value: String, tint: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = DebtMuted, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

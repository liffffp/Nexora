package com.nexora.app

import android.content.Context
import org.json.JSONArray
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class SpendingRecord(
    val type: String,
    val amount: Long,
    val category: String,
    val date: String
)

private val BehaviorPurple = Color(0xFF7C3AED)
private val BehaviorMuted = Color(0xFF64748B)
private val BehaviorRed = Color(0xFFDC2626)
private val BehaviorGreen = Color(0xFF15803D)

private fun behaviorMoney(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")

private fun readSpendingRecords(context: Context): List<SpendingRecord> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString("transactions", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            SpendingRecord(
                type = item.getString("type"),
                amount = item.getLong("amount"),
                category = item.getString("category"),
                date = item.getString("date")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun behaviorDate(value: String): Calendar? {
    return try {
        val date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).apply { isLenient = false }.parse(value)
            ?: return null
        Calendar.getInstance().apply { time = date }
    } catch (_: Exception) { null }
}

@Composable
internal fun SpendingBehaviorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val records = remember { readSpendingRecords(context) }
    val now = Calendar.getInstance()
    var period by remember { mutableStateOf("Bulan ini") }
    val expenses = records.filter { it.type == "Pengeluaran" }
    val filtered = if (period == "Bulan ini") expenses.filter { record ->
        val date = behaviorDate(record.date)
        date != null && date.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    } else expenses
    val total = filtered.sumOf { it.amount }
    val average = if (filtered.isEmpty()) 0L else total / filtered.size
    val categories = filtered.groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    val top = categories.firstOrNull()

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F5FF)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Kembali") }
                Spacer(Modifier.weight(1f))
                Text("NEXORA", color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Text("Spending Behavior", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Kenali pola pengeluaran berdasarkan catatan transaksimu.", color = BehaviorMuted, fontSize = 13.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Bulan ini", "Semua").forEach { option ->
                    FilterChip(selected = period == option, onClick = { period = option }, label = { Text(option) })
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BehaviorPurple)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("TOTAL PENGELUARAN", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(behaviorMoney(total), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Divider(color = Color.White.copy(alpha = 0.25f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Transaksi", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("${filtered.size}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rata-rata per transaksi", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(behaviorMoney(average), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text("Pengeluaran per kategori", fontWeight = FontWeight.Bold, color = Color(0xFF172033), fontSize = 18.sp)
            if (categories.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada data pengeluaran", fontWeight = FontWeight.SemiBold)
                        Text("Catat pengeluaran untuk melihat pola berdasarkan kategori.", color = BehaviorMuted, fontSize = 12.sp)
                    }
                }
            } else {
                categories.forEachIndexed { index, (category, amount) ->
                    val share = if (total <= 0L) 0f else amount.toFloat() / total.toFloat()
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                                Text(behaviorMoney(amount), color = BehaviorPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            LinearProgressIndicator(
                                progress = { share.coerceIn(0f, 1f) },
                                Modifier.fillMaxWidth(),
                                color = if (index == 0) BehaviorPurple else Color(0xFFA78BFA),
                                trackColor = Color(0xFFEDE9FE)
                            )
                            Text("${(share * 100).toInt()}% dari total pengeluaran", color = BehaviorMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Insight sederhana", color = BehaviorPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (top == null) {
                        Text("Tambahkan transaksi pengeluaran agar Nexora dapat menampilkan insight.", color = BehaviorMuted, fontSize = 13.sp)
                    } else {
                        Text("Kategori dengan pengeluaran terbesar adalah ${top.first}, sebesar ${behaviorMoney(top.second)}.", color = Color(0xFF172033), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Ini hanya ringkasan dari catatan yang tersedia, bukan penilaian apakah pengeluaranmu baik atau buruk.", color = BehaviorMuted, fontSize = 11.sp)
                    }
                }
            }
            Text("Analisis dihitung dari transaksi lokal yang tercatat. Data yang belum dimasukkan tidak ikut dihitung.", color = BehaviorMuted, fontSize = 11.sp)
        }
    }
}

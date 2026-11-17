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
import java.util.Locale

private data class TimelineEntry(
    val id: Long,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: String
)

private val TimelinePurple = Color(0xFF7C3AED)
private val TimelineMuted = Color(0xFF64748B)
private val TimelineGreen = Color(0xFF15803D)
private val TimelineRed = Color(0xFFDC2626)

private fun timelineMoney(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")

private fun loadTimelineEntries(context: Context): List<TimelineEntry> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString("transactions", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TimelineEntry(
                id = item.getLong("id"),
                type = item.getString("type"),
                amount = item.getLong("amount"),
                category = item.getString("category"),
                note = item.optString("note"),
                date = item.getString("date")
            )
        }.sortedByDescending { it.id }
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
internal fun FinancialTimelineScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { loadTimelineEntries(context) }
    var filter by remember { mutableStateOf("Semua") }
    val visibleEntries = entries.filter { filter == "Semua" || it.type == filter }
    val income = visibleEntries.filter { it.type == "Pemasukan" }.sumOf { it.amount }
    val expense = visibleEntries.filter { it.type == "Pengeluaran" }.sumOf { it.amount }
    val grouped = visibleEntries.groupBy { it.date }

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
            Text("Financial Timeline", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Perjalanan pemasukan dan pengeluaranmu dalam satu garis waktu.", color = TimelineMuted, fontSize = 13.sp)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = TimelinePurple)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("RINGKASAN TRANSAKSI", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${visibleEntries.size} transaksi", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Pemasukan", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(timelineMoney(income), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pengeluaran", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(timelineMoney(expense), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.25f))
                    Text("Selisih: ${timelineMoney(income - expense)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Text("Filter transaksi", fontWeight = FontWeight.Bold, color = Color(0xFF172033), fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semua", "Pemasukan", "Pengeluaran").forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option, fontSize = 12.sp) }
                    )
                }
            }

            Text("Riwayat aktivitas", fontWeight = FontWeight.Bold, color = Color(0xFF172033), fontSize = 18.sp)
            if (visibleEntries.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada transaksi", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        Text("Transaksi yang kamu catat akan muncul di sini.", color = TimelineMuted, fontSize = 12.sp)
                    }
                }
            } else {
                grouped.forEach { (date, dayEntries) ->
                    Text(date, color = TimelinePurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    dayEntries.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(22.dp)) {
                                Surface(
                                    modifier = Modifier.size(11.dp),
                                    shape = RoundedCornerShape(50),
                                    color = if (entry.type == "Pemasukan") TimelineGreen else TimelineRed
                                ) {}
                                if (index != dayEntries.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(Modifier.width(2.dp).height(62.dp), color = Color(0xFFDDD6FE)) {}
                                }
                            }
                            Card(
                                Modifier.weight(1f).padding(bottom = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.category, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                                        Text(if (entry.note.isBlank()) entry.type else entry.note, color = TimelineMuted, fontSize = 12.sp)
                                        Text(entry.type, color = TimelineMuted, fontSize = 10.sp)
                                    }
                                    Text(
                                        (if (entry.type == "Pemasukan") "+ " else "- ") + timelineMoney(entry.amount),
                                        color = if (entry.type == "Pemasukan") TimelineGreen else TimelineRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text("Riwayat mengikuti tanggal pencatatan transaksi dan tersimpan secara lokal di perangkat.", color = TimelineMuted, fontSize = 11.sp)
        }
    }
}

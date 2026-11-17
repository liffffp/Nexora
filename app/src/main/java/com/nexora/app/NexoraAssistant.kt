package com.nexora.app

import android.content.Context
import org.json.JSONArray
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private data class AssistantEntry(
    val type: String,
    val amount: Long,
    val category: String,
    val note: String
)

private data class AssistantMessage(val question: String, val answer: String, val fromUser: Boolean)

private val AssistantPurple = Color(0xFF7C3AED)
private val AssistantMuted = Color(0xFF64748B)

private fun assistantMoney(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")

private fun assistantLoadEntries(context: Context): List<AssistantEntry> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString("transactions", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            AssistantEntry(
                type = item.optString("type"),
                amount = item.optLong("amount"),
                category = item.optString("category", "Lainnya"),
                note = item.optString("note")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun answerFinanceQuestion(question: String, entries: List<AssistantEntry>): String {
    val q = question.lowercase(Locale("id", "ID"))
    val income = entries.filter { it.type == "Pemasukan" }.sumOf { it.amount }
    val expenseEntries = entries.filter { it.type == "Pengeluaran" }
    val expense = expenseEntries.sumOf { it.amount }
    val balance = income - expense
    val grouped = expenseEntries.groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    val top = grouped.firstOrNull()

    return when {
        q.contains("saldo") || q.contains("sisa uang") ->
            "Berdasarkan transaksi yang tercatat, saldo bersihmu adalah ${assistantMoney(balance)}. Ini dihitung dari total pemasukan dikurangi total pengeluaran."
        q.contains("pemasukan") || q.contains("pendapatan") ->
            "Total pemasukan yang tercatat adalah ${assistantMoney(income)} dari ${entries.count { it.type == "Pemasukan" }} transaksi."
        q.contains("kategori") || q.contains("terbesar") || q.contains("paling banyak") ->
            if (top == null) "Belum ada catatan pengeluaran untuk dibandingkan."
            else "Kategori pengeluaran terbesar adalah ${top.first}, dengan total ${assistantMoney(top.second)}."
        q.contains("rata-rata") || q.contains("rata rata") || q.contains("rerata") ->
            if (expenseEntries.isEmpty()) "Belum ada transaksi pengeluaran untuk menghitung rata-rata."
            else "Rata-rata nominal per transaksi pengeluaran adalah ${assistantMoney(expense / expenseEntries.size)}."
        q.contains("pengeluaran") || q.contains("belanja") ->
            "Total pengeluaran yang tercatat adalah ${assistantMoney(expense)} dari ${expenseEntries.size} transaksi."
        q.contains("hemat") || q.contains("saran") || q.contains("tips") ->
            if (top == null) "Mulailah mencatat pengeluaran. Setelah ada data, aku bisa menunjukkan kategori dengan nilai terbesar untuk kamu tinjau."
            else "Kamu bisa meninjau kembali catatan kategori ${top.first} yang berjumlah ${assistantMoney(top.second)}. Pilih sendiri apakah ada pengeluaran yang bisa disesuaikan; kebutuhan penting tetap diprioritaskan."
        q.contains("ringkasan") || q.contains("keuangan") ->
            "Ringkasan catatanmu: pemasukan ${assistantMoney(income)}, pengeluaran ${assistantMoney(expense)}, dan selisih ${assistantMoney(balance)}."
        else ->
            "Aku bisa membantu membaca ringkasan transaksi. Coba tanyakan saldo, total pemasukan, total pengeluaran, kategori terbesar, rata-rata pengeluaran, atau ringkasan keuangan."
    }
}

@Composable
internal fun NexoraAssistantScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { assistantLoadEntries(context) }
    var question by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            AssistantMessage(
                "",
                "Halo! Aku Nexora Assistant. Aku bisa membantu memahami catatan pemasukan dan pengeluaran yang tersimpan di perangkat ini.",
                false
            )
        )
    }

    fun ask(value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        messages.add(AssistantMessage(clean, answerFinanceQuestion(clean, entries), true))
        question = ""
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F5FF)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Kembali") }
                Spacer(Modifier.weight(1f))
                Text("NEXORA", color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Text("Nexora Assistant", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Asisten ringkasan keuangan lokal", color = AssistantMuted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                messages.forEach { message ->
                    if (message.fromUser) {
                        Card(
                            Modifier.fillMaxWidth(0.88f).align(Alignment.End),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = AssistantPurple)
                        ) {
                            Text(message.question, Modifier.padding(14.dp), color = Color.White, fontSize = 14.sp)
                        }
                    } else {
                        Card(
                            Modifier.fillMaxWidth(0.94f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("NEXORA ASSISTANT", color = AssistantPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(message.answer, color = Color(0xFF172033), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Text("Pertanyaan cepat", color = AssistantMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Saldo saya", "Pengeluaran", "Kategori terbesar").forEach { suggestion ->
                    SuggestionChip(onClick = { ask(suggestion) }, label = { Text(suggestion, fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pertanyaan...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AssistantPurple, cursorColor = AssistantPurple)
                )
                Button(
                    onClick = { ask(question) },
                    enabled = question.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
                ) { Text("Kirim") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Jawaban dihitung dari transaksi lokal yang tercatat. Ini bukan AI online dan tidak terhubung ke rekening atau GoPay.",
                color = AssistantMuted,
                fontSize = 10.sp
            )
        }
    }
}

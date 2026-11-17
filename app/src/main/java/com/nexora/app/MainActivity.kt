package com.nexora.app

import android.content.Context
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NexoraPurple = Color(0xFF7C3AED)
private val NexoraPurpleDark = Color(0xFF4C1D95)
private val PageBackground = Color(0xFFF7F5FF)
private val MutedText = Color(0xFF64748B)
private val IncomeGreen = Color(0xFF15803D)
private val ExpenseRed = Color(0xFFDC2626)
private const val PREFS_NAME = "nexora_finance"
private const val TRANSACTIONS_KEY = "transactions"

private data class FinanceEntry(
    val id: Long,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: String
)

private fun formatRupiah(amount: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace(",00", "")

private fun loadEntries(context: Context): List<FinanceEntry> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(TRANSACTIONS_KEY, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            FinanceEntry(
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

private fun saveEntries(context: Context, entries: List<FinanceEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(JSONObject().apply {
            put("id", entry.id)
            put("type", entry.type)
            put("amount", entry.amount)
            put("category", entry.category)
            put("note", entry.note)
            put("date", entry.date)
        })
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(TRANSACTIONS_KEY, array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = NexoraPurple,
                    onPrimary = Color.White,
                    secondary = Color(0xFFA855F7),
                    background = PageBackground,
                    surface = Color.White
                )
            ) {
                NexoraDashboard()
            }
        }
    }
}

@Composable
private fun NexoraDashboard() {
    val context = LocalContext.current
    val entries = remember {
        mutableStateListOf<FinanceEntry>().apply { addAll(loadEntries(context)) }
    }
    val dialogType = remember { mutableStateOf<String?>(null) }
    val showResetDialog = remember { mutableStateOf(false) }
    val showPockets = remember { mutableStateOf(false) }
    if (showPockets.value) {
        MoneyPocketsScreen(onBack = { showPockets.value = false })
        return
    }
    val totalIncome = entries.filter { it.type == "Pemasukan" }.sumOf { it.amount }
    val totalExpense = entries.filter { it.type == "Pengeluaran" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_nexora_mark),
                    contentDescription = "Logo Nexora",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "NEXORA",
                    color = NexoraPurpleDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showResetDialog.value = true }) {
                    Text("Reset data", color = ExpenseRed, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Ringkasan keuangan", color = Color(0xFF172033), fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Catat dan pantau keuanganmu dengan mudah.", color = MutedText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NexoraPurple)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text("SALDO SAAT INI", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatRupiah(balance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Saldo dihitung otomatis dari seluruh pemasukan dan pengeluaran.",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Pemasukan", formatRupiah(totalIncome), Modifier.weight(1f), IncomeGreen)
                SummaryCard("Pengeluaran", formatRupiah(totalExpense), Modifier.weight(1f), ExpenseRed)
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(value = "Money Pockets", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { TextButton(onClick = { showPockets.value = true }) { Text("Buka") } }, label = { Text("Kelola saldo GoPay, bank, dan tunai") })
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { dialogType.value = "Pemasukan" },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("+ Pemasukan", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { dialogType.value = "Pengeluaran" },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurple),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("+ Pengeluaran", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Aktivitas terbaru", color = Color(0xFF172033), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (entries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Belum ada catatan", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pemasukan dan pengeluaran yang kamu tambahkan akan muncul di sini.",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                entries.take(30).forEach { entry ->
                    EntryCard(entry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    dialogType.value?.let { type ->
        EntryDialog(
            type = type,
            onDismiss = { dialogType.value = null },
            onSave = { amount, category, note ->
                val date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
                val entry = FinanceEntry(System.currentTimeMillis(), type, amount, category, note, date)
                entries.add(0, entry)
                saveEntries(context, entries)
                dialogType.value = null
            }
        )
    }

    if (showResetDialog.value) {
        AlertDialog(
            onDismissRequest = { showResetDialog.value = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Reset seluruh data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Semua catatan pemasukan dan pengeluaran di perangkat ini akan dihapus. Tindakan ini tidak bisa dibatalkan.")
            },
            confirmButton = {
                TextButton(onClick = {
                    entries.clear()
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().remove(TRANSACTIONS_KEY).apply()
                    showResetDialog.value = false
                }) { Text("Ya, hapus semua", color = ExpenseRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog.value = false }) { Text("Batal", color = NexoraPurple) }
            }
        )
    }
}

@Composable
private fun EntryDialog(
    type: String,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit
) {
    val isIncome = type == "Pemasukan"
    val accent = if (isIncome) IncomeGreen else ExpenseRed
    val categories = if (isIncome) {
        listOf("Gaji", "Bonus", "Hadiah", "Usaha", "Lainnya")
    } else {
        listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Pendidikan", "Lainnya")
    }
    val amount = remember { mutableStateOf("") }
    val category = remember { mutableStateOf(categories.first()) }
    val note = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val parsedAmount = amount.value.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text(if (isIncome) "Catat pemasukan" else "Catat pengeluaran", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isIncome) "Tambahkan uang yang kamu terima." else "Catat uang yang kamu keluarkan.",
                    color = MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("NOMINAL", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        OutlinedTextField(
                            value = amount.value,
                            onValueChange = { value ->
                                amount.value = value.filter { it.isDigit() }.take(15)
                                error.value = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0", fontSize = 24.sp) },
                            prefix = { Text("Rp", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        if (parsedAmount > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatRupiah(parsedAmount), color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                Text("Pilih kategori", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.chunked(3).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            group.forEach { item ->
                                val selected = category.value == item
                                TextButton(
                                    onClick = { category.value = item },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (selected) accent.copy(alpha = 0.12f) else PageBackground,
                                        contentColor = if (selected) accent else MutedText
                                    )
                                ) {
                                    Text(item, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                            repeat(3 - group.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }

                OutlinedTextField(
                    value = note.value,
                    onValueChange = { note.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan (opsional)") },
                    placeholder = { Text("Contoh: makan siang") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                if (error.value.isNotEmpty()) {
                    Text(error.value, color = ExpenseRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.value.toLongOrNull()
                    if (value == null || value <= 0L) {
                        error.value = "Masukkan nominal lebih dari Rp0."
                    } else {
                        onSave(value, category.value, note.value.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Simpan catatan", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedText) }
        }
    )
}

@Composable
private fun EntryCard(entry: FinanceEntry) {
    val isIncome = entry.type == "Pemasukan"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                Text(if (entry.note.isBlank()) entry.type else entry.note, color = MutedText, fontSize = 12.sp)
                Text(entry.date, color = MutedText, fontSize = 11.sp)
            }
            Text(
                (if (isIncome) "+ " else "- ") + formatRupiah(entry.amount),
                color = if (isIncome) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MutedText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(amount, color = amountColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

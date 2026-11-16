package com.nexora.app

import android.content.Context
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private const val POCKETS_KEY = "money_pockets"

private fun formatPocketRupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value).replace(",00", "")

private data class MoneyPocket(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Long
)

private fun loadPockets(context: Context): List<MoneyPocket> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString(POCKETS_KEY, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { i ->
            val item = array.getJSONObject(i)
            MoneyPocket(item.getLong("id"), item.getString("name"), item.getString("type"), item.getLong("balance"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun savePockets(context: Context, pockets: List<MoneyPocket>) {
    val array = JSONArray()
    pockets.forEach { p ->
        array.put(JSONObject().apply {
            put("id", p.id); put("name", p.name); put("type", p.type); put("balance", p.balance)
        })
    }
    context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .edit().putString(POCKETS_KEY, array.toString()).apply()
}

@Composable
internal fun MoneyPocketsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pockets = remember { mutableStateListOf<MoneyPocket>().apply { addAll(loadPockets(context)) } }
    var dialog by remember { mutableStateOf("none") }
    var selectedId by remember { mutableLongStateOf(-1L) }
    var amount by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Dompet digital") }
    var error by remember { mutableStateOf("") }
    val total = pockets.sumOf { it.balance }

    fun persist() = savePockets(context, pockets)
    fun openAdjust(p: MoneyPocket) {
        selectedId = p.id; amount = ""; dialog = "adjust"; error = ""
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F5FF)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Kembali") }
                Spacer(Modifier.weight(1f))
                Text("NEXORA", color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Money Pockets", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Pisahkan saldo berdasarkan tempat penyimpanan uang.", color = Color(0xFF64748B), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF7C3AED))) {
                Column(Modifier.padding(22.dp)) {
                    Text("TOTAL SELURUH POCKET", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(formatPocketRupiah(total), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Text("${pockets.size} tempat penyimpanan", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pocket saya", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                Button(onClick = { name = ""; type = "Dompet digital"; amount = ""; dialog = "add"; error = "" }, shape = RoundedCornerShape(12.dp)) { Text("+ Tambah") }
            }
            Spacer(Modifier.height(10.dp))
            if (pockets.isEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada pocket", fontWeight = FontWeight.SemiBold)
                        Text("Tambahkan GoPay, rekening bank, atau uang tunai.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            }
            pockets.forEach { p ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF172033))
                                Text(p.type, color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                            Text(formatPocketRupiah(p.balance), fontWeight = FontWeight.Bold, color = Color(0xFF4C1D95), fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { openAdjust(p) }, modifier = Modifier.weight(1f)) { Text("Ubah saldo") }
                            OutlinedButton(onClick = {
                                name = p.name; type = p.type; selectedId = p.id; dialog = "edit"; error = ""
                            }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            TextButton(onClick = { pockets.removeAll { it.id == p.id }; persist() }) { Text("Hapus", color = Color(0xFFDC2626)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Catatan: saldo di sini dicatat manual oleh kamu. Nexora belum terhubung langsung ke akun GoPay atau bank.", color = Color(0xFF64748B), fontSize = 12.sp)
        }
    }

    if (dialog != "none") {
        val editing = dialog == "edit"
        val adjusting = dialog == "adjust"
        AlertDialog(
            onDismissRequest = { dialog = "none" },
            title = { Text(when (dialog) { "add" -> "Tambah pocket"; "edit" -> "Edit pocket"; else -> "Sesuaikan saldo" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!adjusting) {
                        OutlinedTextField(name, { name = it; error = "" }, label = { Text("Nama pocket") }, singleLine = true)
                        Text("Jenis pocket", fontSize = 12.sp, color = Color(0xFF64748B))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Dompet digital", "Rekening bank", "Tunai", "Tabungan").forEach { option ->
                                FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option, fontSize = 10.sp) })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit); error = "" },
                        label = { Text(if (adjusting) "Saldo baru (Rp)" else "Saldo awal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (error.isNotBlank()) Text(error, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toLongOrNull()
                    if (adjusting) {
                        if (value == null) error = "Masukkan saldo yang valid."
                        else {
                            val index = pockets.indexOfFirst { it.id == selectedId }
                            if (index >= 0) pockets[index] = pockets[index].copy(balance = value)
                            persist(); dialog = "none"
                        }
                    } else if (name.isBlank()) error = "Nama pocket wajib diisi."
                    else if (value == null) error = "Masukkan saldo awal yang valid."
                    else {
                        if (editing) {
                            val index = pockets.indexOfFirst { it.id == selectedId }
                            if (index >= 0) pockets[index] = pockets[index].copy(name = name.trim(), type = type, balance = value)
                        } else pockets.add(MoneyPocket(System.currentTimeMillis(), name.trim(), type, value))
                        persist(); dialog = "none"
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { dialog = "none" }) { Text("Batal") } }
        )
    }
}

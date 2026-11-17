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
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import java.util.Date
import java.util.Locale

private const val SUBSCRIPTIONS_KEY = "subscriptions"
private val SubscriptionPurple = Color(0xFF7C3AED)
private val SubscriptionMuted = Color(0xFF64748B)
private val SubscriptionRed = Color(0xFFDC2626)
private val SubscriptionGreen = Color(0xFF15803D)

private data class NexoraSubscription(
    val id: Long,
    val name: String,
    val category: String,
    val amount: Long,
    val cycle: String,
    val nextDate: String,
    val active: Boolean
)

private fun loadSubscriptions(context: Context): List<NexoraSubscription> {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString(SUBSCRIPTIONS_KEY, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            NexoraSubscription(
                id = item.getLong("id"),
                name = item.getString("name"),
                category = item.getString("category"),
                amount = item.getLong("amount"),
                cycle = item.getString("cycle"),
                nextDate = item.getString("nextDate"),
                active = item.optBoolean("active", true)
            )
        }.sortedBy { it.nextDate }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveSubscriptions(context: Context, subscriptions: List<NexoraSubscription>) {
    val array = JSONArray()
    subscriptions.forEach { item ->
        array.put(JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("category", item.category)
            put("amount", item.amount)
            put("cycle", item.cycle)
            put("nextDate", item.nextDate)
            put("active", item.active)
        })
    }
    context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .edit().putString(SUBSCRIPTIONS_KEY, array.toString()).apply()
}

private fun subscriptionRupiah(amount: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace(",00", "")

private fun monthlySubscriptionCost(item: NexoraSubscription): Long = when (item.cycle) {
    "Mingguan" -> (item.amount * 52L) / 12L
    "Tahunan" -> item.amount / 12L
    else -> item.amount
}

private fun daysUntilSubscription(dateText: String): Long? {
    return try {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).apply { isLenient = false }
        val target = format.parse(dateText) ?: return null
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        (target.time - today.time) / 86_400_000L
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun SubscriptionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val subscriptions = remember {
        mutableStateListOf<NexoraSubscription>().apply { addAll(loadSubscriptions(context)) }
    }
    var dialogMode by remember { mutableStateOf("none") }
    var editingId by remember { mutableStateOf(0L) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Streaming") }
    var amount by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("Bulanan") }
    var nextDate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val activeItems = subscriptions.filter { it.active }
    val monthlyTotal = activeItems.sumOf { monthlySubscriptionCost(it) }
    val annualEstimate = monthlyTotal * 12L

    fun persist() = saveSubscriptions(context, subscriptions)
    fun startAdd() {
        editingId = 0L
        name = ""
        category = "Streaming"
        amount = ""
        cycle = "Bulanan"
        nextDate = ""
        error = ""
        dialogMode = "add"
    }
    fun startEdit(item: NexoraSubscription) {
        editingId = item.id
        name = item.name
        category = item.category
        amount = item.amount.toString()
        cycle = item.cycle
        nextDate = item.nextDate
        error = ""
        dialogMode = "edit"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F5FF)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Kembali") }
                Spacer(Modifier.weight(1f))
                Text("NEXORA", color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Subscription Manager", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Pantau biaya langganan dan jadwal pembayaran rutin.", color = SubscriptionMuted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SubscriptionPurple)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("ESTIMASI BIAYA LANGGANAN / BULAN", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(subscriptionRupiah(monthlyTotal), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${activeItems.size} langganan aktif", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                        Text("Tahunan: ${subscriptionRupiah(annualEstimate)}", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Daftar langganan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                Spacer(Modifier.weight(1f))
                Button(onClick = { startAdd() }, shape = RoundedCornerShape(12.dp)) { Text("+ Tambah") }
            }
            Spacer(Modifier.height(10.dp))

            if (subscriptions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Belum ada langganan", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        Spacer(Modifier.height(5.dp))
                        Text("Tambahkan layanan seperti streaming, musik, atau penyimpanan cloud.", color = SubscriptionMuted, fontSize = 12.sp)
                    }
                }
            }

            subscriptions.sortedBy { it.nextDate }.forEach { item ->
                val remaining = daysUntilSubscription(item.nextDate)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF172033))
                                Text(item.category + " • " + item.cycle, color = SubscriptionMuted, fontSize = 12.sp)
                            }
                            Text(
                                subscriptionRupiah(item.amount),
                                color = SubscriptionPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Berikutnya: ${item.nextDate}", color = SubscriptionMuted, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                when {
                                    !item.active -> "Dihentikan"
                                    remaining == null -> "Periksa tanggal"
                                    remaining < 0 -> "Terlambat"
                                    remaining == 0L -> "Jatuh tempo hari ini"
                                    remaining <= 7 -> "${remaining} hari lagi"
                                    else -> "Aktif"
                                },
                                color = when {
                                    !item.active -> SubscriptionMuted
                                    remaining != null && remaining <= 7 -> SubscriptionRed
                                    else -> SubscriptionGreen
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedButton(onClick = { startEdit(item) }, modifier = Modifier.weight(1f)) {
                                Text("Edit", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val index = subscriptions.indexOfFirst { it.id == item.id }
                                    if (index >= 0) {
                                        subscriptions[index] = subscriptions[index].copy(active = !item.active)
                                        persist()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (item.active) "Hentikan" else "Aktifkan", fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                subscriptions.removeAll { it.id == item.id }
                                persist()
                            }) { Text("Hapus", color = SubscriptionRed, fontSize = 12.sp) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Pengingat saat ini ditampilkan di dalam aplikasi. Nexora belum mengirim notifikasi otomatis.",
                color = SubscriptionMuted,
                fontSize = 12.sp
            )
        }
    }

    if (dialogMode != "none") {
        AlertDialog(
            onDismissRequest = { dialogMode = "none" },
            title = { Text(if (dialogMode == "add") "Tambah langganan" else "Edit langganan") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = "" },
                        label = { Text("Nama layanan") },
                        placeholder = { Text("Contoh: Netflix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kategori", fontSize = 12.sp, color = SubscriptionMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Streaming", "Musik", "Aplikasi", "Cloud", "Lainnya").forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit).take(15); error = "" },
                        label = { Text("Biaya (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Siklus pembayaran", fontSize = 12.sp, color = SubscriptionMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mingguan", "Bulanan", "Tahunan").forEach { option ->
                            FilterChip(
                                selected = cycle == option,
                                onClick = { cycle = option },
                                label = { Text(option, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = nextDate,
                        onValueChange = { nextDate = it; error = "" },
                        label = { Text("Pembayaran berikutnya") },
                        placeholder = { Text("DD/MM/YYYY") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error.isNotBlank()) Text(error, color = SubscriptionRed, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = amount.toLongOrNull()
                    val dateValid = try {
                        val format = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).apply { isLenient = false }
                        format.parse(nextDate.trim()) != null
                    } catch (_: Exception) { false }
                    when {
                        name.isBlank() -> error = "Nama layanan wajib diisi."
                        value == null || value <= 0L -> error = "Masukkan biaya lebih dari Rp0."
                        !dateValid -> error = "Gunakan format tanggal DD/MM/YYYY."
                        else -> {
                            val updated = NexoraSubscription(
                                id = if (dialogMode == "edit") editingId else System.currentTimeMillis(),
                                name = name.trim(),
                                category = category,
                                amount = value,
                                cycle = cycle,
                                nextDate = nextDate.trim(),
                                active = subscriptions.firstOrNull { it.id == editingId }?.active ?: true
                            )
                            if (dialogMode == "edit") {
                                val index = subscriptions.indexOfFirst { it.id == editingId }
                                if (index >= 0) subscriptions[index] = updated
                            } else subscriptions.add(updated)
                            persist()
                            dialogMode = "none"
                        }
                    }
                }, shape = RoundedCornerShape(12.dp)) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { dialogMode = "none" }) { Text("Batal", color = SubscriptionMuted) }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White
        )
    }
}

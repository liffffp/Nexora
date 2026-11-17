package com.nexora.app

import android.content.Context
import org.json.JSONArray
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrivacyPurple = Color(0xFF7C3AED)
private val PrivacyMuted = Color(0xFF64748B)

private fun storedItemCount(context: Context, key: String): Int {
    val raw = context.getSharedPreferences("nexora_finance", Context.MODE_PRIVATE)
        .getString(key, "[]") ?: "[]"
    return try { JSONArray(raw).length() } catch (_: Exception) { 0 }
}

@Composable
internal fun PrivacyDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val counts = remember {
        listOf(
            "Transaksi" to storedItemCount(context, "transactions"),
            "Money Pockets" to storedItemCount(context, "money_pockets"),
            "Langganan" to storedItemCount(context, "subscriptions"),
            "Catatan utang/piutang" to storedItemCount(context, "debt_tracker_records")
        )
    }

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
            Text("Privacy Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            Text("Kenali cara Nexora menyimpan dan mengelola data.", color = PrivacyMuted, fontSize = 13.sp)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PENYIMPANAN DATA", color = PrivacyPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Data tersimpan secara lokal", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Fitur keuangan saat ini menggunakan penyimpanan lokal aplikasi di perangkat ini (SharedPreferences). Data tidak otomatis disinkronkan ke akun bank atau GoPay.", color = PrivacyMuted, fontSize = 13.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RINGKASAN DATA", fontSize = 11.sp, color = PrivacyPurple, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    counts.forEach { (label, count) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f), color = Color(0xFF334155), fontSize = 13.sp)
                            Text("$count item", color = Color(0xFF172033), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Text("Jumlah dihitung dari catatan yang tersimpan saat layar dibuka.", color = PrivacyMuted, fontSize = 11.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IZIN APLIKASI", fontSize = 11.sp, color = PrivacyPurple, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Tidak ada izin khusus yang diminta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Manifest Nexora saat ini tidak mendeklarasikan izin kamera, lokasi, kontak, mikrofon, atau penyimpanan eksternal.", color = PrivacyMuted, fontSize = 13.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KONTROL PRIVASI", fontSize = 11.sp, color = PrivacyPurple, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Cadangan data aplikasi dinonaktifkan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Android Auto Backup disetel nonaktif pada konfigurasi aplikasi. Ini membantu mencegah data aplikasi ikut dicadangkan melalui fitur tersebut.", color = PrivacyMuted, fontSize = 13.sp)
                    Divider(color = Color(0xFFE2E8F0))
                    Text("Kelola atau hapus data", fontWeight = FontWeight.SemiBold, color = Color(0xFF172033))
                    Text("Gunakan tombol Reset data di halaman utama untuk menghapus catatan transaksi. Data Money Pockets, langganan, dan utang/piutang dikelola dari halaman fiturnya masing-masing.", color = PrivacyMuted, fontSize = 13.sp)
                    Text("Reset transaksi tidak menghapus data dari fitur lainnya.", color = Color(0xFFB45309), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Catatan: layar ini menampilkan konfigurasi aplikasi saat ini, bukan audit keamanan menyeluruh. Data lokal tetap dapat terhapus jika data aplikasi dihapus atau aplikasi di-uninstall.", color = PrivacyMuted, fontSize = 11.sp)
        }
    }
}

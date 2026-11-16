package com.nexora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NexoraPurple = Color(0xFF7C3AED)
private val NexoraPurpleDark = Color(0xFF4C1D95)
private val PageBackground = Color(0xFFF7F5FF)
private val MutedText = Color(0xFF64748B)

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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PageBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_nexora_mark),
                    contentDescription = "Logo Nexora",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEXORA",
                    color = NexoraPurpleDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ringkasan keuangan",
                color = Color(0xFF172033),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Catat dan pantau keuanganmu dengan mudah.",
                color = MutedText,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NexoraPurple)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text("SALDO SAAT INI", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Rp0",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Saldo akan dihitung dari pemasukan dan pengeluaran yang kamu catat.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Pemasukan",
                    amount = "Rp0",
                    modifier = Modifier.weight(1f),
                    amountColor = Color(0xFF15803D)
                )
                SummaryCard(
                    title = "Pengeluaran",
                    amount = "Rp0",
                    modifier = Modifier.weight(1f),
                    amountColor = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Aktivitas terbaru",
                color = Color(0xFF172033),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Belum ada catatan", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pemasukan dan pengeluaran yang kamu tambahkan akan muncul di sini.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
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
            Text(amount, color = amountColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

package com.tippool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippool.ui.screens.CateringScreen
import com.tippool.ui.screens.TipPoolScreen
import com.tippool.ui.theme.*

class MainActivity : ComponentActivity() {
    private val tipVm:     TipPoolViewModel  by viewModels()
    private val cateringVm: CateringViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        requestNotificationPermission()
        setContent {
            TipPoolTheme {
                var selectedTab by remember { mutableStateOf(0) }

                // Intercept back press — don't let it accidentally switch tabs
                BackHandler(enabled = false) {}

                Column(modifier = Modifier.fillMaxSize().background(Background)) {
                    // Content
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> TipPoolScreen(tipVm)
                            1 -> CateringScreen(cateringVm)
                        }
                    }
                    // Bottom nav
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(0.dp))
                            .navigationBarsPadding(),
                    ) {
                        listOf("💰" to "Tips", "📋" to "Catering").forEachIndexed { index, (icon, label) ->
                            val selected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(icon, fontSize = 20.sp)
                                    Text(
                                        label,
                                        color = if (selected) Orange else TextDim,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp,
                                    )
                                    // Active indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 4.dp else 0.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Orange)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Shift Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Closing shift checklist reminders"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object { const val CHANNEL_ID = "shift_reminders" }
}
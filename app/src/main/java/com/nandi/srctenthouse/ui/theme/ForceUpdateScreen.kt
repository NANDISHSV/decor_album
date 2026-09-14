package com.nandi.srctenthouse.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)

@Composable
fun ForceUpdateScreen(message: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().background(InkBackground).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Update Required", color = IvoryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = IvoryText, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.nandi.srctenthouse")
                    )
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MarigoldGold, contentColor = InkBackground)
            ) {
                Text("Update Now")
            }
        }
    }
}
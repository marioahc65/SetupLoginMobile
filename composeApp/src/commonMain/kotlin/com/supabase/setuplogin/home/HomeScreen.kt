package com.supabase.setuplogin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.time.Clock

private data class BottomTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val emphasis: Boolean = false
)

@Composable
fun HomeScreen(
    email: String,
    loading: Boolean,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit,
    accessToken: String?,
    tokenExpiryEpoch: Long?,
    inactivityRemainingSeconds: Long?,
    onUserActivity: () -> Unit
) {
    val tabs = remember {
        listOf(
            BottomTab("Inicio", Icons.Filled.Home),
            BottomTab("Lista", Icons.AutoMirrored.Filled.List),
            BottomTab("Acción", Icons.Filled.AddCircle, emphasis = true),
            BottomTab("Alertas", Icons.Filled.Notifications),
            BottomTab("Perfil", Icons.Filled.Person)
        )
    }
    var selected by remember { mutableStateOf(tabs.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D0E))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bienvenido", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(email, color = Color.White.copy(alpha = 0.8f))
            accessToken?.let {
                val signature = it.substringAfterLast(".")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Token (firma):", color = Color.White.copy(alpha = 0.7f))
                Text(signature, color = Color.White.copy(alpha = 0.8f))
            }
            inactivityRemainingSeconds?.let { remaining ->
                Spacer(modifier = Modifier.height(8.dp))
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    "Cierra por inactividad en: %d:%02d".format(minutes, seconds),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vista: ${selected.title}", color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Text(if (loading) "Actualizando..." else "Refrescar", color = Color.White)
                }
                TextButton(onClick = onSignOut, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Text("Cerrar sesión", color = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val active = tab == selected
                    val iconSize = if (tab.emphasis) 36.dp else 24.dp
                    val textAlpha = if (active) 1f else 0.6f
                    TextButton(onClick = {
                        selected = tab
                        onUserActivity()
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(iconSize),
                                tint = if (active) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                tab.title,
                                color = Color.White.copy(alpha = textAlpha),
                                fontWeight = if (tab.emphasis) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

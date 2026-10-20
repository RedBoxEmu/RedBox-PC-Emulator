package com.rimvydop.redboxpcemulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val nextRed = Color(0xFFFF4054)

@Composable
fun RedBoxNextHome(
    modifier: Modifier = Modifier,
    createdVM: VMModel?,
    isRunning: Boolean,
    qemuStatus: String,
    onCreateVM: () -> Unit,
    onVMClick: () -> Unit,
    onVmsClick: () -> Unit,
    onQuickAccess: (String) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.redbox_logo),
                contentDescription = "RedBox logo",
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("REDBOX", fontSize = 19.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp)
                Text("PC EMULATOR  /  NEXT", fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.1.sp)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(30.dp)) {
                Text("v1.0", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(32.dp))
        AnimatedVisibility(entered, enter = fadeIn(tween(550)) +
            slideInVertically(tween(550)) { it / 3 }) {
            Column {
                Text("YOUR WORKSPACE", color = nextRed, fontSize = 11.sp,
                    letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
                Text("Power up.", fontSize = 42.sp, fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp, color = textColor)
                Text("Your computers, all in one place.", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(26.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp),
            color = Color(0xFF19131C)) {
            Box(Modifier.background(Brush.linearGradient(listOf(
                Color(0xFF45202C), Color(0xFF19131C), Color(0xFF11141B)
            )))) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Computer, null, tint = Color(0xFFFFA4AE),
                            modifier = Modifier.size(32.dp))
                        Spacer(Modifier.weight(1f))
                        Surface(color = Color(0xFF5B2937), shape = RoundedCornerShape(20.dp)) {
                            Text(if (isRunning) "● RUNNING" else "● STANDBY",
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC2C9))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("FEATURED MACHINE", fontSize = 10.sp, letterSpacing = 2.sp,
                        color = Color(0xFFCFADB5))
                    Spacer(Modifier.height(7.dp))
                    Text(createdVM?.name ?: "No machine yet", fontSize = 26.sp,
                        fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(if (createdVM == null) "Create your first virtual PC to begin."
                        else if (isRunning) "Your virtual machine is active."
                        else "Your next session is ready.", fontSize = 13.sp,
                        color = Color(0xFFD8C4C9))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = if (createdVM == null) onCreateVM else onVMClick,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = nextRed)) {
                        Icon(if (createdVM == null) Icons.Filled.Add else Icons.Filled.PlayArrow,
                            null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (createdVM == null) "CREATE YOUR FIRST VM" else "OPEN MACHINE",
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QUICK LAUNCH", modifier = Modifier.weight(1f), fontSize = 14.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
            Text("VIEW ALL  →", modifier = Modifier.clickable(onClick = onVmsClick),
                color = nextRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextHomeAction("New machine", "Set up a virtual PC", Icons.Filled.Add,
                Modifier.weight(1f), onCreateVM)
            NextHomeAction("VM library", "Your saved machines", Icons.Filled.Memory,
                Modifier.weight(1f), onVmsClick)
        }
        Spacer(Modifier.height(27.dp))
        Text("EXPLORE FILES", fontSize = 14.sp, fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp)
        Spacer(Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxWidth().clickable { onQuickAccess("Windows") },
            shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Folder, null, tint = nextRed, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text("Browse disk images", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Open your files and OS images", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, null, tint = nextRed)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ENGINE STATUS", fontSize = 10.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp)
                    Text(qemuStatus.ifBlank { "Ready for your next session" }, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NextHomeAction(title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(17.dp)) {
            Surface(color = Color(0x33FF4054), shape = RoundedCornerShape(13.dp)) {
                Icon(icon, null, tint = nextRed,
                    modifier = Modifier.padding(12.dp).size(25.dp))
            }
            Spacer(Modifier.height(21.dp))
            Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

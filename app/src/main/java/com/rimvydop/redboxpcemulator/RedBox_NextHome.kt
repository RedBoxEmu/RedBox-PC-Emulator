package com.rimvydop.redboxpcemulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimvydop.redboxpcemulator.ui.theme.*

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

    // Pulsing animation for running state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RedBoxBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // ====================================================================
        // 1. TOP BAR / BRAND HEADER
        // ====================================================================
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF151922))
                    .padding(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.redbox_logo),
                    contentDescription = "RedBox logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "REDBOX",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "PC",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        color = RedBoxCrimson
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = RedBoxCrimson.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, RedBoxCrimson.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "v1.1.0",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedBoxCrimson,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "QEMU 11.1.1 • ARM64 TCG ENGINE",
                    fontSize = 9.sp,
                    color = RedBoxTextSecondary,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Engine status indicator pill
            Surface(
                color = if (isRunning) RedBoxStatusOnline.copy(alpha = 0.15f) else Color(0xFF151922),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRunning) RedBoxStatusOnline.copy(alpha = 0.5f) else Color(0x22FFFFFF)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) RedBoxStatusOnline.copy(alpha = pulseAlpha)
                                else RedBoxStatusStandby
                            )
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isRunning) "RUNNING" else "STANDBY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isRunning) RedBoxStatusOnline else RedBoxTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // ====================================================================
        // 2. HERO GREETING WITH ENTRANCE ANIMATION
        // ====================================================================
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
        ) {
            Column {
                Text(
                    text = "CONSOLE WORKSPACE",
                    color = RedBoxCrimson,
                    fontSize = 11.sp,
                    letterSpacing = 2.8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "High Performance PC Emulation.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
                Text(
                    text = "Native x86_64 virtualization powered by QEMU JIT.",
                    fontSize = 13.sp,
                    color = RedBoxTextSecondary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ====================================================================
        // 3. HERO MACHINE DECK (FEATURED / ACTIVE VIRTUAL MACHINE)
        // ====================================================================
        val hasVM = createdVM != null
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = RedBoxSurface,
            border = BorderStroke(1.dp, RedBoxGlassBorderGradient),
            shadowElevation = 16.dp
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isRunning) Color(0xFF0F2B1F) else Color(0xFF381017),
                            Color(0xFF131722),
                            Color(0xFF0A0D13)
                        ),
                        radius = 900f
                    )
                )
            ) {
                Column(Modifier.padding(22.dp)) {
                    // Top header row inside card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // OS Avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        ) {
                            val vmNameLower = createdVM?.name?.lowercase().orEmpty()
                            val osDrawableId = when {
                                "win" in vmNameLower -> R.drawable.redbox_windows_logo
                                "linux" in vmNameLower || "ubuntu" in vmNameLower -> R.drawable.redbox_linux_logo
                                "android" in vmNameLower -> R.drawable.redbox_android_logo
                                else -> null
                            }

                            if (osDrawableId != null) {
                                Image(
                                    painter = painterResource(id = osDrawableId),
                                    contentDescription = "OS Logo",
                                    modifier = Modifier.size(26.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Computer,
                                    contentDescription = null,
                                    tint = if (isRunning) RedBoxStatusOnline else RedBoxCrimson,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE PROFILE",
                                fontSize = 9.sp,
                                letterSpacing = 2.sp,
                                color = RedBoxTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = createdVM?.architecture ?: "x86_64 Emulated PC",
                                fontSize = 11.sp,
                                color = RedBoxCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Status pill in hero
                        Surface(
                            color = if (isRunning) RedBoxStatusOnline.copy(alpha = 0.18f) else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isRunning) RedBoxStatusOnline.copy(alpha = 0.4f) else Color(0x22FFFFFF)
                            )
                        ) {
                            Text(
                                text = if (isRunning) "● ACTIVE SESSION" else "STANDBY",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) RedBoxStatusOnline else Color(0xFFC7D2E5)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // VM Name
                    Text(
                        text = createdVM?.name ?: "No Machine Configured",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (!hasVM) "Create a virtual machine profile to install Windows or Linux."
                        else if (isRunning) "Session is active in isolated background process (:qemu)."
                        else "Ready to boot with ${createdVM?.ram ?: "1024 MB"} RAM and ${createdVM?.cpuCores ?: "1 Core"}.",
                        fontSize = 12.sp,
                        color = RedBoxTextSecondary
                    )

                    Spacer(Modifier.height(18.dp))

                    // Spec chips row
                    if (hasVM) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SpecBadge(label = createdVM?.ram ?: "1024 MB", icon = Icons.Filled.Memory)
                            SpecBadge(label = "${createdVM?.cpuCores ?: 1} Core", icon = Icons.Filled.Tune)
                            SpecBadge(label = createdVM?.diskInterface ?: "AHCI", icon = Icons.Filled.Storage)
                            SpecBadge(label = createdVM?.displayAdapter ?: "VGA", icon = Icons.Filled.Computer)
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Main Action CTA Button
                    Button(
                        onClick = if (!hasVM) onCreateVM else onVMClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFF0D7E45) else RedBoxCrimson
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (!hasVM) Icons.Filled.Add else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                !hasVM -> "CREATE VIRTUAL MACHINE"
                                isRunning -> "RESUME RUNNING VM"
                                else -> "START VIRTUAL MACHINE"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ====================================================================
        // 4. HARDWARE TELEMETRY CARDS (3-COLUMN SPEC TILES)
        // ====================================================================
        Text(
            text = "HARDWARE & PERFORMANCE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.8.sp,
            color = RedBoxTextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TelemetryTile(
                title = "EMULATED CPU",
                value = createdVM?.cpuModel?.ifBlank { "qemu64" } ?: "qemu64",
                subtitle = "TCG JIT Acceleration",
                accent = RedBoxCyan,
                icon = Icons.Filled.Memory,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "SYSTEM RAM",
                value = createdVM?.ram ?: "1024 MB",
                subtitle = "TCG Cache: ${createdVM?.tcgCache ?: "256 MB"}",
                accent = RedBoxPurple,
                icon = Icons.Filled.Storage,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "STORAGE BUS",
                value = createdVM?.diskInterface ?: "AHCI SATA",
                subtitle = createdVM?.diskImageName?.ifBlank { "VHD / QCOW2" } ?: "VHD / QCOW2",
                accent = RedBoxAmber,
                icon = Icons.Filled.Tune,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(26.dp))

        // ====================================================================
        // 5. QUICK LAUNCH ACTION TILES
        // ====================================================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "COMMAND CENTER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
                color = RedBoxTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ALL MACHINES →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RedBoxCrimson,
                modifier = Modifier.clickable(onClick = onVmsClick)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionTile(
                title = "New Machine",
                subtitle = "Configure virtual PC",
                icon = Icons.Filled.Add,
                accent = RedBoxCrimson,
                modifier = Modifier.weight(1f),
                onClick = onCreateVM
            )
            ActionTile(
                title = "VM Library",
                subtitle = "Saved profiles & disks",
                icon = Icons.Filled.List,
                accent = RedBoxCyan,
                modifier = Modifier.weight(1f),
                onClick = onVmsClick
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionTile(
                title = "Disk & ISO Media",
                subtitle = "Browse SAF storage",
                icon = Icons.Filled.Folder,
                accent = RedBoxAmber,
                modifier = Modifier.weight(1f),
                onClick = { onQuickAccess("Windows") }
            )
            ActionTile(
                title = "Hardware Tuning",
                subtitle = "VirGL 3D & Audio",
                icon = Icons.Filled.Tune,
                accent = RedBoxPurple,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (hasVM) onVMClick() else onCreateVM()
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ====================================================================
        // 6. LIVE ENGINE STATUS TERMINAL BAR
        // ====================================================================
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0B0E14),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FF2A42)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = RedBoxCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CORE ENGINE TERMINAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = RedBoxTextSecondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "QEMU 11.1.1",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedBoxCyan
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = qemuStatus.ifBlank { "QEMU 11.1.1 JIT engine initialized and ready." },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4E2F5),
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ============================================================================
// HELPER COMPONENTS
// ============================================================================

@Composable
private fun SpecBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = Color(0x2AFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RedBoxCyan,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = RedBoxSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0x18FFFFFF))
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = RedBoxTextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = RedBoxTextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = RedBoxSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0x1EFFFFFF))
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = RedBoxTextSecondary
            )
        }
    }
}

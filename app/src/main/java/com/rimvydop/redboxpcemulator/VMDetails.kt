package com.rimvydop.redboxpcemulator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimvydop.redboxpcemulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMDetailsScreen(
    vm: VMModel,
    isRunning: Boolean,
    qemuRuntimeStatus: String,
    onEditVM: () -> Unit,
    onStartVM: () -> Unit,
    onStopVM: () -> Unit,
    onReturnToVM: () -> Unit,
    onDuplicateVM: () -> Unit,
    onChangePrimaryIso: () -> Unit,
    onEjectPrimaryIso: () -> Unit,
    onChangeDriverIso: () -> Unit,
    onEjectDriverIso: () -> Unit,
    onDeleteVM: () -> Unit,
    onBack: () -> Unit
) {
    // Pulsing transition for online state
    val infiniteTransition = rememberInfiniteTransition(label = "vmDetailsPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Formatted core count (prevents "4 cores cores" duplication bug)
    val rawCores = vm.cpuCores.filter { it.isDigit() }.ifEmpty { "1" }
    val formattedCores = "$rawCores core${if (rawCores == "1") "" else "s"}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VM DETAILS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = RedBoxTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = RedBoxCrimson.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, RedBoxCrimson.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = vm.architecture.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedBoxCrimson,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RedBoxTextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onEditVM,
                        enabled = !isRunning
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Lock else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isRunning) RedBoxTextSecondary else RedBoxCrimson
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isRunning) "Locked" else "Edit",
                                color = if (isRunning) RedBoxTextSecondary else RedBoxCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedBoxSurface
                )
            )
        },
        containerColor = RedBoxBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================================================================
            // 1. HERO VM PROFILE CARD
            // ================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = RedBoxCardBg,
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Machine icon badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(RedBoxCrimson.copy(alpha = 0.12f))
                                .border(1.dp, RedBoxCrimson.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = RedBoxCrimson,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vm.name,
                                color = RedBoxTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${vm.architecture}  \u2022  ${vm.ram}  \u2022  $formattedCores",
                                color = RedBoxTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Status pill badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isRunning) RedBoxStatusOnline.copy(alpha = 0.15f) else Color(0xFF1E232F),
                            border = BorderStroke(
                                1.dp,
                                if (isRunning) RedBoxStatusOnline.copy(alpha = 0.5f) else Color(0x22FFFFFF)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isRunning) RedBoxStatusOnline.copy(alpha = pulseAlpha)
                                            else RedBoxStatusStandby
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "RUNNING" else "STOPPED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = if (isRunning) RedBoxStatusOnline else RedBoxTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = RedBoxCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when {
                            vm.diskImageName.isNotBlank() && vm.isoImageName.isNotBlank() ->
                                "Disk image and installation media are mounted."
                            vm.diskImageName.isNotBlank() ->
                                "Disk image attached. No primary ISO selected."
                            vm.isoImageName.isNotBlank() ->
                                "Installation media attached. No primary disk selected."
                            else ->
                                "No boot storage or installation media selected."
                        },
                        color = RedBoxTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isRunning) {
                        Button(
                            onClick = onReturnToVM,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Return to Viewport", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onStopVM,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop VM", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = onStartVM,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start VM", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Engine status banner if active
            if (qemuRuntimeStatus.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = RedBoxSurface,
                    border = BorderStroke(1.dp, RedBoxCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = RedBoxAccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRunning) "QEMU Engine" else "Status Message",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RedBoxTextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = qemuRuntimeStatus,
                                color = RedBoxTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ================================================================
            // 2. HARDWARE SPECIFICATIONS
            // ================================================================
            Text(
                text = "HARDWARE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = RedBoxTextSecondary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RedBoxCardBg,
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    VMDetailRow(
                        icon = Icons.Default.Memory,
                        title = "Memory",
                        value = vm.ram
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Speed,
                        title = "Processor",
                        value = "$formattedCores \u2022 ${vm.cpuModel}"
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.DeveloperBoard,
                        title = "Machine Type",
                        value = when (vm.machineType) {
                            "isapc" -> "ISA PC (Legacy)"
                            "q35" -> "Q35 (Modern PCIe)"
                            else -> "PC (i440FX + PIIX)"
                        }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Tv,
                        title = "Display Adapter",
                        value = vm.displayAdapter
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Tune,
                        title = "3D Acceleration",
                        value = if (vm.displayAdapter == "VirtIO VGA" && vm.threeDAcceleration) "Enabled (VirGL)" else "Disabled"
                    )
                }
            }

            // ================================================================
            // 3. STORAGE & MEDIA
            // ================================================================
            Text(
                text = "STORAGE & DRIVES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = RedBoxTextSecondary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RedBoxCardBg,
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    VMDetailRow(
                        icon = Icons.Default.Storage,
                        title = "Disk Image",
                        value = vm.diskImageName.ifBlank { "Not selected" }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Album,
                        title = "Primary ISO",
                        value = vm.isoImageName.ifBlank { "Not selected" }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Album,
                        title = "Driver ISO",
                        value = vm.driverIsoImageName.ifBlank { "Not selected" }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.FolderShared,
                        title = "Shared Drive",
                        value = vm.sharedDiskImageName.ifBlank { "Not selected" }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.SettingsInputComponent,
                        title = "Disk Interface",
                        value = vm.diskInterface
                    )
                }
            }

            // ================================================================
            // 4. REMOVABLE MEDIA MANAGEMENT
            // ================================================================
            Text(
                text = "REMOVABLE MEDIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = RedBoxTextSecondary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RedBoxCardBg,
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Primary Installation ISO",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = RedBoxTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = vm.isoImageName.ifBlank { "No ISO mounted" },
                        color = RedBoxTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChangePrimaryIso,
                            enabled = !isRunning,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RedBoxCardBorder)
                        ) {
                            Text(if (vm.isoImage.isBlank()) "Mount ISO" else "Change ISO", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onEjectPrimaryIso,
                            enabled = !isRunning && vm.isoImage.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RedBoxCardBorder)
                        ) {
                            Text("Eject", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = RedBoxCardBorder)

                    Text(
                        text = "Driver & Tools ISO",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = RedBoxTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = vm.driverIsoImageName.ifBlank { "No driver ISO mounted" },
                        color = RedBoxTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChangeDriverIso,
                            enabled = !isRunning,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RedBoxCardBorder)
                        ) {
                            Text(if (vm.driverIsoImage.isBlank()) "Mount ISO" else "Change ISO", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onEjectDriverIso,
                            enabled = !isRunning && vm.driverIsoImage.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RedBoxCardBorder)
                        ) {
                            Text("Eject", fontSize = 12.sp)
                        }
                    }

                    if (isRunning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Stop the virtual machine before changing mounted ISOs.",
                            color = RedBoxStatusStandby,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ================================================================
            // 5. ENGINE & SUBSYSTEMS
            // ================================================================
            Text(
                text = "ENGINE & DEVICES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = RedBoxTextSecondary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RedBoxCardBg,
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    VMDetailRow(
                        icon = Icons.Default.Speed,
                        title = "Performance Preset",
                        value = "${vm.performancePreset} \u2022 TCG ${vm.tcgCache}"
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.AltRoute,
                        title = "Multi-threaded TCG",
                        value = if (vm.multiThreadedTcg) "Enabled" else "Disabled"
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Wifi,
                        title = "Network Subsystem",
                        value = if (vm.networkEnabled) "${vm.networkAdapter} \u2022 ${vm.networkMode}" else "Disabled"
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound Card",
                        value = vm.soundCard
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RedBoxCardBorder)
                    VMDetailRow(
                        icon = Icons.Default.Terminal,
                        title = "Custom QEMU Parameters",
                        value = if (vm.qemuParams.isBlank()) "None" else "Active"
                    )
                }
            }

            // ================================================================
            // 6. MACHINE ACTIONS
            // ================================================================
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onDuplicateVM,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Duplicate VM", color = RedBoxTextPrimary, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onEditVM,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RedBoxCardBorder)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Edit VM (Stop VM First)" else "Edit Configuration",
                    color = if (isRunning) RedBoxTextSecondary else RedBoxTextPrimary,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = onDeleteVM,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Delete VM (Stop VM First)" else "Delete Machine",
                    fontSize = 13.sp
                )
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to VM Library", color = RedBoxTextSecondary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VMDetailRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = RedBoxCrimson.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = RedBoxCrimson
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = RedBoxTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = RedBoxTextPrimary
            )
        }
    }
}

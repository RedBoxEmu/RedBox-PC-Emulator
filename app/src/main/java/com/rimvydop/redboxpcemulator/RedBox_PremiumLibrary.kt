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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimvydop.redboxpcemulator.ui.theme.*

@Composable
fun RedBoxPremiumLibrary(
    modifier: Modifier = Modifier,
    savedVMs: List<VMModel>,
    runningVMIndex: Int,
    isRunning: Boolean,
    onCreateVM: () -> Unit,
    onVMClick: (Int) -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    // Pulsing animation for running machines
    val infiniteTransition = rememberInfiniteTransition(label = "libraryPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "libraryPulseAlpha"
    )

    val visible = savedVMs.mapIndexed { index, vm -> index to vm }.filter { (index, vm) ->
        val matchesSearch = vm.name.contains(search, ignoreCase = true) ||
                vm.architecture.contains(search, ignoreCase = true) ||
                vm.diskImageName.contains(search, ignoreCase = true)

        val vmNameLower = vm.name.lowercase()
        val matchesFilter = when (filter) {
            "Running" -> isRunning && runningVMIndex == index
            "Stopped" -> !(isRunning && runningVMIndex == index)
            "Windows" -> "win" in vmNameLower
            "Linux" -> "linux" in vmNameLower || "ubuntu" in vmNameLower
            else -> true
        }

        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RedBoxBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====================================================================
        // 1. HEADER ROW
        // ====================================================================
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF151922))
                        .padding(5.dp)
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
                            text = "MACHINE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LIBRARY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = RedBoxCrimson
                        )
                    }
                    Text(
                        text = "${savedVMs.size} PROFILE${if (savedVMs.size == 1) "" else "S"} CONFIGURED",
                        fontSize = 9.sp,
                        color = RedBoxTextSecondary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // New VM Quick Action Button
                FilledIconButton(
                    onClick = onCreateVM,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = RedBoxCrimson
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create VM",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ====================================================================
        // 2. FLEET METRICS BANNER
        // ====================================================================
        item {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RedBoxSurface,
                    border = BorderStroke(1.dp, RedBoxGlassBorderGradient),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF381018),
                                    Color(0xFF141924),
                                    Color(0xFF0C0F17)
                                )
                            )
                        )
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "VIRTUAL FLEET STATUS",
                                    fontSize = 10.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedBoxCrimson,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isRunning) {
                                    Surface(
                                        color = RedBoxStatusOnline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(RedBoxStatusOnline.copy(alpha = pulseAlpha))
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = RedBoxStatusOnline
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "${savedVMs.size} Virtual PC${if (savedVMs.size == 1) "" else "s"}",
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = if (isRunning) "Active session running in isolated :qemu process."
                                else "All machines idle. Select a virtual machine to launch or edit.",
                                color = RedBoxTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 3. SEARCH FIELD
        // ====================================================================
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search by name, architecture, disk...",
                        color = RedBoxTextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = RedBoxCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = RedBoxSurface,
                    unfocusedContainerColor = RedBoxSurface,
                    focusedBorderColor = RedBoxCrimson,
                    unfocusedBorderColor = Color(0x24FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // ====================================================================
        // 4. FILTER PILLS
        // ====================================================================
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Running", "Stopped", "Windows", "Linux").forEach { option ->
                    val isSelected = filter == option
                    Surface(
                        modifier = Modifier.clickable { filter = option },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RedBoxCrimson else RedBoxSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) RedBoxCrimson else Color(0x1FFFFFFF)
                        )
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else RedBoxTextSecondary
                        )
                    }
                }
            }
        }

        // ====================================================================
        // 5. MACHINE LIST OR EMPTY STATE
        // ====================================================================
        if (visible.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RedBoxSurface,
                    border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(RedBoxCrimson.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = RedBoxCrimson
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = if (savedVMs.isEmpty()) "No Virtual PCs Found" else "No Matching Profiles",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = if (savedVMs.isEmpty())
                                "Create a virtual machine profile to start emulating Windows or Linux on Android."
                            else "No machines match your filter. Try adjusting your search query.",
                            color = RedBoxTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (savedVMs.isEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onCreateVM,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "CREATE YOUR FIRST VM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(visible, key = { _, pair -> pair.first }) { _, (originalIndex, vm) ->
                val running = isRunning && runningVMIndex == originalIndex
                val vmNameLower = vm.name.lowercase()

                val osDrawableId = when {
                    "win" in vmNameLower -> R.drawable.redbox_windows_logo
                    "linux" in vmNameLower || "ubuntu" in vmNameLower -> R.drawable.redbox_linux_logo
                    "android" in vmNameLower -> R.drawable.redbox_android_logo
                    else -> null
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVMClick(originalIndex) },
                    shape = RoundedCornerShape(22.dp),
                    color = RedBoxSurface,
                    border = BorderStroke(
                        1.dp,
                        if (running) RedBoxStatusOnline.copy(alpha = 0.4f) else Color(0x1AFFFFFF)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(18.dp)) {
                        // Header row of VM card
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
                                    .background(Color(0xFF161B26))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            ) {
                                if (osDrawableId != null) {
                                    Image(
                                        painter = painterResource(id = osDrawableId),
                                        contentDescription = "OS Logo",
                                        modifier = Modifier.size(24.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Computer,
                                        contentDescription = null,
                                        tint = if (running) RedBoxStatusOnline else RedBoxCrimson,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = vm.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${vm.architecture} • ${vm.machineType.uppercase()}",
                                    fontSize = 11.sp,
                                    color = RedBoxCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Running / Standby Pill
                            Surface(
                                color = if (running) RedBoxStatusOnline.copy(alpha = 0.18f) else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (running) RedBoxStatusOnline.copy(alpha = pulseAlpha)
                                                else RedBoxStatusStandby
                                            )
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (running) "RUNNING" else "STANDBY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (running) RedBoxStatusOnline else RedBoxTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Specs pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LibraryChip(label = vm.ram, icon = Icons.Filled.Memory)
                            LibraryChip(label = "${vm.cpuCores} Core", icon = Icons.Filled.Tune)
                            LibraryChip(label = vm.diskInterface, icon = Icons.Filled.Storage)
                            LibraryChip(label = vm.displayAdapter, icon = Icons.Filled.Computer)
                        }

                        Spacer(Modifier.height(12.dp))

                        // Storage metadata line
                        val storageText = when {
                            vm.diskImageName.isNotBlank() -> "Disk: ${vm.diskImageName}"
                            vm.isoImageName.isNotBlank() -> "Boot ISO: ${vm.isoImageName}"
                            else -> "No virtual disk attached"
                        }

                        Text(
                            text = storageText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            color = RedBoxTextSecondary
                        )

                        Spacer(Modifier.height(14.dp))

                        HorizontalDivider(color = Color(0x18FFFFFF))

                        Spacer(Modifier.height(10.dp))

                        // Bottom action buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TAP TO CONFIGURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = RedBoxTextMuted,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { onVMClick(originalIndex) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (running) Color(0xFF0E7D46) else RedBoxCrimson
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (running) Icons.Filled.Computer else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (running) "OPEN CONTROLS" else "LAUNCH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = Color(0x1FFFFFFF),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0x14FFFFFF))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RedBoxCyan,
                modifier = Modifier.size(10.dp)
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

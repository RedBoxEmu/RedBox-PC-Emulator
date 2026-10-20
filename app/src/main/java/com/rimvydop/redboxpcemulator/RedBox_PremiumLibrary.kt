package com.rimvydop.redboxpcemulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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

private val libraryRed = Color(0xFFFF4054)

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
    val visible = savedVMs.mapIndexed { index, vm -> index to vm }.filter { (index, vm) ->
        vm.name.contains(search, ignoreCase = true) && when (filter) {
            "Running" -> isRunning && runningVMIndex == index
            "Stopped" -> !(isRunning && runningVMIndex == index)
            else -> true
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.redbox_logo), "RedBox logo", Modifier.size(48.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("MACHINE LIBRARY", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = libraryRed)
                    Text("Your virtual PCs", fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                FilledIconButton(onClick = onCreateVM, colors = IconButtonDefaults.filledIconButtonColors(containerColor = libraryRed)) {
                    Icon(Icons.Filled.Add, "Create VM", tint = Color.White)
                }
            }
        }
        item {
            AnimatedVisibility(visible = entered, enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 4 }) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFF49212D), Color(0xFF211721), Color(0xFF141720)))
                )) {
                    Column(Modifier.padding(22.dp)) {
                        Text("YOUR FLEET", fontSize = 10.sp, color = Color(0xFFFFA9B5), letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("${savedVMs.size} saved machine${if (savedVMs.size == 1) "" else "s"}", fontSize = 25.sp, color = Color.White, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text(if (isRunning) "A virtual PC is currently running" else "Ready when you are", color = Color(0xFFE3C8D0), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search machines") },
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Running", "Stopped").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                    Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Computer, null, Modifier.size(40.dp), tint = libraryRed)
                        Spacer(Modifier.height(12.dp))
                        Text(if (savedVMs.isEmpty()) "No virtual PCs yet" else "No matching machines", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(if (savedVMs.isEmpty()) "Create a machine to start building your library." else "Try another search or filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        if (savedVMs.isEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = onCreateVM) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Create VM") }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(visible, key = { _, pair -> pair.first }) { _, (originalIndex, vm) ->
                val running = isRunning && runningVMIndex == originalIndex
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onVMClick(originalIndex) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(libraryRed.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Computer, null, Modifier.size(27.dp), tint = libraryRed)
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(vm.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Text(vm.architecture, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, "Open VM", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(15.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${vm.ram} RAM  •  ${vm.cpuCores} CPU", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (running) "● RUNNING" else "● STOPPED", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = if (running) Color(0xFF4BC98A) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val storage = when {
                            vm.diskImageName.isNotBlank() -> "Disk: ${vm.diskImageName}"
                            vm.isoImageName.isNotBlank() -> "ISO: ${vm.isoImageName}"
                            else -> "No storage selected"
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(storage, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onVMClick(originalIndex) }) {
                                Icon(if (running) Icons.Filled.Computer else Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (running) "OPEN CONTROLS" else "OPEN MACHINE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

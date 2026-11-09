package com.rimvydop.redboxpcemulator

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.system.Os
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimvydop.redboxpcemulator.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVMScreen(
    vm: VMModel,
    onBack: () -> Unit,
    onSave: (VMModel) -> Unit
) {
    val context = LocalContext.current

    // Core Identity
    var name by remember(vm) { mutableStateOf(vm.name) }
    var architecture by remember(vm) { mutableStateOf(vm.architecture) }

    // Compute & CPU
    var ram by remember(vm) { mutableStateOf(vm.ram) }
    var cpuCores by remember(vm) { mutableStateOf(vm.cpuCores) }
    var performancePreset by remember(vm) { mutableStateOf(vm.performancePreset) }
    var cpuModel by remember(vm) { mutableStateOf(vm.cpuModel) }
    var cpuFlags by remember(vm) { mutableStateOf(vm.cpuFlags) }
    var tcgCache by remember(vm) { mutableStateOf(vm.tcgCache) }
    var multiThreadedTcg by remember(vm) { mutableStateOf(vm.multiThreadedTcg) }
    var machineType by remember(vm) { mutableStateOf(vm.machineType) }
    var highPriority by remember(vm) { mutableStateOf(vm.highPriority) }

    // Storage & Media
    var diskInterface by remember(vm) { mutableStateOf(vm.diskInterface) }
    var diskImage by remember(vm) { mutableStateOf(vm.diskImage) }
    var diskImageName by remember(vm) { mutableStateOf(vm.diskImageName) }
    var isoImage by remember(vm) { mutableStateOf(vm.isoImage) }
    var isoImageName by remember(vm) { mutableStateOf(vm.isoImageName) }
    var driverIsoImage by remember(vm) { mutableStateOf(vm.driverIsoImage) }
    var driverIsoImageName by remember(vm) { mutableStateOf(vm.driverIsoImageName) }
    var sharedDiskImage by remember(vm) { mutableStateOf(vm.sharedDiskImage) }
    var sharedDiskImageName by remember(vm) { mutableStateOf(vm.sharedDiskImageName) }
    var sharedFolderEnabled by remember(vm) { mutableStateOf(vm.sharedFolderEnabled) }
    var sharedFolderLastFileName by remember(vm) { mutableStateOf(vm.sharedFolderLastFileName) }

    // Display & Audio
    var displayAdapter by remember(vm) { mutableStateOf(vm.displayAdapter) }
    var threeDAcceleration by remember(vm) { mutableStateOf(vm.threeDAcceleration) }
    var soundCard by remember(vm) { mutableStateOf(vm.soundCard) }

    // System & Network
    var bootPriority by remember(vm) { mutableStateOf(vm.bootPriority) }
    var firmwareMode by remember(vm) { mutableStateOf(vm.firmwareMode) }
    var networkEnabled by remember(vm) { mutableStateOf(vm.networkEnabled) }
    var networkAdapter by remember(vm) { mutableStateOf(vm.networkAdapter) }
    var networkMode by remember(vm) { mutableStateOf(vm.networkMode) }
    var qemuParams by remember(vm) { mutableStateOf(vm.qemuParams) }
    var biosDate by remember(vm) { mutableStateOf(vm.biosDate) }

    // Active Category Tab
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf(
        "Compute" to Icons.Filled.Memory,
        "Storage" to Icons.Filled.Storage,
        "Display & Sound" to Icons.Filled.Computer,
        "System" to Icons.Filled.Tune
    )

    // New Disk Dialog State
    var showCreateDiskDialog by remember { mutableStateOf(false) }
    var newDiskName by remember { mutableStateOf("RedBoxDisk.img") }
    var newDiskSizeGb by remember { mutableIntStateOf(32) }

    // Custom RAM & CPU Modal Option Dialogs
    var showCustomRamDialog by remember { mutableStateOf(false) }

    // Document Name & Permission Helpers
    fun displayName(uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index) ?: uri.lastPathSegment.orEmpty()
                } else {
                    uri.lastPathSegment.orEmpty()
                }
            } ?: uri.lastPathSegment.orEmpty()
        } catch (_: Exception) {
            uri.lastPathSegment.orEmpty()
        }
    }

    fun persistUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
    }

    val createDiskLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            try {
                val finalName = newDiskName
                    .trim()
                    .ifBlank { "RedBoxDisk.img" }
                    .let { if (it.endsWith(".img", ignoreCase = true)) it else "$it.img" }

                val sizeBytes = newDiskSizeGb.toLong() * 1024L * 1024L * 1024L
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    Os.ftruncate(pfd.fileDescriptor, sizeBytes)
                } ?: throw IllegalStateException("Could not open new disk file")

                persistUri(uri)
                diskImage = uri.toString()
                diskImageName = finalName

                Toast.makeText(context, "Created $finalName (${newDiskSizeGb} GB)", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, "Could not create disk: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val diskPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(uri)
            diskImage = uri.toString()
            diskImageName = displayName(uri)
        }
    }

    val isoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(uri)
            isoImage = uri.toString()
            isoImageName = displayName(uri)
        }
    }

    val driverIsoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(uri)
            driverIsoImage = uri.toString()
            driverIsoImageName = displayName(uri)
        }
    }

    val sharedDiskPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(uri)
            sharedDiskImage = uri.toString()
            sharedDiskImageName = displayName(uri)
        }
    }

    fun importFileToSharedFolder(uri: Uri) {
        try {
            val safeName = File(displayName(uri)).name.ifBlank { "shared_file" }
            val sharedFolder = File(context.filesDir, "redbox_shared")
            sharedFolder.mkdirs()
            val destination = File(sharedFolder, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open selected file")

            sharedFolderEnabled = true
            sharedFolderLastFileName = safeName
            sharedDiskImage = ""
            sharedDiskImageName = ""

            Toast.makeText(context, "Added $safeName to Shared Folder", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(context, "Could not add file: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    val sharedFolderFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importFileToSharedFolder(uri)
        }
    }

    fun saveCurrentModel() {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            onSave(
                VMModel(
                    name = cleanName,
                    architecture = architecture,
                    ram = ram,
                    cpuCores = cpuCores,
                    diskImage = diskImage,
                    diskImageName = diskImageName,
                    isoImage = isoImage,
                    isoImageName = isoImageName,
                    driverIsoImage = driverIsoImage,
                    driverIsoImageName = driverIsoImageName,
                    sharedDiskImage = sharedDiskImage,
                    sharedDiskImageName = sharedDiskImageName,
                    sharedFolderEnabled = sharedFolderEnabled,
                    sharedFolderLastFileName = sharedFolderLastFileName,
                    performancePreset = performancePreset,
                    cpuModel = cpuModel,
                    cpuFlags = cpuFlags,
                    tcgCache = tcgCache,
                    multiThreadedTcg = multiThreadedTcg,
                    machineType = machineType,
                    diskInterface = diskInterface,
                    displayAdapter = displayAdapter,
                    threeDAcceleration = threeDAcceleration && displayAdapter == "VirtIO VGA",
                    networkEnabled = networkEnabled,
                    networkAdapter = networkAdapter,
                    networkMode = networkMode,
                    qemuParams = qemuParams.trim(),
                    biosDate = biosDate.trim().ifBlank { "Default" },
                    soundCard = soundCard,
                    audioBackend = "Default",
                    bootPriority = bootPriority,
                    firmwareMode = firmwareMode,
                    highPriority = highPriority
                )
            )
        }
    }

    // New Disk Dialog
    if (showCreateDiskDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDiskDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = RedBoxSurfaceElevated,
            title = {
                Text("Create Virtual Disk", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = newDiskName,
                        onValueChange = { newDiskName = it },
                        label = { Text("Disk Name (.img / .vhd / .qcow2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedBoxCrimson,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    Text("Storage Capacity", fontWeight = FontWeight.Bold, color = RedBoxTextSecondary, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(16, 32, 64, 128).forEach { size ->
                            val selected = newDiskSizeGb == size
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newDiskSizeGb = size },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) RedBoxCrimson else Color(0x1AFFFFFF),
                                border = BorderStroke(1.dp, if (selected) RedBoxCrimson else Color(0x22FFFFFF))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$size GB",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else RedBoxTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Created as a sparse file that grows dynamically on device storage as the guest writes data.",
                        fontSize = 11.sp,
                        color = RedBoxTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeName = newDiskName.trim().ifBlank { "RedBoxDisk.img" }
                            .let { if (it.contains(".")) it else "$it.img" }
                        newDiskName = safeName
                        showCreateDiskDialog = false
                        createDiskLauncher.launch(safeName)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson)
                ) {
                    Text("CREATE DISK", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDiskDialog = false }) {
                    Text("CANCEL", color = RedBoxTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    // Custom RAM Dialog
    if (showCustomRamDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRamDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = RedBoxSurfaceElevated,
            title = {
                Text("Select System Memory", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        listOf("128 MB", "256 MB", "512 MB"),
                        listOf("1024 MB", "1536 MB", "2048 MB"),
                        listOf("3072 MB", "4096 MB", "6144 MB"),
                        listOf("8192 MB", "12288 MB", "16384 MB")
                    ).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { size ->
                                val selected = ram == size
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            ram = size
                                            showCustomRamDialog = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) RedBoxCyan else Color(0x1AFFFFFF),
                                    border = BorderStroke(1.dp, if (selected) RedBoxCyan else Color(0x22FFFFFF))
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = size,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selected) Color(0xFF002229) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomRamDialog = false }) {
                    Text("CLOSE", color = RedBoxTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // OS Avatar Logic
    val vmNameLower = name.lowercase()
    val osDrawableId = when {
        "win" in vmNameLower -> R.drawable.redbox_windows_logo
        "linux" in vmNameLower || "ubuntu" in vmNameLower -> R.drawable.redbox_linux_logo
        "android" in vmNameLower -> R.drawable.redbox_android_logo
        else -> null
    }

    Scaffold(
        containerColor = RedBoxBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EDIT",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "VM",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = RedBoxCrimson
                            )
                        }
                        Text(
                            text = "${name.ifBlank { vm.name }} · Hardware Tuning",
                            fontSize = 11.sp,
                            color = RedBoxTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick Save Button right in the TopAppBar!
                    Button(
                        onClick = { saveCurrentModel() },
                        enabled = name.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("SAVE", fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedBoxBackground
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = RedBoxSurface,
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text("CANCEL", color = RedBoxTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { saveCurrentModel() },
                        enabled = name.trim().isNotEmpty(),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE CHANGES", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================================================================
            // HERO IDENTITY CARD (NAME & ARCHITECTURE)
            // ================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = RedBoxSurface,
                border = BorderStroke(1.dp, RedBoxGlassBorderGradient)
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF381017),
                                Color(0xFF131722),
                                Color(0xFF0C0F17)
                            )
                        )
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF191F2C))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            ) {
                                if (osDrawableId != null) {
                                    Image(
                                        painter = painterResource(id = osDrawableId),
                                        contentDescription = "OS",
                                        modifier = Modifier.size(24.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Computer,
                                        contentDescription = null,
                                        tint = RedBoxCrimson,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "VIRTUAL MACHINE PROFILE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = RedBoxTextSecondary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$ram RAM • $cpuCores • $diskInterface",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RedBoxCyan
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // VM Name TextField
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Machine Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x33000000),
                                unfocusedContainerColor = Color(0x33000000),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = RedBoxCrimson,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        // Target Architecture Selector
                        Text("Guest Architecture", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RedBoxTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("x86_64", "x86", "ARM64", "ARM").forEach { arch ->
                                val isSelected = architecture == arch
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { architecture = arch },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) RedBoxCrimson else Color(0x18FFFFFF),
                                    border = BorderStroke(1.dp, if (isSelected) RedBoxCrimson else Color(0x22FFFFFF))
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = arch,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else RedBoxTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================================================================
            // CATEGORY TABS BAR (Makes editing easy and uncluttered!)
            // ================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RedBoxSurface,
                border = BorderStroke(1.dp, Color(0x18FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEachIndexed { index, (label, icon) ->
                        val selected = selectedCategory == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategory = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) RedBoxCrimson else Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) Color.White else RedBoxTextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (selected) Color.White else RedBoxTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ================================================================
            // TAB CONTENT PAGES
            // ================================================================
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(200)) { it / 4 } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(160)) { -it / 4 } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(tween(200)) { -it / 4 } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(160)) { it / 4 } + fadeOut()
                        )
                    }
                },
                label = "EditVMTabs"
            ) { tabIndex ->
                when (tabIndex) {
                    // --------------------------------------------------------
                    // 0. COMPUTE & CPU
                    // --------------------------------------------------------
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // CPU Cores Quick Selector
                        EditSectionCard(title = "CPU Core Allocation", icon = Icons.Filled.Tune, accent = RedBoxCyan) {
                            Text("Number of virtual CPU cores allocated to the guest:", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("1", "2", "4", "6", "8").forEach { count ->
                                    val countText = "$count core${if (count == "1") "" else "s"}"
                                    val isSelected = cpuCores == count || cpuCores == countText
                                    SelectChip(
                                        label = "$count Cores",
                                        selected = isSelected,
                                        modifier = Modifier.weight(1f),
                                        accent = RedBoxCyan
                                    ) {
                                        cpuCores = countText
                                    }
                                }
                            }
                        }

                        // Memory Allocation Card
                        EditSectionCard(title = "RAM (Guest Memory)", icon = Icons.Filled.Memory, accent = RedBoxPurple) {
                            Text("Allocated memory: $ram", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("512 MB", "1024 MB", "2048 MB", "4096 MB").forEach { size ->
                                    SelectChip(
                                        label = size.replace(" MB", "M"),
                                        selected = ram == size,
                                        modifier = Modifier.weight(1f),
                                        accent = RedBoxPurple
                                    ) {
                                        ram = size
                                    }
                                }
                                SelectChip(
                                    label = "More...",
                                    selected = ram !in listOf("512 MB", "1024 MB", "2048 MB", "4096 MB"),
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxPurple
                                ) {
                                    showCustomRamDialog = true
                                }
                            }
                        }

                        // Performance Preset
                        EditSectionCard(title = "Performance Preset", icon = Icons.Filled.Speed, accent = RedBoxCrimson) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Compatibility", "Balanced", "Performance", "Custom").forEach { preset ->
                                    SelectChip(
                                        label = preset,
                                        selected = performancePreset == preset,
                                        modifier = Modifier.weight(1f),
                                        accent = RedBoxCrimson
                                    ) {
                                        performancePreset = preset
                                        when (preset) {
                                            "Compatibility" -> {
                                                cpuModel = "Default"
                                                tcgCache = "128 MB"
                                                multiThreadedTcg = false
                                                machineType = "pc"
                                            }
                                            "Balanced" -> {
                                                cpuModel = "Default"
                                                tcgCache = "256 MB"
                                                multiThreadedTcg = true
                                                machineType = "pc"
                                            }
                                            "Performance" -> {
                                                cpuModel = "Default"
                                                tcgCache = "512 MB"
                                                multiThreadedTcg = true
                                                machineType = "pc"
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CPU Model & Emulated Chipset
                        EditSectionCard(title = "Emulated CPU Model", icon = Icons.Filled.Memory, accent = RedBoxCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    listOf("Default", "qemu64", "core2duo"),
                                    listOf("Nehalem", "SandyBridge", "Haswell"),
                                    listOf("max")
                                ).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { model ->
                                            SelectChip(
                                                label = model,
                                                selected = cpuModel == model,
                                                modifier = Modifier.weight(1f),
                                                accent = RedBoxCyan
                                            ) {
                                                cpuModel = model
                                                performancePreset = "Custom"
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Emulated PC Motherboard Chipset
                        EditSectionCard(title = "PC Chipset Architecture", icon = Icons.Filled.Computer, accent = RedBoxAmber) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectChip(
                                    label = "PC (i440FX)",
                                    selected = machineType == "pc",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) {
                                    machineType = "pc"
                                    performancePreset = "Custom"
                                }
                                SelectChip(
                                    label = "Q35 (PCIe)",
                                    selected = machineType == "q35",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) {
                                    machineType = "q35"
                                    if (diskInterface == "IDE") diskInterface = "AHCI"
                                    performancePreset = "Custom"
                                }
                                SelectChip(
                                    label = "ISA PC (Legacy)",
                                    selected = machineType == "isapc",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) {
                                    machineType = "isapc"
                                    diskInterface = "IDE"
                                    performancePreset = "Custom"
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (machineType) {
                                    "q35" -> "Q35: Modern PCIe chipset. Best for Windows 10/11 & Linux."
                                    "isapc" -> "ISA PC: Ancient ISA bus for Windows 95, 3.1 & DOS."
                                    else -> "i440FX: Classic PCI chipset with maximum compatibility."
                                },
                                fontSize = 11.sp,
                                color = RedBoxTextMuted
                            )
                        }

                        // TCG Engine Tuning
                        EditSectionCard(title = "TCG Engine Tuning", icon = Icons.Filled.Terminal, accent = RedBoxStatusOnline) {
                            ToggleRow(
                                title = "Multi-threaded TCG",
                                subtitle = "Distribute emulation across Android CPU threads",
                                checked = multiThreadedTcg,
                                onCheckedChange = {
                                    multiThreadedTcg = it
                                    performancePreset = "Custom"
                                }
                            )

                            HorizontalDivider(color = Color(0x18FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                            Text("TCG Translation Cache Size:", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("128 MB", "256 MB", "512 MB", "1024 MB", "2048 MB").forEach { cache ->
                                    SelectChip(
                                        label = cache.replace(" MB", "M"),
                                        selected = tcgCache == cache,
                                        modifier = Modifier.weight(1f),
                                        accent = RedBoxStatusOnline
                                    ) {
                                        tcgCache = cache
                                        performancePreset = "Custom"
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0x18FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                            ToggleRow(
                                title = "Urgent Thread Priority",
                                subtitle = "Prioritize QEMU CPU thread over background Android tasks",
                                checked = highPriority,
                                onCheckedChange = { highPriority = it }
                            )
                        }
                    }

                    // --------------------------------------------------------
                    // 1. STORAGE & MEDIA
                    // --------------------------------------------------------
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Main Hard Disk Card
                        EditSectionCard(title = "Primary Virtual Disk", icon = Icons.Filled.Storage, accent = RedBoxAmber) {
                            if (diskImageName.isNotBlank()) {
                                Surface(
                                    color = Color(0x22FFA000),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x44FFA000)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Storage, null, tint = RedBoxAmber, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(diskImageName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Attached Hard Drive", color = RedBoxTextSecondary, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = {
                                            diskImage = ""
                                            diskImageName = ""
                                        }) {
                                            Icon(Icons.Filled.Close, "Eject", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { diskPicker.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedBoxAmber)
                                ) {
                                    Icon(Icons.Filled.Folder, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("SELECT DISK", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }

                                OutlinedButton(
                                    onClick = { showCreateDiskDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("NEW DISK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }

                        // Disk Interface Controller
                        EditSectionCard(title = "Disk Controller Interface", icon = Icons.Filled.Tune, accent = RedBoxCyan) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectChip(
                                    label = "AHCI (SATA)",
                                    selected = diskInterface == "AHCI",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) { diskInterface = "AHCI" }

                                SelectChip(
                                    label = "IDE (Legacy)",
                                    selected = diskInterface == "IDE",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) {
                                    diskInterface = "IDE"
                                    if (machineType == "q35") machineType = "pc"
                                }

                                SelectChip(
                                    label = "VirtIO Block",
                                    selected = diskInterface == "VirtIO Block",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) { diskInterface = "VirtIO Block" }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (diskInterface) {
                                    "VirtIO Block" -> "VirtIO: Fastest I/O speeds (requires VirtIO guest driver ISO during setup)."
                                    "IDE" -> "IDE: Native compatibility for Windows 95, 98, XP."
                                    else -> "AHCI: Standard SATA controller for Windows 7, 10, 11 & Linux."
                                },
                                fontSize = 11.sp,
                                color = RedBoxTextMuted
                            )
                        }

                        // Installation ISO Media (CD-ROM 1)
                        EditSectionCard(title = "Installation ISO (CD-ROM 1)", icon = Icons.Filled.Album, accent = RedBoxPurple) {
                            if (isoImageName.isNotBlank()) {
                                Surface(
                                    color = Color(0x22B388FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x44B388FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Album, null, tint = RedBoxPurple, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(isoImageName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Mounted Optical Disc", color = RedBoxTextSecondary, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = {
                                            isoImage = ""
                                            isoImageName = ""
                                        }) {
                                            Icon(Icons.Filled.Close, "Eject", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Button(
                                onClick = { isoPicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedBoxPurple)
                            ) {
                                Icon(Icons.Filled.Folder, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isoImageName.isNotBlank()) "CHANGE ISO IMAGE" else "MOUNT ISO IMAGE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Driver ISO (CD-ROM 2 / VirtIO Drivers)
                        EditSectionCard(title = "VirtIO Driver ISO (CD-ROM 2)", icon = Icons.Filled.DeveloperBoard, accent = RedBoxCyan) {
                            if (driverIsoImageName.isNotBlank()) {
                                Surface(
                                    color = Color(0x2200E5FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x4400E5FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.DeveloperBoard, null, tint = RedBoxCyan, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(driverIsoImageName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("VirtIO Driver Disc", color = RedBoxTextSecondary, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = {
                                            driverIsoImage = ""
                                            driverIsoImageName = ""
                                        }) {
                                            Icon(Icons.Filled.Close, "Eject", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Button(
                                onClick = { driverIsoPicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007585))
                            ) {
                                Icon(Icons.Filled.Folder, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (driverIsoImageName.isNotBlank()) "CHANGE DRIVER ISO" else "SELECT VIRTIO DRIVERS ISO", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Shared Storage Options
                        EditSectionCard(title = "Host File Sharing & Second Drive", icon = Icons.Filled.Share, accent = RedBoxStatusOnline) {
                            ToggleRow(
                                title = "RedBox Shared Folder (FAT)",
                                subtitle = "Exports files from Android to a virtual FAT drive inside Windows",
                                checked = sharedFolderEnabled,
                                onCheckedChange = { sharedFolderEnabled = it }
                            )

                            if (sharedFolderEnabled) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { sharedFolderFilePicker.launch(arrayOf("*/*")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF144D31))
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("IMPORT FILE TO SHARED FOLDER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = Color(0x18FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                            Text("Secondary Hard Disk Image:", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            if (sharedDiskImageName.isNotBlank()) {
                                Text(sharedDiskImageName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                            }
                            Button(
                                onClick = { sharedDiskPicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF233045))
                            ) {
                                Icon(Icons.Filled.Storage, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (sharedDiskImageName.isNotBlank()) "CHANGE SECONDARY DRIVE" else "ATTACH SECONDARY DRIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --------------------------------------------------------
                    // 2. DISPLAY & AUDIO
                    // --------------------------------------------------------
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Display Adapter Card
                        EditSectionCard(title = "Emulated Graphics Adapter", icon = Icons.Filled.Tv, accent = RedBoxPurple) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Standard VGA", "Bochs Display", "VirtIO VGA", "Cirrus VGA").forEach { adapter ->
                                    SelectChip(
                                        label = adapter,
                                        selected = displayAdapter == adapter,
                                        modifier = Modifier.fillMaxWidth(),
                                        accent = RedBoxPurple
                                    ) {
                                        displayAdapter = adapter
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = when (displayAdapter) {
                                    "VirtIO VGA" -> "VirtIO VGA: High performance adapter. Supports 3D hardware acceleration via VirGL."
                                    "Bochs Display" -> "Bochs: Lightweight VBE adapter supporting arbitrary screen resolutions."
                                    "Cirrus VGA" -> "Cirrus: Recommended for older Windows 95, 98, and NT."
                                    else -> "Standard VGA: Default VESA graphics device compatible with all operating systems."
                                },
                                fontSize = 11.sp,
                                color = RedBoxTextMuted
                            )

                            if (displayAdapter == "VirtIO VGA") {
                                HorizontalDivider(color = Color(0x18FFFFFF), modifier = Modifier.padding(vertical = 10.dp))
                                ToggleRow(
                                    title = "VirGL 3D Hardware Acceleration",
                                    subtitle = "Routes OpenGL guest calls to the host Android GPU via libvirglrenderer",
                                    checked = threeDAcceleration,
                                    onCheckedChange = { threeDAcceleration = it }
                                )
                            }
                        }

                        // Sound Card Card
                        EditSectionCard(title = "Emulated Sound Card", icon = Icons.Filled.VolumeUp, accent = RedBoxCyan) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectChip(
                                    label = "Intel HDA",
                                    selected = soundCard == "Intel HDA",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) { soundCard = "Intel HDA" }

                                SelectChip(
                                    label = "AC97",
                                    selected = soundCard == "AC97",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) { soundCard = "AC97" }

                                SelectChip(
                                    label = "Sound Blaster 16",
                                    selected = soundCard == "Sound Blaster 16",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxCyan
                                ) { soundCard = "Sound Blaster 16" }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (soundCard) {
                                    "AC97" -> "AC97: Broad legacy audio support for Windows XP & 2000."
                                    "Sound Blaster 16" -> "SB16: Essential for classic DOS games & Windows 95/98."
                                    else -> "Intel HDA: High definition audio for Windows 7, 10, 11 & Linux."
                                },
                                fontSize = 11.sp,
                                color = RedBoxTextMuted
                            )
                        }
                    }

                    // --------------------------------------------------------
                    // 3. SYSTEM, NETWORK & ADVANCED
                    // --------------------------------------------------------
                    3 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Boot & Firmware
                        EditSectionCard(title = "Firmware & Boot Priority", icon = Icons.Filled.SettingsSuggest, accent = RedBoxAmber) {
                            Text("Boot Priority Order:", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectChip(
                                    label = "Hard Disk First",
                                    selected = bootPriority == "Hard Disk First",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) { bootPriority = "Hard Disk First" }

                                SelectChip(
                                    label = "CD/DVD ISO First",
                                    selected = bootPriority == "CD/DVD ISO First",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) { bootPriority = "CD/DVD ISO First" }
                            }

                            HorizontalDivider(color = Color(0x18FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                            Text("Firmware Mode:", fontSize = 11.sp, color = RedBoxTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectChip(
                                    label = "Legacy BIOS (SeaBIOS)",
                                    selected = firmwareMode == "Legacy BIOS",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) { firmwareMode = "Legacy BIOS" }

                                SelectChip(
                                    label = "UEFI (EDK2)",
                                    selected = firmwareMode == "UEFI (EDK2)",
                                    modifier = Modifier.weight(1f),
                                    accent = RedBoxAmber
                                ) { firmwareMode = "UEFI (EDK2)" }
                            }
                        }

                        // Network Interface Card
                        EditSectionCard(title = "Virtual Networking (NAT)", icon = Icons.Filled.Wifi, accent = RedBoxStatusOnline) {
                            ToggleRow(
                                title = "Enable Network Adapter",
                                subtitle = "User-mode SLIRP NAT connection sharing your device internet",
                                checked = networkEnabled,
                                onCheckedChange = { networkEnabled = it }
                            )

                            if (networkEnabled) {
                                Spacer(Modifier.height(10.dp))
                                Text("Virtual NIC Device:", fontSize = 11.sp, color = RedBoxTextSecondary)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf("Realtek RTL8139", "Intel E1000", "Intel E1000E", "AMD PCnet").forEach { nic ->
                                        SelectChip(
                                            label = nic.replace("Realtek ", "").replace("Intel ", ""),
                                            selected = networkAdapter == nic,
                                            modifier = Modifier.weight(1f),
                                            accent = RedBoxStatusOnline
                                        ) { networkAdapter = nic }
                                    }
                                }
                            }
                        }

                        // Guest Date / RTC
                        EditSectionCard(title = "Guest RTC / BIOS Date", icon = Icons.Filled.Schedule, accent = RedBoxCyan) {
                            OutlinedTextField(
                                value = biosDate,
                                onValueChange = { biosDate = it },
                                label = { Text("Date (e.g. 2005-01-01 or Default)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = RedBoxCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Overrides the virtual clock. Helpful for installing legacy Windows versions.", fontSize = 10.sp, color = RedBoxTextMuted)
                        }

                        // Advanced QEMU Parameters
                        EditSectionCard(title = "Custom QEMU Parameters", icon = Icons.Filled.Code, accent = RedBoxCrimson) {
                            OutlinedTextField(
                                value = qemuParams,
                                onValueChange = { qemuParams = it },
                                label = { Text("Extra command line arguments (e.g. -usb -device ...)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = RedBoxCrimson,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            if (qemuParams.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { qemuParams = "" }) {
                                    Text("CLEAR ARGUMENTS", color = RedBoxCrimson, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// MODULAR REUSABLE UI COMPONENTS FOR EDIT VM
// ============================================================================

@Composable
private fun EditSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = RedBoxSurface,
        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = RedBoxCrimson,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else Color(0x14FFFFFF),
        border = BorderStroke(1.dp, if (selected) accent else Color(0x1FFFFFFF))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                color = if (selected) Color.White else RedBoxTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = RedBoxTextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RedBoxCrimson,
                uncheckedThumbColor = RedBoxTextSecondary,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

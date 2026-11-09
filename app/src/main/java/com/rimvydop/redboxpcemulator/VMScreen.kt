package com.rimvydop.redboxpcemulator

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import com.rimvydop.redboxpcemulator.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMScreen(
    vm: VMModel,
    onStop: () -> Unit,
    onBack: () -> Unit
) {

    /*
     * STEP 13C:
     * Tell MainActivity when the VM screen is actually visible.
     * This does not intercept or modify any SDL touch events.
     */
    val redBoxContext = LocalContext.current
    val redBoxActivity = redBoxContext as? MainActivity

    DisposableEffect(redBoxActivity) {
        redBoxActivity?.setRedBoxVmScreenVisible(true)

        onDispose {
            redBoxActivity?.setRedBoxVmScreenVisible(false)
        }
    }

    /*
     * v0.2.0 Stage 2D.1:
     * Running a VM opens directly into the immersive VM experience.
     */
    var isFullscreen by remember {
        mutableStateOf(true)
    }

    var showFullscreenToolbar by remember {
        mutableStateOf(true)
    }

    var showRuntimeSettings by remember {
        mutableStateOf(false)
    }

    // RedBox v0.2.1 - increments whenever the user requests a display refresh.
    var displayRefreshRequest by remember {
        mutableStateOf(0)
    }

    var preferredDisplayFps by remember { mutableStateOf(0) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showShutdownConfirmation by remember { mutableStateOf(false) }

    /*
     * v0.2.0 Mouse Pointer v2
     *
     * The VM display now has an explicit mouse mode. When enabled, the
     * transparent Android input layer behaves like a laptop touchpad:
     * every finger-down starts a fresh relative movement sequence, so the
     * user can lift, reposition, and continue moving the guest pointer.
     */
    var mouseModeEnabled by remember {
        mutableStateOf(true)
    }

    /*
     * RedBox v0.2.1 - Mouse sensitivity.
     * Normal (1.0x) preserves the v0.2.0 mouse behavior.
     */
    var mouseSensitivity by remember {
        mutableStateOf(1.0f)
    }

    /*
     * Mouse v4 - Stage 2:
     * Keep relative-touch state across Compose recompositions.
     * The previous implementation used ordinary local variables inside the
     * display Box; those could reset while a swipe was still active.
     */
    var touchLastX by remember {
        mutableStateOf(0f)
    }
    var touchLastY by remember {
        mutableStateOf(0f)
    }
    var touchActive by remember {
        mutableStateOf(false)
    }
    var touchMoved by remember {
        mutableStateOf(false)
    }

    /*
     * Mouse v4 - Stage 1
     *
     * Mouse mode now means the SDL ExSDLSurface owns Android touch input.
     * VMScreen no longer places a Compose gesture layer over the VM display.
     */
    val view = LocalView.current

    /*
     * STEP 13A.3
     *
     * IMPORTANT:
     * VMDisplay is composed ONCE at one stable location below.
     *
     * Fullscreen no longer moves, replaces, reparents, or recreates the
     * SDL ExSDLSurface. We only change the layout around the exact same
     * AndroidView.
     *
     * This is safer for SDL/QEMU because its native display connection
     * remains attached to one Surface for the whole VM session.
     */
    DisposableEffect(isFullscreen, view) {
        val activity = view.context as? Activity
        val window = activity?.window

        if (window != null) {
            val controller =
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )

            if (isFullscreen) {
                // Part 2M.1: true edge-to-edge fullscreen, including display
                // cutout/notch areas, around the SAME persistent SDL Surface.
                WindowCompat.setDecorFitsSystemWindows(
                    window,
                    false
                )

                window.addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )

                if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.P
                ) {
                    window.attributes =
                        window.attributes.apply {
                            layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams
                                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                }

                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                controller.hide(
                    WindowInsetsCompat.Type.systemBars()
                )

                Log.d(
                    "RedBoxDisplay",
                    "Part 2M immersive fullscreen enabled; persistent SDL Surface preserved"
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )

                WindowCompat.setDecorFitsSystemWindows(
                    window,
                    true
                )

                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )
            }
        }

        onDispose {
            if (window != null && isFullscreen) {
                val controller =
                    WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    )

                WindowCompat.setDecorFitsSystemWindows(
                    window,
                    true
                )

                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        onBack()
    }

    if (showRuntimeSettings) {
        AlertDialog(
            onDismissRequest = { showRuntimeSettings = false },
            title = { Text("Running VM Settings") },
            text = {
                Column {
                    Text(text = vm.name, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("RAM: ${vm.ram}")
                    Text("CPU cores: ${vm.cpuCores}")
                    Text("CPU model: ${vm.cpuModel}")
                    if (vm.cpuFlags.isNotBlank()) {
                        Text("CPU flags: ${vm.cpuFlags}")
                    }
                    Text("Display: ${vm.displayAdapter}")
                    Text(
                        if (vm.networkEnabled) {
                            "Network: ${vm.networkAdapter}"
                        } else {
                            "Network: Disabled"
                        }
                    )
                    Text("Sound: ${vm.soundCard}")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mouse sensitivity",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Low" to 0.6f,
                            "Normal" to 1.0f,
                            "High" to 1.6f
                        ).forEach { (label, value) ->
                            if (mouseSensitivity == value) {
                                Button(
                                    onClick = { mouseSensitivity = value },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { mouseSensitivity = value },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hardware settings are locked while the VM is running. Stop the VM, then use Edit VM to change them.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRuntimeSettings = false }) {
                    Text("Close")
                }
            }
        )
    }

    RedBoxMaterialTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isFullscreen) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                )
        ) {
            /*
             * Normal top bar disappears in fullscreen.
             *
             * This sibling can appear/disappear safely because VMDisplay
             * itself stays at the same call site below.
             */
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = vm.name,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "VM Screen",
                                fontSize = 12.sp,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Text(
                                text = "‹",
                                fontSize = 30.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                SDLActivity.showRedBoxKeyboard()
                            }
                        ) {
                            Text(
                                text = "⌨",
                                fontSize = 21.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                isFullscreen = true
                            }
                        ) {
                            Text(
                                text = "⛶",
                                fontSize = 22.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
                )
            }

            /*
             * THIS IS THE ONLY VMDisplay IN THIS FILE.
             *
             * In normal mode it is 430dp tall.
             * In fullscreen it expands to all remaining screen space.
             * The AndroidView / ExSDLSurface itself remains mounted.
             */
            Box(
                modifier =
                    if (isFullscreen) {
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(Color.Black)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                            .padding(
                                start = 14.dp,
                                end = 14.dp,
                                top = 14.dp
                            )
                            .background(Color.Black)
                    }
            ) {
                VMDisplay(
                    refreshRequest = displayRefreshRequest,
                    preferredFrameRate = preferredDisplayFps,
                    modifier =
                        if (isFullscreen) {
                            Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    onSurfaceReady = {
                        Log.d(
                            "RedBoxDisplay",
                            "Persistent SDL VM Surface ready"
                        )
                    },
                    onSurfaceDestroyed = {
                        Log.d(
                            "RedBoxDisplay",
                            "Persistent SDL VM Surface destroyed"
                        )
                    }
                )

                /*
                 * Mouse v4 - Stage 2: relative touchpad input.
                 *
                 * RedBox runs QEMU/SDL in the isolated :qemu process, so the
                 * UI-process SDL touch callback cannot move the guest pointer.
                 * This layer therefore translates Android finger movement into
                 * relative deltas and sends those deltas through RedBox's
                 * existing IPC/native QEMU mouse path.
                 *
                 * Important stability change from the old implementation:
                 * touch state is Compose state remembered across recompositions,
                 * rather than local variables recreated whenever this Box
                 * recomposes (for example when the toolbar changes).
                 */
                if (mouseModeEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                        touchLastX = event.x
                                        touchLastY = event.y
                                        touchActive = true
                                        touchMoved = false
                                        true
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)

                                        if (!touchActive) {
                                            touchLastX = event.x
                                            touchLastY = event.y
                                            touchActive = true
                                            return@pointerInteropFilter true
                                        }

                                        for (historyIndex in 0 until event.historySize) {
                                            val historicalX =
                                                event.getHistoricalX(historyIndex)
                                            val historicalY =
                                                event.getHistoricalY(historyIndex)

                                            val dx = historicalX - touchLastX
                                            val dy = historicalY - touchLastY

                                            if (dx != 0f || dy != 0f) {
                                                touchMoved = true
                                                redBoxActivity?.moveRedBoxMouse(dx * mouseSensitivity, dy * mouseSensitivity)
                                            }

                                            touchLastX = historicalX
                                            touchLastY = historicalY
                                        }

                                        val dx = event.x - touchLastX
                                        val dy = event.y - touchLastY

                                        if (dx != 0f || dy != 0f) {
                                            touchMoved = true
                                            redBoxActivity?.moveRedBoxMouse(dx * mouseSensitivity, dy * mouseSensitivity)
                                        }

                                        touchLastX = event.x
                                        touchLastY = event.y
                                        true
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        // A short tap acts as a left click; a swipe only moves the pointer.
                                        if (touchActive && !touchMoved) {
                                            redBoxActivity?.clickRedBoxMouse()
                                        }
                                        touchActive = false
                                        touchMoved = false
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                        true
                                    }

                                    MotionEvent.ACTION_CANCEL -> {
                                        touchActive = false
                                        touchMoved = false
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                        true
                                    }

                                    else -> true
                                }
                            }
                    )
                }

                if (isFullscreen) {
                    RedBoxVmAnimatedToolbar(visible = showFullscreenToolbar) {
                        Card(
                            modifier = Modifier
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xF20F131C)
                            ),
                            border = BorderStroke(1.dp, RedBoxGlassBorderGradient)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                /*
                                 * Small RedBox identity mark.
                                 */
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .size(26.dp)
                                        .background(
                                            color = RedBoxCrimson,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                RedBoxToolbarButton(
                                    icon = "⌨",
                                    label = "Keys",
                                    onClick = {
                                        SDLActivity.showRedBoxKeyboard()
                                    }
                                )

                                RedBoxToolbarButton(
                                    icon = if (mouseModeEnabled) "●" else "○",
                                    label =
                                        if (mouseModeEnabled) {
                                            "Mouse"
                                        } else {
                                            "Mouse Off"
                                        },
                                    active = mouseModeEnabled,
                                    onClick = {
                                        mouseModeEnabled = !mouseModeEnabled
                                    }
                                )

                                RedBoxToolbarButton(
                                    icon = "⚙",
                                    label = "Settings",
                                    onClick = {
                                        showRuntimeSettings = true
                                    }
                                )

                                Box {
                                    RedBoxToolbarButton(
                                        icon = "FPS",
                                        label = if (preferredDisplayFps == 0) "FPS Auto" else "${preferredDisplayFps} FPS",
                                        onClick = { showFpsMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showFpsMenu,
                                        onDismissRequest = { showFpsMenu = false }
                                    ) {
                                        listOf(0, 30, 60, 90, 120).forEach { fps ->
                                            DropdownMenuItem(
                                                text = { Text(if (fps == 0) "Auto (system)" else "$fps FPS") },
                                                leadingIcon = {
                                                    if (preferredDisplayFps == fps) {
                                                        Icon(Icons.Filled.Check, contentDescription = null)
                                                    }
                                                },
                                                onClick = {
                                                    preferredDisplayFps = fps
                                                    showFpsMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                RedBoxToolbarButton(
                                    icon = "↻",
                                    label = "Refresh",
                                    onClick = {
                                        displayRefreshRequest++
                                        Log.d(
                                            "RedBoxDisplay",
                                            "Manual display refresh requested"
                                        )
                                    }
                                )

                                RedBoxToolbarButton(
                                    icon = "⏹",
                                    label = "Stop",
                                    danger = true,
                                    onClick = {
                                        showShutdownConfirmation = true
                                    }
                                )

                                RedBoxToolbarButton(
                                    icon = "‹",
                                    label = "RedBox",
                                    onClick = onBack
                                )

                                RedBoxToolbarButton(
                                    icon = "⌃",
                                    label = "Hide",
                                    onClick = {
                                        showFullscreenToolbar = false
                                    }
                                )
                            }
                        }
                    }
                    if (!showFullscreenToolbar) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .clickable {
                                    showFullscreenToolbar = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xF00F131C)
                            ),
                            border = BorderStroke(1.dp, RedBoxGlassBorderGradient)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(RedBoxStatusOnline)
                                )

                                Text(
                                    text = "${vm.name} • RUNNING",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = "▾",
                                    color = RedBoxTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (showShutdownConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showShutdownConfirmation = false },
                        shape = RoundedCornerShape(22.dp),
                        containerColor = RedBoxSurface,
                        tonalElevation = 16.dp,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = RedBoxCrimson,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Shutdown Machine?",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        },
                        text = {
                            Text(
                                text = "Request ACPI shutdown for '${vm.name}'? Make sure guest work has been saved.",
                                color = RedBoxTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showShutdownConfirmation = false
                                    onStop()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedBoxCrimson),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("SHUTDOWN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showShutdownConfirmation = false }) {
                                Text("CANCEL", color = RedBoxTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                }
            }

            /*
             * Normal controls disappear in fullscreen, but the SDL display
             * above is untouched.
             */
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "VM Controls",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(9.dp)
                    ) {
                        VMControlCard(
                            modifier = Modifier.weight(1f),
                            icon = "⌨",
                            title = "Keyboard",
                            onClick = {
                                SDLActivity.showRedBoxKeyboard()
                            }
                        )

                        VMControlCard(
                            modifier = Modifier.weight(1f),
                            icon = if (mouseModeEnabled) "●" else "○",
                            title = if (mouseModeEnabled) "Mouse On" else "Mouse Off",
                            onClick = {
                                mouseModeEnabled = !mouseModeEnabled
                            }
                        )

                        VMControlCard(
                            modifier = Modifier.weight(1f),
                            icon = "⛶",
                            title = "Fullscreen",
                            onClick = {
                                isFullscreen = true
                            }
                        )

                        VMControlCard(
                            modifier = Modifier.weight(1f),
                            icon = "⚙",
                            title = "Settings",
                            onClick = {
                                showRuntimeSettings = true
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Button(
                        onClick = { showShutdownConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBoxCrimson
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SHUTDOWN VIRTUAL MACHINE",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            )
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun RedBoxToolbarButton(
    icon: String,
    label: String,
    active: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val vector = when (label) {
        "Keys" -> Icons.Filled.Keyboard
        "Mouse", "Mouse Off" -> Icons.Filled.Mouse
        "Settings" -> Icons.Filled.Settings
        "Refresh" -> Icons.Filled.Refresh
        "FPS Auto", "30 FPS", "60 FPS", "90 FPS", "120 FPS" -> Icons.Filled.Tune
        "RedBox" -> Icons.Filled.ArrowBack
        "Hide" -> Icons.Filled.KeyboardArrowUp
        "Stop" -> Icons.Filled.Close
        else -> Icons.Filled.Tune
    }
    val buttonBg = when {
        danger -> Color(0xFF4A0E17)
        active -> Color(0xFF6E2332)
        else -> Color(0xFF1E2330)
    }
    val iconTint = when {
        danger -> Color(0xFFFF5268)
        active -> Color(0xFFFF9EAB)
        else -> Color.White
    }
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = buttonBg,
            border = BorderStroke(1.dp, if (danger) Color(0x66FF2A42) else Color(0x18FFFFFF))
        ) {
            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                Icon(
                    vector,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (danger) Color(0xFFFF8A99) else Color(0xFFC5CEE0),
            fontSize = 9.sp,
            fontWeight = if (active || danger) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun VMControlCard(
    modifier: Modifier,
    icon: String,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape =
            androidx.compose.foundation.shape.RoundedCornerShape(
                16.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C202A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

// Kept outside ColumnScope so Compose selects the correct AnimatedVisibility overload.
@Composable
private fun RedBoxVmAnimatedToolbar(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(230)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(animationSpec = tween(170)) + scaleOut(targetScale = 0.92f)
        ) {
            content()
        }
    }
}

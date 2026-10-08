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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                                                redBoxActivity?.moveRedBoxMouse(dx * mouseSensitivity, dy * mouseSensitivity)
                                            }

                                            touchLastX = historicalX
                                            touchLastY = historicalY
                                        }

                                        val dx = event.x - touchLastX
                                        val dy = event.y - touchLastY

                                        if (dx != 0f || dy != 0f) {
                                            redBoxActivity?.moveRedBoxMouse(dx * mouseSensitivity, dy * mouseSensitivity)
                                        }

                                        touchLastX = event.x
                                        touchLastY = event.y
                                        true
                                    }

                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> {
                                        touchActive = false
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                        true
                                    }

                                    else -> true
                                }
                            }
                    )
                }

                if (isFullscreen) {
                    if (showFullscreenToolbar) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    16.dp
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xE6101216)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 5.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                /*
                                 * Small RedBox identity mark. It is intentionally
                                 * non-clickable so it cannot steal VM controls.
                                 */
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .width(24.dp)
                                        .height(24.dp)
                                        .background(
                                            color = Color(0xFFE53935),
                                            shape =
                                                androidx.compose.foundation.shape.RoundedCornerShape(
                                                    7.dp
                                                )
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
                    } else {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                                .clickable {
                                    showFullscreenToolbar = true
                                },
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    12.dp
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xD9101216)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 5.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(18.dp)
                                        .background(
                                            color = Color(0xFFE53935),
                                            shape =
                                                androidx.compose.foundation.shape.RoundedCornerShape(
                                                    5.dp
                                                )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Text(
                                    text = "RedBox  ▾",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
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
                        onClick = onStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFB3261E)
                        )
                    ) {
                        Text(
                            text = "Stop VM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            color =
                if (active) {
                    Color(0xFFFF6B66)
                } else {
                    Color.White
                },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = label,
            color =
                if (active) {
                    Color(0xFFFFB4B0)
                } else {
                    Color(0xFFE7E7E7)
                },
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
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
            containerColor =
                MaterialTheme.colorScheme.surface
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

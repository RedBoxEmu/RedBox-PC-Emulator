package com.rimvydop.redboxpcemulator

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.core.animateDpAsState
import android.view.KeyEvent
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.Icon
import android.net.Uri
import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.res.Configuration
import android.provider.OpenableColumns
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.widget.Toast
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import org.libsdl.app.SDLActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.PendingPurchasesParams

private object RedBoxSupportLog {
    private const val MAX_ENTRIES = 250
    private val entries = mutableListOf<String>()

    @Synchronized
    fun add(message: String) {
        val time = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        ).format(Date())

        val safeMessage = message
            .replace(Regex("""content://\S+"""), "[content-uri]")
            .replace(Regex("""file://\S+"""), "[file-uri]")

        entries.add("$time  $safeMessage")
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }

        Log.d("RedBoxSupport", safeMessage)
    }

    @Synchronized
    fun clear() {
        entries.clear()
        add("Support log cleared")
    }

    @Synchronized
    fun buildReport(): String {
        val body =
            if (entries.isEmpty()) {
                "No support events recorded yet."
            } else {
                entries.joinToString("\n")
            }

        return buildString {
            appendLine("RedBox PC Emulator Support Log")
            appendLine("App version: 1.1.0")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
            appendLine()
            appendLine("Privacy note: RedBox does not intentionally include full disk/ISO content URIs in this report.")
            appendLine()
            append(body)
        }
    }
}

class MainActivity : SDLActivity() {

    private val donateProductId = "donate_4_99"
    private var billingClient: BillingClient? = null
    private var donateProductDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { purchase ->
                    if (purchase.products.contains(donateProductId) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        consumeDonationPurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d("RedBoxBilling", "Donation purchase canceled")
            }
            else -> {
                Log.e("RedBoxBilling", "Purchase update failed: ${billingResult.debugMessage}")
                Toast.makeText(this, "Google Play purchase failed: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Volatile
    private var redBoxVmScreenVisible = false

    @Volatile
    private var qemuServiceBound = false

    private var qemuServiceMessenger: Messenger? = null

    /*
     * Part 2L.1: keep the Compose start callback until the isolated VM
     * session actually ends, so isVMRunning is released after Stop.
     */
    private var qemuSessionEndedCallback: ((String) -> Unit)? = null

    private val qemuReplyHandler =
        object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(message: Message) {
                val status =
                    message.data.getString(
                        QemuIpc.KEY_STATUS
                    ) ?: ""

                Log.d(
                    "RedBoxQemuIPC",
                    "Reply from :qemu process: what=${message.what}, status=$status"
                )

                when (message.what) {
                    QemuIpc.MSG_VM_STARTING,
                    QemuIpc.MSG_VM_RUNNING,
                    QemuIpc.MSG_VM_STOPPING -> {
                        // The isolated QEMU process is still active.
                    }

                    QemuIpc.MSG_VM_STOPPED -> {
                        qemuProcessActive.set(false)

                        val callback = qemuSessionEndedCallback
                        qemuSessionEndedCallback = null
                        callback?.invoke(status)

                        Log.d(
                            "RedBoxQemuIPC",
                            "Isolated QEMU VM stopped; UI state + process guard released"
                        )
                    }

                    QemuIpc.MSG_VM_ERROR -> {
                        qemuProcessActive.set(false)

                        val callback = qemuSessionEndedCallback
                        qemuSessionEndedCallback = null
                        callback?.invoke(status)

                        Log.e(
                            "RedBoxQemuIPC",
                            "Isolated QEMU VM reported an error: $status"
                        )
                    }

                    else -> {
                        super.handleMessage(message)
                    }
                }
            }
        }

    private val qemuReplyMessenger =
        Messenger(qemuReplyHandler)

    private val qemuServiceConnection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                qemuServiceMessenger =
                    if (service != null) {
                        Messenger(service)
                    } else {
                        null
                    }

                qemuServiceBound =
                    qemuServiceMessenger != null

                Log.d(
                    "RedBoxQemuIPC",
                    "Connected to :qemu service. UI PID=${android.os.Process.myPid()}"
                )


            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {
                qemuServiceMessenger = null
                qemuServiceBound = false
                qemuProcessActive.set(false)

                // Part 2L.1 fallback if the old :qemu PID dies before its
                // MSG_VM_STOPPED Binder reply reaches the UI process.
                val callback = qemuSessionEndedCallback
                qemuSessionEndedCallback = null
                callback?.invoke("QEMU VM stopped")

                Log.d(
                    "RedBoxQemuIPC",
                    ":qemu service disconnected; UI state + process guard released"
                )

                // The old :qemu PID was intentionally killed by Part 2L.
                // Ask Android for a fresh isolated service process now.
                mainHandler.postDelayed(
                    {
                        if (!isFinishing && !isDestroyed && !qemuServiceBound) {
                            Log.d(
                                "RedBoxQemuIPC",
                                "Part 2L.2: rebinding fresh :qemu service after process death"
                            )
                            bindFreshQemuService()
                        }
                    },
                    250L
                )
            }
        }

    /*
     * RedBox Part 2L.2:
     * Explicitly recreate/rebind the isolated :qemu service after Part 2L
     * deliberately terminates the old QEMU process.
     */
    private fun bindFreshQemuService() {
        if (qemuServiceBound) {
            return
        }

        val qemuServiceIntent =
            Intent(
                this,
                QemuService::class.java
            )

        val bindStarted =
            bindService(
                qemuServiceIntent,
                qemuServiceConnection,
                Context.BIND_AUTO_CREATE
            )

        Log.d(
            "RedBoxQemuIPC",
            "Part 2L.2 bindService(:qemu) returned $bindStarted. UI PID=${android.os.Process.myPid()}"
        )
    }

    fun setRedBoxVmScreenVisible(visible: Boolean) {
        redBoxVmScreenVisible = visible
    }

    /*
     * STEP 12D Part 5E:
     * Tell SDLActivity exactly which native libraries RedBox uses.
     *
     * SDLActivity.onCreate() will now run SDL.setupJNI(), SDL.initialize(),
     * and SDL.setContext(this) before RedBox starts QEMU.
     */
    override fun getLibraries(): Array<String> {
        return arrayOf(
            "SDL2",
            "compat-SDL2-ext",
            "redboxpcemulator",
            "qemu-system-x86_64"
        )
    }

    private val limboFileDescriptors =
        mutableMapOf<Int, ParcelFileDescriptor>()

    companion object {
        /*
         * STEP 13A.1:
         * QEMU is process-level native state. Android rotation must never
         * start a second qemu_init() while the current VM is still alive.
         */
        private val qemuProcessActive =
            AtomicBoolean(false)

        init {
            System.loadLibrary("SDL2")
            Log.d(
                "RedBoxQEMU",
                "Android loaded libSDL2.so"
            )

            System.loadLibrary("compat-SDL2-ext")
            Log.d(
                "RedBoxQEMU",
                "Android loaded libcompat-SDL2-ext.so"
            )

            System.loadLibrary("redboxpcemulator")
            Log.d(
                "RedBoxQEMU",
                "Android loaded libredboxpcemulator.so"
            )

            try {
                System.loadLibrary("qemu-system-x86_64")
                Log.d(
                    "RedBoxQEMU",
                    "Android loaded libqemu-system-x86_64.so"
                )
            } catch (error: UnsatisfiedLinkError) {
                Log.e(
                    "RedBoxQEMU",
                    "Android could not load libqemu-system-x86_64.so",
                    error
                )
            }
        }

        /*
         * Called from Limbo's SDL compatibility library when QEMU changes
         * the guest display resolution. JNI expects this exact STATIC method:
         * MainActivity.onVMResolutionChanged(int width, int height).
         */
        @JvmStatic
        fun onVMResolutionChanged(width: Int, height: Int) {
            Log.d(
                "RedBoxDisplay",
                "VM resolution changed by SDL/QEMU: ${width}x${height}"
            )
        }
    }

    private external fun stringFromJNI(): String
    private external fun nativeQemuStatus(): String
    private external fun nativeQemuStop(): Boolean
    private external fun nativeQemuStart(
        ramMb: Int,
        cpuCores: Int,
        diskUri: String,
        diskImageName: String,
        isoUri: String,
        driverIsoUri: String,
        sharedDiskUri: String,
        sharedDiskImageName: String,
        sharedFolderEnabled: Boolean,
        cpuModel: String,
        cpuFlags: String,
        tcgCacheMb: Int,
        multiThreadedTcg: Boolean,
        machineType: String,
        diskInterface: String,
        displayAdapter: String,
        threeDAcceleration: Boolean,
        networkEnabled: Boolean,
        networkAdapter: String,
        networkMode: String,
        qemuParams: String,
        biosDate: String,
        soundCard: String,
        bootPriority: String,
        firmwareMode: String
    ): String

    /*
     * STEP 13C:
     * Direct QEMU mouse button injection.
     *
     * button:
     *   0 = left
     *   1 = middle
     *   2 = right
     *
     * down:
     *   true  = press
     *   false = release
     */
    private external fun nativeQemuMouseButton(
        button: Int,
        down: Boolean
    ): Boolean

    private external fun nativeQemuMouseMove(
        deltaX: Int,
        deltaY: Int
    ): Boolean

    fun moveRedBoxMouse(
        deltaX: Float,
        deltaY: Float
    ) {
        if (!qemuProcessActive.get()) {
            return
        }

        // STEP 13C.4: make the virtual touchpad feel quicker.
        // 1.5x means the guest cursor travels farther than the finger.
        val touchpadSensitivity = 1.5f

        val dx = (deltaX * touchpadSensitivity).toInt()
        val dy = (deltaY * touchpadSensitivity).toInt()

        if (dx == 0 && dy == 0) {
            return
        }

        val serviceMessenger = qemuServiceMessenger
        if (!qemuServiceBound || serviceMessenger == null) {
            return
        }

        try {
            val message =
                Message.obtain(
                    null,
                    QemuIpc.MSG_MOUSE_MOVE
                ).apply {
                    data = Bundle().apply {
                        putInt(QemuIpc.KEY_MOUSE_DX, dx)
                        putInt(QemuIpc.KEY_MOUSE_DY, dy)
                    }
                }

            serviceMessenger.send(message)
        } catch (error: RemoteException) {
            Log.e(
                "RedBoxQemuIPC",
                "Failed to send mouse movement to :qemu process",
                error
            )
        }
    }

    private external fun nativeSetDisplaySurface(
        surface: android.view.Surface?
    )

    /*
     * RedBox Part 2M.2:
     * Send the Android Surface AND its current geometry to the isolated
     * :qemu process. This lets SDL receive a real resize event when Compose
     * changes the VM display between normal and fullscreen layouts.
     */
    fun setQemuDisplaySurface(
        surface: android.view.Surface?,
        width: Int = 0,
        height: Int = 0,
        pixelFormat: Int = 0,
        refreshRate: Float = 60f
    ) {
        val serviceMessenger = qemuServiceMessenger

        if (!qemuServiceBound || serviceMessenger == null) {
            Log.w(
                "RedBoxQemuIPC",
                "Cannot send display Surface: :qemu service is not connected"
            )
            return
        }

        try {
            val message =
                if (surface != null && surface.isValid) {
                    Message.obtain(
                        null,
                        QemuIpc.MSG_SET_SURFACE
                    ).apply {
                        data = Bundle().apply {
                            putParcelable(
                                QemuIpc.KEY_SURFACE,
                                surface
                            )
                            putInt(QemuIpc.KEY_SURFACE_WIDTH, width)
                            putInt(QemuIpc.KEY_SURFACE_HEIGHT, height)
                            putInt(QemuIpc.KEY_SURFACE_FORMAT, pixelFormat)
                            putFloat(QemuIpc.KEY_SURFACE_REFRESH_RATE, refreshRate)
                        }
                        replyTo = qemuReplyMessenger
                    }
                } else {
                    Message.obtain(
                        null,
                        QemuIpc.MSG_CLEAR_SURFACE
                    ).apply {
                        replyTo = qemuReplyMessenger
                    }
                }

            serviceMessenger.send(message)

            Log.d(
                "RedBoxQemuIPC",
                if (surface != null && surface.isValid) {
                    "Display Surface sent to :qemu process: ${width}x${height}, format=$pixelFormat, refresh=${refreshRate}Hz"
                } else {
                    "Display Surface clear sent to :qemu process"
                }
            )
        } catch (error: Throwable) {
            Log.e(
                "RedBoxQemuIPC",
                "Failed to send display Surface to :qemu process",
                error
            )
        }
    }

    /*
     * Limbo JNI compatibility bridge.
     */
    fun get_fd(path: String): Int {
        return synchronized(limboFileDescriptors) {
            try {
                if (path.isEmpty()) {
                    return@synchronized -1
                }

                val decodedPath =
                    if (path.startsWith("/content//")) {
                        path
                            .replace("/content//", "content://")
                            .replace("^^^", "%")
                    } else {
                        path
                    }

                val parcelFileDescriptor =
                    if (decodedPath.startsWith("content://")) {
                        val uri = Uri.parse(decodedPath)

                        val mode =
                            if (decodedPath.lowercase().endsWith(".iso")) {
                                "r"
                            } else {
                                "rw"
                            }

                        contentResolver.openFileDescriptor(uri, mode)
                            ?: return@synchronized -1
                    } else {
                        val file = File(decodedPath)

                        if (!file.exists()) {
                            file.parentFile?.mkdirs()
                            file.createNewFile()
                        }

                        val mode =
                            if (decodedPath.lowercase().endsWith(".iso")) {
                                ParcelFileDescriptor.MODE_READ_ONLY
                            } else {
                                ParcelFileDescriptor.MODE_READ_WRITE
                            }

                        ParcelFileDescriptor.open(file, mode)
                    }

                val fd = parcelFileDescriptor.fd
                limboFileDescriptors[fd] = parcelFileDescriptor

                Log.d(
                    "RedBoxQEMU",
                    "Limbo get_fd(): $decodedPath -> FD $fd"
                )

                fd
            } catch (error: Exception) {
                Log.e(
                    "RedBoxQEMU",
                    "Limbo get_fd() failed for: $path",
                    error
                )
                -1
            }
        }
    }

    fun close_fd(fd: Int): Int {
        return synchronized(limboFileDescriptors) {
            try {
                val parcelFileDescriptor =
                    limboFileDescriptors.remove(fd)

                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.fileDescriptor.sync()
                    } catch (_: IOException) {
                    }

                    parcelFileDescriptor.close()

                    Log.d(
                        "RedBoxQEMU",
                        "Limbo close_fd(): FD $fd closed"
                    )

                    0
                } else {
                    val fallback =
                        ParcelFileDescriptor.fromFd(fd)

                    try {
                        fallback.fileDescriptor.sync()
                    } catch (_: IOException) {
                    }

                    fallback.close()

                    Log.d(
                        "RedBoxQEMU",
                        "Limbo close_fd(): fallback FD $fd handled"
                    )

                    0
                }
            } catch (error: Exception) {
                Log.e(
                    "RedBoxQEMU",
                    "Limbo close_fd() failed for FD $fd",
                    error
                )
                -1
            }
        }
    }

    /*
     * Extract all QEMU pc-bios assets into getFilesDir().
     * QEMU is started with -L pointing to this directory.
     */
    fun prepareQemuFirmware(): Boolean {
        return try {
            val root = filesDir
            val assetRoot = "pc-bios"

            fun copyAssetTree(
                assetPath: String,
                relativePath: String
            ) {
                val children =
                    assets.list(assetPath) ?: emptyArray()

                if (children.isEmpty()) {
                    val destination =
                        if (relativePath.isEmpty()) {
                            root
                        } else {
                            File(root, relativePath)
                        }

                    destination.parentFile?.mkdirs()

                    assets.open(assetPath).use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d(
                        "RedBoxQEMU",
                        "Extracted firmware: ${destination.absolutePath}"
                    )
                    return
                }

                for (child in children) {
                    val childAssetPath =
                        "$assetPath/$child"

                    val childRelativePath =
                        if (relativePath.isEmpty()) {
                            child
                        } else {
                            "$relativePath/$child"
                        }

                    copyAssetTree(
                        childAssetPath,
                        childRelativePath
                    )
                }
            }

            copyAssetTree(assetRoot, "")

            val biosFile =
                File(root, "bios-256k.bin")

            if (!biosFile.exists() ||
                biosFile.length() == 0L
            ) {
                Log.e(
                    "RedBoxQEMU",
                    "QEMU BIOS extraction failed"
                )
                false
            } else {
                Log.i(
                    "RedBoxQEMU",
                    "QEMU firmware ready: ${biosFile.absolutePath} (${biosFile.length()} bytes)"
                )
                true
            }
        } catch (error: Exception) {
            Log.e(
                "RedBoxQEMU",
                "Failed to extract QEMU firmware",
                error
            )
            false
        }
    }

    /*
     * Save the VM so it survives app restarts.
     */
    /*
     * v0.1.3 Stage 5A:
     * Multi-VM storage foundation.
     *
     * The visible UI still uses one selected VM during this stage. The VM is
     * now also stored inside a JSON library so the next stage can expose
     * multiple saved VMs without changing QEMU execution.
     *
     * The old redbox_vm_storage slot is intentionally kept during migration.
     * This gives existing v0.1.2 installs a safe fallback while v0.1.3 is
     * being tested.
     */
    private fun saveVM(vm: VMModel) {
        saveLegacyVM(vm)

        val library = loadVMLibrary().toMutableList()

        if (library.isEmpty()) {
            library.add(vm)
        } else {
            // Stage 5A has only one visible/selected VM. Update that first
            // library entry until Stage 5B introduces explicit VM selection.
            library[0] = vm
        }

        saveVMLibrary(library)

        Log.d(
            "RedBoxStorage",
            "VM saved to legacy slot and v0.1.3 library: ${vm.name}"
        )
    }

    private fun saveLegacyVM(vm: VMModel) {
        getSharedPreferences("redbox_vm_storage", MODE_PRIVATE)
            .edit()
            .putString("name", vm.name)
            .putString("architecture", vm.architecture)
            .putString("ram", vm.ram)
            .putString("cpuCores", vm.cpuCores)
            .putString("diskImage", vm.diskImage)
            .putString("diskImageName", vm.diskImageName)
            .putString("isoImage", vm.isoImage)
            .putString("isoImageName", vm.isoImageName)
            .putString("driverIsoImage", vm.driverIsoImage)
            .putString("driverIsoImageName", vm.driverIsoImageName)
            .putString("sharedDiskImage", vm.sharedDiskImage)
            .putString("sharedDiskImageName", vm.sharedDiskImageName)
            .putBoolean("sharedFolderEnabled", vm.sharedFolderEnabled)
            .putString("sharedFolderLastFileName", vm.sharedFolderLastFileName)
            .putString("performancePreset", vm.performancePreset)
            .putString("cpuModel", vm.cpuModel)
            .putString("cpuFlags", vm.cpuFlags)
            .putString("tcgCache", vm.tcgCache)
            .putBoolean("multiThreadedTcg", vm.multiThreadedTcg)
            .putString("machineType", vm.machineType)
            .putString("diskInterface", vm.diskInterface)
            .putString("displayAdapter", vm.displayAdapter)
            .putBoolean("networkEnabled", vm.networkEnabled)
            .putString("networkAdapter", vm.networkAdapter)
            .putString("networkMode", vm.networkMode)
            .putString("qemuParams", vm.qemuParams)
            .putString("biosDate", vm.biosDate)
            .putString("soundCard", vm.soundCard)
            .apply()
    }

    private fun vmToJson(vm: VMModel): JSONObject =
        JSONObject().apply {
            put("name", vm.name)
            put("architecture", vm.architecture)
            put("ram", vm.ram)
            put("cpuCores", vm.cpuCores)
            put("diskImage", vm.diskImage)
            put("diskImageName", vm.diskImageName)
            put("isoImage", vm.isoImage)
            put("isoImageName", vm.isoImageName)
            put("driverIsoImage", vm.driverIsoImage)
            put("driverIsoImageName", vm.driverIsoImageName)
            put("sharedDiskImage", vm.sharedDiskImage)
            put("sharedDiskImageName", vm.sharedDiskImageName)
            put("sharedFolderEnabled", vm.sharedFolderEnabled)
            put("sharedFolderLastFileName", vm.sharedFolderLastFileName)
            put("performancePreset", vm.performancePreset)
            put("cpuModel", vm.cpuModel)
            put("cpuFlags", vm.cpuFlags)
            put("tcgCache", vm.tcgCache)
            put("multiThreadedTcg", vm.multiThreadedTcg)
            put("machineType", vm.machineType)
            put("diskInterface", vm.diskInterface)
            put("displayAdapter", vm.displayAdapter)
            put("threeDAcceleration", vm.threeDAcceleration)
            put("networkEnabled", vm.networkEnabled)
            put("networkAdapter", vm.networkAdapter)
            put("networkMode", vm.networkMode)
            put("qemuParams", vm.qemuParams)
            put("biosDate", vm.biosDate)
            put("soundCard", vm.soundCard)
            put("audioBackend", vm.audioBackend)
            put("bootPriority", vm.bootPriority)
            put("firmwareMode", vm.firmwareMode)
            put("highPriority", vm.highPriority)
        }

    private fun jsonToVM(json: JSONObject): VMModel =
        VMModel(
            name = json.optString("name", "Virtual Machine"),
            architecture = json.optString("architecture", "x86_64"),
            ram = json.optString("ram", "1024 MB"),
            cpuCores = json.optString("cpuCores", "1"),
            diskImage = json.optString("diskImage", ""),
            diskImageName = json.optString("diskImageName", ""),
            isoImage = json.optString("isoImage", ""),
            isoImageName = json.optString("isoImageName", ""),
            driverIsoImage = json.optString("driverIsoImage", ""),
            driverIsoImageName = json.optString("driverIsoImageName", ""),
            sharedDiskImage = json.optString("sharedDiskImage", ""),
            sharedDiskImageName = json.optString("sharedDiskImageName", ""),
            sharedFolderEnabled = json.optBoolean("sharedFolderEnabled", false),
            sharedFolderLastFileName =
                json.optString("sharedFolderLastFileName", ""),
            performancePreset = json.optString("performancePreset", "Balanced"),
            cpuModel = json.optString("cpuModel", "Default"),
            cpuFlags = json.optString("cpuFlags", ""),
            tcgCache = json.optString("tcgCache", "256 MB"),
            multiThreadedTcg = json.optBoolean("multiThreadedTcg", true),
            machineType = json.optString("machineType", "pc"),
            diskInterface =
                json.optString("diskInterface", "AHCI").let {
                    if (it == "VirtIO") "VirtIO Block" else it
                },
            displayAdapter = json.optString("displayAdapter", "Standard VGA"),
            threeDAcceleration = json.optBoolean("threeDAcceleration", false),
            networkEnabled = json.optBoolean("networkEnabled", true),
            networkAdapter =
                json.optString("networkAdapter", "Realtek RTL8139"),
            networkMode = json.optString("networkMode", "User (NAT)"),
            qemuParams = json.optString("qemuParams", ""),
            biosDate = json.optString("biosDate", "Default"),
            soundCard = json.optString("soundCard", "Intel HDA"),
            audioBackend = json.optString("audioBackend", "Default"),
            bootPriority = json.optString("bootPriority", "Hard Disk First"),
            firmwareMode = json.optString("firmwareMode", "Legacy BIOS"),
            highPriority = json.optBoolean("highPriority", false)
        )

    private fun saveVMLibrary(vms: List<VMModel>) {
        val array = JSONArray()

        vms.forEach { vm ->
            array.put(vmToJson(vm))
        }

        getSharedPreferences("redbox_vm_library", MODE_PRIVATE)
            .edit()
            .putInt("storageVersion", 1)
            .putString("vms_json", array.toString())
            .apply()

        Log.d(
            "RedBoxStorage",
            "v0.1.3 VM library saved: ${vms.size} VM(s)"
        )
    }

    private fun loadVMLibrary(): List<VMModel> {
        val preferences =
            getSharedPreferences("redbox_vm_library", MODE_PRIVATE)

        val raw = preferences.getString("vms_json", null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<VMModel>()

            for (index in 0 until array.length()) {
                result.add(jsonToVM(array.getJSONObject(index)))
            }

            result
        } catch (error: Throwable) {
            Log.e(
                "RedBoxStorage",
                "Failed to read v0.1.3 VM library; legacy VM remains untouched",
                error
            )
            emptyList()
        }
    }

    /*
     * Restore the VM used by the current UI.
     *
     * First try the new v0.1.3 library. If it does not exist yet, load the
     * old v0.1.2 single-VM slot and copy that VM into the new library.
     */
    private fun loadSavedVM(): VMModel? {
        val library = loadVMLibrary()

        if (library.isNotEmpty()) {
            return library.first().also {
                Log.d(
                    "RedBoxStorage",
                    "VM restored from v0.1.3 library: ${it.name}"
                )
            }
        }

        val legacyVM = loadLegacyVM() ?: return null

        saveVMLibrary(listOf(legacyVM))

        Log.d(
            "RedBoxStorage",
            "Migrated legacy VM into v0.1.3 library: ${legacyVM.name}"
        )

        return legacyVM
    }

    private fun loadLegacyVM(): VMModel? {
        val preferences =
            getSharedPreferences("redbox_vm_storage", MODE_PRIVATE)

        val name =
            preferences.getString("name", null)
                ?: return null

        return VMModel(
            name = name,
            architecture =
                preferences.getString("architecture", "x86_64")
                    ?: "x86_64",
            ram =
                preferences.getString("ram", "1024 MB")
                    ?: "1024 MB",
            cpuCores =
                preferences.getString("cpuCores", "1")
                    ?: "1",
            diskImage =
                preferences.getString("diskImage", "")
                    ?: "",
            diskImageName =
                preferences.getString("diskImageName", "")
                    ?: "",
            isoImage =
                preferences.getString("isoImage", "")
                    ?: "",
            isoImageName =
                preferences.getString("isoImageName", "")
                    ?: "",
            driverIsoImage =
                preferences.getString("driverIsoImage", "")
                    ?: "",
            driverIsoImageName =
                preferences.getString("driverIsoImageName", "")
                    ?: "",
            sharedDiskImage =
                preferences.getString("sharedDiskImage", "")
                    ?: "",
            sharedDiskImageName =
                preferences.getString("sharedDiskImageName", "")
                    ?: "",
            sharedFolderEnabled =
                preferences.getBoolean("sharedFolderEnabled", false),
            sharedFolderLastFileName =
                preferences.getString("sharedFolderLastFileName", "")
                    ?: "",
            performancePreset =
                preferences.getString("performancePreset", "Balanced")
                    ?: "Balanced",
            cpuModel =
                preferences.getString("cpuModel", "Default")
                    ?: "Default",
            cpuFlags =
                preferences.getString("cpuFlags", "")
                    ?: "",
            tcgCache =
                preferences.getString("tcgCache", "256 MB")
                    ?: "256 MB",
            multiThreadedTcg =
                preferences.getBoolean("multiThreadedTcg", true),
            machineType =
                preferences.getString("machineType", "pc")
                    ?: "pc",
            diskInterface =
                (preferences.getString("diskInterface", "AHCI") ?: "AHCI").let {
                    if (it == "VirtIO") "VirtIO Block" else it
                },
            displayAdapter =
                preferences.getString("displayAdapter", "Standard VGA")
                    ?: "Standard VGA",
            networkEnabled =
                preferences.getBoolean("networkEnabled", true),
            networkAdapter =
                preferences.getString("networkAdapter", "Realtek RTL8139")
                    ?: "Realtek RTL8139",
            networkMode =
                preferences.getString("networkMode", "User (NAT)")
                    ?: "User (NAT)",
            qemuParams =
                preferences.getString("qemuParams", "")
                    ?: "",
            biosDate =
                preferences.getString("biosDate", "Default")
                    ?: "Default",
            soundCard =
                preferences.getString("soundCard", "Intel HDA")
                    ?: "Intel HDA",
            audioBackend = "Default"
        ).also {
            Log.d("RedBoxStorage", "Legacy VM restored: ${it.name}")
        }
    }

    /*
     * Keep SDLActivity's SDL thread alive.
     *
     * Limbo overrides runSDLMain() and blocks that thread while QEMU is
     * running. RedBox starts QEMU on its own executor, so we only need to
     * prevent SDLMain from returning immediately (which would make
     * SDLActivity call finish() and close the app).
     */
    private val sdlKeepAliveLatch = CountDownLatch(1)

    @Synchronized
    override fun runSDLMain() {
        Log.d("RedBoxSDL", "SDL main thread entered; keeping SDLActivity alive")

        try {
            sdlKeepAliveLatch.await()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.d("RedBoxSDL", "SDL keep-alive thread interrupted")
        }

        Log.d("RedBoxSDL", "SDL main thread released")
    }

    private val qemuExecutor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    /*
     * STEP 13C:
     * Physical volume buttons become guest mouse buttons only while the
     * RedBox VM screen is visible.
     *
     * Volume Down = left mouse button
     * Volume Up   = right mouse button
     *
     * This is forwarded through Messenger to QemuService, so input reaches
     * the QEMU instance running inside the isolated :qemu process.
     */
    /**
     * Tap-to-click for the VM touch surface. Sends a complete left-button
     * press/release pair through the existing QEMU IPC path.
     */
    fun clickRedBoxMouse() {
        sendRedBoxMouseButton(button = 0, down = true)
        sendRedBoxMouseButton(button = 0, down = false)
    }

    private fun sendRedBoxMouseButton(
        button: Int,
        down: Boolean
    ) {
        val serviceMessenger = qemuServiceMessenger

        if (!qemuProcessActive.get() ||
            !qemuServiceBound ||
            serviceMessenger == null
        ) {
            return
        }

        try {
            val message =
                Message.obtain(
                    null,
                    QemuIpc.MSG_MOUSE_BUTTON
                ).apply {
                    data = Bundle().apply {
                        putInt(
                            QemuIpc.KEY_MOUSE_BUTTON,
                            button
                        )
                        putBoolean(
                            QemuIpc.KEY_MOUSE_BUTTON_DOWN,
                            down
                        )
                    }
                }

            serviceMessenger.send(message)
        } catch (error: RemoteException) {
            Log.e(
                "RedBoxQemuIPC",
                "Failed to send mouse button to :qemu process",
                error
            )
        }
    }

    private fun sendRedBoxKeyboardKey(
        keyCode: Int,
        down: Boolean
    ) {
        val serviceMessenger = qemuServiceMessenger

        if (!qemuProcessActive.get() ||
            !qemuServiceBound ||
            serviceMessenger == null
        ) {
            return
        }

        try {
            val message =
                Message.obtain(
                    null,
                    QemuIpc.MSG_KEYBOARD_KEY
                ).apply {
                    data = Bundle().apply {
                        putInt(QemuIpc.KEY_KEYBOARD_KEY_CODE, keyCode)
                        putBoolean(QemuIpc.KEY_KEYBOARD_KEY_DOWN, down)
                    }
                }

            serviceMessenger.send(message)
        } catch (error: RemoteException) {
            Log.e(
                "RedBoxQemuIPC",
                "Failed to send keyboard key to :qemu process",
                error
            )
        }
    }

    /*
     * REDBOX Keyboard IME bridge:
     * Android software keyboards usually commit text through InputConnection
     * instead of producing ordinary KeyEvent objects. SDLActivity forwards that
     * committed text here so it can cross the existing Messenger boundary into
     * the isolated :qemu process.
     */
    protected override fun onRedBoxImeText(text: String): Boolean {
        if (!redBoxVmScreenVisible || !qemuProcessActive.get()) {
            return false
        }

        text.forEach { character ->
            sendRedBoxImeCharacter(character)
        }

        return true
    }

    protected override fun onRedBoxImeKey(
        keyCode: Int,
        down: Boolean
    ): Boolean {
        if (!redBoxVmScreenVisible || !qemuProcessActive.get()) {
            return false
        }

        if (!isRedBoxKeyboardKey(keyCode)) {
            return false
        }

        sendRedBoxKeyboardKey(keyCode, down)
        return true
    }

    private fun sendRedBoxImeCharacter(character: Char) {
        val mapping = when (character) {
            in 'a'..'z' -> Pair(KeyEvent.KEYCODE_A + (character - 'a'), false)
            in 'A'..'Z' -> Pair(KeyEvent.KEYCODE_A + (character - 'A'), true)
            in '0'..'9' -> Pair(KeyEvent.KEYCODE_0 + (character - '0'), false)
            ' ' -> Pair(KeyEvent.KEYCODE_SPACE, false)
            '\n', '\r' -> Pair(KeyEvent.KEYCODE_ENTER, false)
            '\t' -> Pair(KeyEvent.KEYCODE_TAB, false)
            ',' -> Pair(KeyEvent.KEYCODE_COMMA, false)
            '<' -> Pair(KeyEvent.KEYCODE_COMMA, true)
            '.' -> Pair(KeyEvent.KEYCODE_PERIOD, false)
            '>' -> Pair(KeyEvent.KEYCODE_PERIOD, true)
            '`' -> Pair(KeyEvent.KEYCODE_GRAVE, false)
            '~' -> Pair(KeyEvent.KEYCODE_GRAVE, true)
            '-' -> Pair(KeyEvent.KEYCODE_MINUS, false)
            '_' -> Pair(KeyEvent.KEYCODE_MINUS, true)
            '=' -> Pair(KeyEvent.KEYCODE_EQUALS, false)
            '+' -> Pair(KeyEvent.KEYCODE_EQUALS, true)
            '[' -> Pair(KeyEvent.KEYCODE_LEFT_BRACKET, false)
            '{' -> Pair(KeyEvent.KEYCODE_LEFT_BRACKET, true)
            ']' -> Pair(KeyEvent.KEYCODE_RIGHT_BRACKET, false)
            '}' -> Pair(KeyEvent.KEYCODE_RIGHT_BRACKET, true)
            '\\' -> Pair(KeyEvent.KEYCODE_BACKSLASH, false)
            '|' -> Pair(KeyEvent.KEYCODE_BACKSLASH, true)
            ';' -> Pair(KeyEvent.KEYCODE_SEMICOLON, false)
            ':' -> Pair(KeyEvent.KEYCODE_SEMICOLON, true)
            '\'' -> Pair(KeyEvent.KEYCODE_APOSTROPHE, false)
            '"' -> Pair(KeyEvent.KEYCODE_APOSTROPHE, true)
            '/' -> Pair(KeyEvent.KEYCODE_SLASH, false)
            '?' -> Pair(KeyEvent.KEYCODE_SLASH, true)
            '!' -> Pair(KeyEvent.KEYCODE_1, true)
            '@' -> Pair(KeyEvent.KEYCODE_2, true)
            '#' -> Pair(KeyEvent.KEYCODE_3, true)
            '$' -> Pair(KeyEvent.KEYCODE_4, true)
            '%' -> Pair(KeyEvent.KEYCODE_5, true)
            '^' -> Pair(KeyEvent.KEYCODE_6, true)
            '&' -> Pair(KeyEvent.KEYCODE_7, true)
            '*' -> Pair(KeyEvent.KEYCODE_8, true)
            '(' -> Pair(KeyEvent.KEYCODE_9, true)
            ')' -> Pair(KeyEvent.KEYCODE_0, true)
            else -> null
        } ?: run {
            Log.d("RedBoxKeyboard", "IME character not mapped: U+${character.code.toString(16)}")
            return
        }

        val (keyCode, needsShift) = mapping

        if (needsShift) {
            sendRedBoxKeyboardKey(KeyEvent.KEYCODE_SHIFT_LEFT, true)
        }

        sendRedBoxKeyboardKey(keyCode, true)
        sendRedBoxKeyboardKey(keyCode, false)

        if (needsShift) {
            sendRedBoxKeyboardKey(KeyEvent.KEYCODE_SHIFT_LEFT, false)
        }
    }

    private fun isRedBoxKeyboardKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ||
            keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ||
            keyCode in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 ||
            keyCode in setOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DEL,
                KeyEvent.KEYCODE_FORWARD_DEL,
                KeyEvent.KEYCODE_SPACE,
                KeyEvent.KEYCODE_TAB,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.KEYCODE_SHIFT_RIGHT,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.KEYCODE_ALT_RIGHT,
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.KEYCODE_CTRL_RIGHT,
                KeyEvent.KEYCODE_CAPS_LOCK,
                KeyEvent.KEYCODE_INSERT,
                KeyEvent.KEYCODE_MOVE_HOME,
                KeyEvent.KEYCODE_MOVE_END,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_COMMA,
                KeyEvent.KEYCODE_PERIOD,
                KeyEvent.KEYCODE_GRAVE,
                KeyEvent.KEYCODE_MINUS,
                KeyEvent.KEYCODE_EQUALS,
                KeyEvent.KEYCODE_LEFT_BRACKET,
                KeyEvent.KEYCODE_RIGHT_BRACKET,
                KeyEvent.KEYCODE_BACKSLASH,
                KeyEvent.KEYCODE_SEMICOLON,
                KeyEvent.KEYCODE_APOSTROPHE,
                KeyEvent.KEYCODE_SLASH
            )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (redBoxVmScreenVisible && qemuProcessActive.get()) {
            val qemuButton = when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> 0
                KeyEvent.KEYCODE_VOLUME_UP -> 2
                else -> -1
            }

            if (qemuButton != -1) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            sendRedBoxMouseButton(
                                qemuButton,
                                true
                            )
                        }
                    }

                    KeyEvent.ACTION_UP -> {
                        sendRedBoxMouseButton(
                            qemuButton,
                            false
                        )
                    }
                }

                // Consume the volume key so Android volume does not change.
                return true
            }

            if (isRedBoxKeyboardKey(event.keyCode)) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        sendRedBoxKeyboardKey(
                            event.keyCode,
                            true
                        )
                    }

                    KeyEvent.ACTION_UP -> {
                        sendRedBoxKeyboardKey(
                            event.keyCode,
                            false
                        )
                    }
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("RedBoxBilling", "Google Play Billing connected")
                    queryDonationProduct()
                } else {
                    Log.e("RedBoxBilling", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("RedBoxBilling", "Google Play Billing disconnected")
            }
        })
    }

    private fun queryDonationProduct(onReady: (() -> Unit)? = null) {
        val client = billingClient ?: return
        if (!client.isReady) return

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(donateProductId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                donateProductDetails = queryResult.productDetailsList.firstOrNull()
                if (donateProductDetails != null) onReady?.invoke()
            } else {
                Log.e("RedBoxBilling", "Could not load donation product: ${billingResult.debugMessage}")
            }
        }
    }

    private fun launchDonation() {
        val client = billingClient
        if (client == null || !client.isReady) {
            Toast.makeText(this, "Google Play Billing is connecting. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }

        val details = donateProductDetails
        if (details == null) {
            queryDonationProduct { launchDonation() }
            Toast.makeText(this, "Loading donation...", Toast.LENGTH_SHORT).show()
            return
        }

        val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (offer == null) {
            Toast.makeText(this, "Donation is not available yet.", Toast.LENGTH_LONG).show()
            return
        }

        val offerToken = offer.offerToken
        if (offerToken.isNullOrBlank()) {
            Toast.makeText(this, "Donation offer is not available yet.", Toast.LENGTH_LONG).show()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = client.launchBillingFlow(this, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Toast.makeText(this, "Could not open Google Play purchase: ${result.debugMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun consumeDonationPurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.consumeAsync(params) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this, "Thank you for supporting RedBox!", Toast.LENGTH_LONG).show()
                Log.d("RedBoxBilling", "Donation purchase consumed successfully")
            } else {
                Log.e("RedBoxBilling", "Could not consume donation: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBilling()
        bindFreshQemuService()

        val nativeMessage = stringFromJNI()
        Log.d("RedBoxNative", nativeMessage)

        val qemuStatus = nativeQemuStatus()
        Log.d("RedBoxQEMU", qemuStatus)
        RedBoxSupportLog.add("RedBox 1.1.0 started; native QEMU status: $qemuStatus")

        // Stage 5B: load/migrate first, then expose the complete VM library.
        loadSavedVM()
        val savedVMs = loadVMLibrary()

        setContent {
            RedBoxApp(
                qemuStatus = qemuStatus,
                initialVMs = savedVMs,
                onVMLibrarySaved = { vms ->
                    saveVMLibrary(vms)

                    // Keep the old single-VM slot as a development fallback.
                    vms.firstOrNull()?.let { saveLegacyVM(it) }
                },
                onDonate = { launchDonation() },
                onStopQemu = {
                    val serviceMessenger = qemuServiceMessenger

                    if (!qemuServiceBound || serviceMessenger == null) {
                        Log.e("RedBoxQemuIPC", "Cannot stop VM: :qemu service is not connected")
                        false
                    } else {
                        try {
                            val message = Message.obtain(null, QemuIpc.MSG_STOP_VM)
                            message.replyTo = qemuReplyMessenger
                            serviceMessenger.send(message)
                            Log.d("RedBoxQemuIPC", "Sent MSG_STOP_VM to :qemu process")
                            true
                        } catch (error: RemoteException) {
                            Log.e("RedBoxQemuIPC", "Failed to send MSG_STOP_VM", error)
                            false
                        }
                    }
                },
                onStartQemu = startQemu@ { vm, onResult ->
                    val serviceMessenger = qemuServiceMessenger

                    if (!qemuServiceBound || serviceMessenger == null) {
                        Log.e("RedBoxQemuIPC", "Cannot start VM: :qemu service is not connected")
                        mainHandler.post { onResult("QEMU service is not connected") }
                        return@startQemu
                    }

                    if (!qemuProcessActive.compareAndSet(false, true)) {
                        Log.w("RedBoxQemuIPC", "QEMU start ignored because a VM is already active")
                        mainHandler.post { onResult("QEMU VM is already running") }
                        return@startQemu
                    }

                    qemuSessionEndedCallback = onResult

                    try {
                        val ramMb = vm.ram.filter { it.isDigit() }.toIntOrNull() ?: 1024
                        val cpuCores = vm.cpuCores.filter { it.isDigit() }.toIntOrNull() ?: 1
                        val tcgCacheMb = vm.tcgCache.substringBefore(" ").toIntOrNull() ?: 256

                        if (vm.sharedFolderEnabled) {
                            File(filesDir, "redbox_shared").mkdirs()
                        }

                        val data = Bundle().apply {
                            putInt(QemuIpc.KEY_RAM_MB, ramMb)
                            putInt(QemuIpc.KEY_CPU_CORES, cpuCores)
                            putString("disk_uri", vm.diskImage)
                            putString(QemuIpc.KEY_DISK_IMAGE_NAME, vm.diskImageName)
                            putString("iso_uri", vm.isoImage)
                            putString("driver_iso_uri", vm.driverIsoImage)
                            putString("shared_disk_uri", vm.sharedDiskImage)
                            putString(QemuIpc.KEY_SHARED_DISK_IMAGE_NAME, vm.sharedDiskImageName)
                            putBoolean(QemuIpc.KEY_SHARED_FOLDER_ENABLED, vm.sharedFolderEnabled)
                            putString(QemuIpc.KEY_CPU_MODEL, vm.cpuModel)
                            putString(QemuIpc.KEY_CPU_FLAGS, vm.cpuFlags)
                            putInt(QemuIpc.KEY_TCG_CACHE_MB, tcgCacheMb)
                            putBoolean(QemuIpc.KEY_MULTI_THREADED_TCG, vm.multiThreadedTcg)
                            putString(QemuIpc.KEY_MACHINE_TYPE, vm.machineType)
                            putString(QemuIpc.KEY_DISK_INTERFACE, vm.diskInterface)
                            putString(QemuIpc.KEY_DISPLAY_ADAPTER, vm.displayAdapter)
                            putBoolean("three_d_acceleration", vm.threeDAcceleration && vm.displayAdapter == "VirtIO VGA")
                            putBoolean(QemuIpc.KEY_NETWORK_ENABLED, vm.networkEnabled)
                            putString(QemuIpc.KEY_NETWORK_ADAPTER, vm.networkAdapter)
                            putString(QemuIpc.KEY_NETWORK_MODE, vm.networkMode)
                            putString(QemuIpc.KEY_QEMU_PARAMS, vm.qemuParams)
                            putString(QemuIpc.KEY_BIOS_DATE, vm.biosDate)
                            putString(QemuIpc.KEY_SOUND_CARD, vm.soundCard)
                            putString("boot_priority", vm.bootPriority)
                            putString("firmware_mode", vm.firmwareMode)
                            putBoolean("high_priority", vm.highPriority)
                        }

                        val message = Message.obtain(null, QemuIpc.MSG_START_VM)
                        message.data = data
                        message.replyTo = qemuReplyMessenger

                        Log.d(
                            "RedBoxQemuIPC",
                            "Sending VM to :qemu: RAM=${ramMb}MB, CPU=$cpuCores, model=${vm.cpuModel}, disk=${vm.diskInterface}, display=${vm.displayAdapter}"
                        )

                        serviceMessenger.send(message)

                        Log.d(
                            "RedBoxQemuIPC",
                            "MSG_START_VM sent. UI PID=${android.os.Process.myPid()}"
                        )
                    } catch (error: Throwable) {
                        qemuProcessActive.set(false)
                        qemuSessionEndedCallback = null
                        Log.e("RedBoxQemuIPC", "Failed to send VM start request", error)
                        mainHandler.post { onResult("QEMU start failed: ${error.message}") }
                    }
                }
            )
        }
    }

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(newConfig)

        val orientationName =
            when (newConfig.orientation) {
                Configuration.ORIENTATION_LANDSCAPE ->
                    "landscape"

                Configuration.ORIENTATION_PORTRAIT ->
                    "portrait"

                else ->
                    "undefined"
            }

        Log.d(
            "RedBoxRotation",
            "Configuration changed in-place: $orientationName"
        )
    }

    override fun onDestroy() {
        /*
         * Normal rotation is handled in-place by AndroidManifest configChanges,
         * so this path should not run for rotation. This extra check protects
         * the native VM if Android ever recreates us for another configuration.
         */
        if (isChangingConfigurations) {
            Log.d(
                "RedBoxRotation",
                "Activity changing configuration; keeping QEMU/SDL worker alive"
            )

            super.onDestroy()
            return
        }

        billingClient?.endConnection()
        billingClient = null

        if (qemuServiceBound) {
            try {
                unbindService(
                    qemuServiceConnection
                )
            } catch (error: IllegalArgumentException) {
                Log.w(
                    "RedBoxQemuIPC",
                    "QEMU service was already unbound",
                    error
                )
            }

            qemuServiceBound = false
            qemuServiceMessenger = null
        }

        sdlKeepAliveLatch.countDown()
        qemuExecutor.shutdownNow()
        super.onDestroy()
    }
}

@Composable
fun RedBoxApp(
    qemuStatus: String,
    initialVMs: List<VMModel>,
    onVMLibrarySaved: (List<VMModel>) -> Unit,
    onDonate: () -> Unit,
    onStopQemu: () -> Boolean,
    onStartQemu: ((VMModel, (String) -> Unit) -> Unit)
) {
    var showCreateVM by rememberSaveable { mutableStateOf(false) }
    var showEditVM by rememberSaveable { mutableStateOf(false) }
    var showVMDetails by rememberSaveable { mutableStateOf(false) }
    var showVMScreen by rememberSaveable { mutableStateOf(false) }
    var showDeleteVMConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDuplicateVMDialog by rememberSaveable { mutableStateOf(false) }
    var duplicateVMName by rememberSaveable { mutableStateOf("") }

    var selectedTab by rememberSaveable {
        mutableIntStateOf(0)
    }

    var selectedFileFilter by rememberSaveable {
        mutableStateOf("All")
    }

    var savedVMs by remember {
        mutableStateOf(initialVMs)
    }

    val context = LocalContext.current

    /*
     * v0.2.1 Stage 2B:
     * A lightweight local RedBox profile powers the Windows-inspired welcome
     * screen. No account, sign-in, or network connection is required.
     */
    val profilePreferences = remember {
        context.getSharedPreferences("redbox_profile", Context.MODE_PRIVATE)
    }

    var profileName by rememberSaveable {
        mutableStateOf(
            profilePreferences
                .getString("profile_name", "User")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "User"
        )
    }

    var profileAvatarUri by rememberSaveable {
        mutableStateOf(
            profilePreferences
                .getString("profile_avatar_uri", "")
                .orEmpty()
        )
    }

    val setupPreferences = remember {
        context.getSharedPreferences("redbox_v1_setup", Context.MODE_PRIVATE)
    }
    // Existing installations keep their current setup; only new installations
    // with no profile preferences enter first-time setup.
    var showFirstSetup by rememberSaveable {
        mutableStateOf(
            !setupPreferences.getBoolean("completed", false) &&
                !profilePreferences.contains("profile_name") &&
                !profilePreferences.contains("profile_avatar_uri")
        )
    }
    var showWelcomeScreen by rememberSaveable {
        mutableStateOf(true)
    }

    // Restart the welcome timer when first-time setup finishes.
    // Without showFirstSetup in the key, the loading screen can loop forever.
    LaunchedEffect(showWelcomeScreen, showFirstSetup) {
        if (showWelcomeScreen && !showFirstSetup) {
            delay(2200L)
            showWelcomeScreen = false
        }
    }

    val recentVmPreferences = remember {
        context.getSharedPreferences("redbox_recent_vm", Context.MODE_PRIVATE)
    }

    var selectedVMIndex by rememberSaveable {
        val storedIndex = recentVmPreferences.getInt("selected_vm_index", 0)
        mutableIntStateOf(
            if (initialVMs.isEmpty()) {
                -1
            } else {
                storedIndex.coerceIn(initialVMs.indices)
            }
        )
    }

    fun rememberSelectedVM(index: Int) {
        selectedVMIndex = index
        recentVmPreferences
            .edit()
            .putInt("selected_vm_index", index)
            .apply()
    }

    val selectedVM =
        if (selectedVMIndex in savedVMs.indices) {
            savedVMs[selectedVMIndex]
        } else {
            null
        }

    var runningVMIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    var isVMRunning by rememberSaveable {
        mutableStateOf(false)
    }

    fun persistSelectedDocumentUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (error: SecurityException) {
            Log.w("RedBoxStorage", "Could not persist media URI permission: $uri", error)
        }
    }

    fun mediaDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) {
                    cursor.getString(index) ?: "Selected ISO"
                } else {
                    "Selected ISO"
                }
            } ?: "Selected ISO"
        } catch (_: Exception) {
            "Selected ISO"
        }
    }

    fun updateSelectedVMMedia(updatedVM: VMModel, action: String) {
        if (selectedVMIndex !in savedVMs.indices || isVMRunning) {
            return
        }

        val updatedLibrary = savedVMs.toMutableList()
        updatedLibrary[selectedVMIndex] = updatedVM
        savedVMs = updatedLibrary
        onVMLibrarySaved(updatedLibrary)

        RedBoxSupportLog.add("$action for VM '${updatedVM.name}'")
        Log.d("RedBoxStorage", "$action: ${updatedVM.name}")
    }

    val primaryIsoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val vm = if (selectedVMIndex in savedVMs.indices) savedVMs[selectedVMIndex] else null
        if (uri != null && vm != null && !isVMRunning) {
            persistSelectedDocumentUri(uri)
            updateSelectedVMMedia(
                vm.copy(
                    isoImage = uri.toString(),
                    isoImageName = mediaDisplayName(uri)
                ),
                "Primary ISO changed"
            )
        }
    }

    val driverIsoQuickPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val vm = if (selectedVMIndex in savedVMs.indices) savedVMs[selectedVMIndex] else null
        if (uri != null && vm != null && !isVMRunning) {
            persistSelectedDocumentUri(uri)
            updateSelectedVMMedia(
                vm.copy(
                    driverIsoImage = uri.toString(),
                    driverIsoImageName = mediaDisplayName(uri)
                ),
                "Driver ISO changed"
            )
        }
    }

    var qemuRuntimeStatus by rememberSaveable {
        mutableStateOf("")
    }

    var darkTheme by rememberSaveable {
        mutableStateOf(true)
    }

    /*
     * Stage 5B:
     * RedBox now keeps a real list of saved VMs. selectedVMIndex determines
     * which VM is shown in Details/Edit/Files, while runningVMIndex tracks
     * which library entry owns the active QEMU session.
     */
    BackHandler(enabled = true) {
        when {
            showDuplicateVMDialog -> {
                showDuplicateVMDialog = false
            }

            showDeleteVMConfirmation -> {
                showDeleteVMConfirmation = false
            }

            showCreateVM -> {
                showCreateVM = false
            }

            showEditVM -> {
                showEditVM = false
                showVMDetails = true
            }

            showVMScreen -> {
                showVMScreen = false
                showVMDetails = true
            }

            showVMDetails -> {
                showVMDetails = false
            }

            selectedTab != 0 -> {
                selectedTab = 0
            }

            else -> {
                Log.d(
                    "RedBoxNavigation",
                    "System Back ignored on Home so RedBox stays open"
                )
            }
        }
    }

    if (showCreateVM) {
        CreateVMScreen(
            onBack = {
                showCreateVM = false
            },
            onVMCreated = { vm ->
                val updatedLibrary = savedVMs + vm
                savedVMs = updatedLibrary
                rememberSelectedVM(updatedLibrary.lastIndex)
                onVMLibrarySaved(updatedLibrary)

                isVMRunning = false
                runningVMIndex = -1
                qemuRuntimeStatus = ""
                showCreateVM = false
                selectedTab = 1

                Log.d(
                    "RedBoxStorage",
                    "VM added to library: ${vm.name}; total=${updatedLibrary.size}"
                )
            }
        )
        return
    }

    if (showEditVM && selectedVM != null) {
        EditVMScreen(
            vm = selectedVM,
            onBack = {
                showEditVM = false
                showVMDetails = true
            },
            onSave = { updatedVM ->
                if (selectedVMIndex in savedVMs.indices) {
                    val updatedLibrary = savedVMs.toMutableList()
                    updatedLibrary[selectedVMIndex] = updatedVM
                    savedVMs = updatedLibrary
                    onVMLibrarySaved(updatedLibrary)
                }

                isVMRunning = false
                runningVMIndex = -1
                qemuRuntimeStatus = ""
                showEditVM = false
                showVMDetails = true

                Log.d(
                    "RedBoxStorage",
                    "VM edited and saved: ${updatedVM.name}, RAM=${updatedVM.ram}, CPU=${updatedVM.cpuCores}"
                )
            }
        )
        return
    }

    if (showVMScreen && selectedVM != null) {
        VMScreen(
            vm = selectedVM,
            onStop = {
                qemuRuntimeStatus = "Stopping QEMU..."
                val stopRequested = onStopQemu()

                if (stopRequested) {
                    showVMScreen = false
                    showVMDetails = true
                } else {
                    qemuRuntimeStatus = "QEMU stop request failed"
                }
            },
            onBack = {
                showVMScreen = false
                showVMDetails = true
            }
        )
        return
    }

    if (showVMDetails && selectedVM != null) {
        VMDetailsScreen(
            vm = selectedVM,
            isRunning =
                isVMRunning && runningVMIndex == selectedVMIndex,
            qemuRuntimeStatus =
                if (runningVMIndex == selectedVMIndex) {
                    qemuRuntimeStatus
                } else {
                    ""
                },
            onEditVM = {
                if (!isVMRunning) {
                    showVMDetails = false
                    showEditVM = true
                }
            },
            onStartVM = {
                if (!isVMRunning) {
                    val validationError =
                        validateVMForStart(selectedVM)

                    if (validationError != null) {
                        qemuRuntimeStatus = validationError
                        RedBoxSupportLog.add(
                            "Validation blocked VM start: ${selectedVM.name} \u2014 $validationError"
                        )
                    } else {
                        isVMRunning = true
                        runningVMIndex = selectedVMIndex
                        showVMDetails = false
                        showVMScreen = true
                        qemuRuntimeStatus = "Starting QEMU..."

                        RedBoxSupportLog.add(
                            "Starting VM '${selectedVM.name}': arch=${selectedVM.architecture}, RAM=${selectedVM.ram}, cores=${selectedVM.cpuCores}, machine=${selectedVM.machineType}, diskInterface=${selectedVM.diskInterface}, display=${selectedVM.displayAdapter}, disk=${selectedVM.diskImageName.ifBlank { "none" }}, ISO=${selectedVM.isoImageName.ifBlank { "none" }}, customQemu=${selectedVM.qemuParams.isNotBlank()}"
                        )

                        onStartQemu(selectedVM) { result ->
                        qemuRuntimeStatus = result
                        isVMRunning = false
                        runningVMIndex = -1

                            RedBoxSupportLog.add(
                                "VM '${selectedVM.name}' session result: $result"
                            )

                            Log.d(
                                "RedBoxQEMU",
                                "Result returned to UI: $result"
                            )
                        }
                    }
                }
            },
            onStopVM = {
                qemuRuntimeStatus = "Stopping QEMU..."
                RedBoxSupportLog.add("Stop requested for VM '${selectedVM.name}'")

                if (!onStopQemu()) {
                    qemuRuntimeStatus = "QEMU stop request failed"
                    RedBoxSupportLog.add("Stop request failed for VM '${selectedVM.name}'")
                }
            },
            onReturnToVM = {
                if (isVMRunning && runningVMIndex == selectedVMIndex) {
                    showVMDetails = false
                    showVMScreen = true

                    Log.d(
                        "RedBoxNavigation",
                        "Returning to running VM screen: ${selectedVM.name}"
                    )
                }
            },
            onDuplicateVM = {
                duplicateVMName = "${selectedVM.name} Copy"
                showDuplicateVMDialog = true
            },
            onChangePrimaryIso = {
                if (!isVMRunning) {
                    primaryIsoPicker.launch(arrayOf("application/x-iso9660-image", "application/octet-stream", "*/*"))
                }
            },
            onEjectPrimaryIso = {
                if (!isVMRunning && selectedVM.isoImage.isNotBlank()) {
                    updateSelectedVMMedia(
                        selectedVM.copy(isoImage = "", isoImageName = ""),
                        "Primary ISO ejected"
                    )
                }
            },
            onChangeDriverIso = {
                if (!isVMRunning) {
                    driverIsoQuickPicker.launch(arrayOf("application/x-iso9660-image", "application/octet-stream", "*/*"))
                }
            },
            onEjectDriverIso = {
                if (!isVMRunning && selectedVM.driverIsoImage.isNotBlank()) {
                    updateSelectedVMMedia(
                        selectedVM.copy(driverIsoImage = "", driverIsoImageName = ""),
                        "Driver ISO ejected"
                    )
                }
            },
            onDeleteVM = {
                if (!isVMRunning) {
                    showDeleteVMConfirmation = true
                }
            },
            onBack = {
                showVMDetails = false
            }
        )

        if (showDuplicateVMDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDuplicateVMDialog = false
                },
                title = {
                    Text("Duplicate virtual machine")
                },
                text = {
                    Column {
                        Text(
                            "Create a new VM with the same configuration as \"${selectedVM.name}\". " +
                                "The disk image, ISO files, and shared storage are referenced, not copied."
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = duplicateVMName,
                            onValueChange = {
                                duplicateVMName = it.take(64)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New VM name") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleanName = duplicateVMName.trim()
                            if (cleanName.isNotEmpty()) {
                                val duplicatedVM = selectedVM.copy(name = cleanName)
                                val updatedLibrary = savedVMs + duplicatedVM

                                savedVMs = updatedLibrary
                                onVMLibrarySaved(updatedLibrary)
                                rememberSelectedVM(updatedLibrary.lastIndex)

                                showDuplicateVMDialog = false
                                showVMDetails = true

                                RedBoxSupportLog.add(
                                    "Duplicated VM configuration '${selectedVM.name}' as '$cleanName'"
                                )

                                Log.d(
                                    "RedBoxStorage",
                                    "VM duplicated: ${selectedVM.name} -> $cleanName; total=${updatedLibrary.size}"
                                )
                            }
                        },
                        enabled = duplicateVMName.trim().isNotEmpty()
                    ) {
                        Text("Duplicate")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDuplicateVMDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteVMConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteVMConfirmation = false
                },
                title = {
                    Text("Delete virtual machine?")
                },
                text = {
                    Text(
                        "Remove \"${selectedVM.name}\" from RedBox? " +
                            "Your disk images, ISO files, VHD/QCOW2/IMG files, " +
                            "and other source files will not be deleted."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (selectedVMIndex in savedVMs.indices &&
                                !isVMRunning
                            ) {
                                val deletedName =
                                    savedVMs[selectedVMIndex].name

                                val updatedLibrary =
                                    savedVMs.toMutableList().apply {
                                        removeAt(selectedVMIndex)
                                    }

                                savedVMs = updatedLibrary
                                onVMLibrarySaved(updatedLibrary)

                                val nextSelectedIndex =
                                    when {
                                        updatedLibrary.isEmpty() -> -1
                                        selectedVMIndex >= updatedLibrary.size ->
                                            updatedLibrary.lastIndex
                                        else -> selectedVMIndex
                                    }

                                selectedVMIndex = nextSelectedIndex

                                if (nextSelectedIndex >= 0) {
                                    recentVmPreferences
                                        .edit()
                                        .putInt("selected_vm_index", nextSelectedIndex)
                                        .apply()
                                } else {
                                    recentVmPreferences
                                        .edit()
                                        .remove("selected_vm_index")
                                        .apply()
                                }

                                showDeleteVMConfirmation = false
                                showVMDetails = false
                                selectedTab = 1
                                qemuRuntimeStatus = ""

                                Log.d(
                                    "RedBoxStorage",
                                    "VM removed from library only: $deletedName; remaining=${updatedLibrary.size}"
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = Color(0xFFEF5350)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteVMConfirmation = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        return
    }

    if (showFirstSetup) {
        RedBoxMaterialTheme(darkTheme = darkTheme) {
            RedBoxFirstTimeSetup(
                initialName = profileName,
                darkTheme = darkTheme,
                onThemeChanged = { darkTheme = it },
                onFinish = { chosenName ->
                    val safeName = chosenName.trim().take(32).ifEmpty { "User" }
                    profileName = safeName
                    profilePreferences.edit().putString("profile_name", safeName).apply()
                    setupPreferences.edit().putBoolean("completed", true).apply()
                    showFirstSetup = false
                }
            )
        }
        return
    }

    if (showWelcomeScreen) {
        RedBoxMaterialTheme(darkTheme = darkTheme) {
            RedBoxWelcomeScreen(
                profileName = profileName,
                profileAvatarUri = profileAvatarUri
            )
        }
        return
    }

    RedBoxMaterialTheme(darkTheme = darkTheme) {
        Scaffold(
            containerColor =
                MaterialTheme.colorScheme.background,
            bottomBar = {
                RedBoxBottomBar(
                    selectedTab = selectedTab,
                    onSelected = {
                        selectedTab = it
                    }
                )
            }
        ) { innerPadding ->

            when (selectedTab) {
                0 -> RedBoxNextHome(
                    modifier = Modifier.padding(innerPadding),
                    createdVM = selectedVM ?: savedVMs.firstOrNull(),
                    isRunning =
                        isVMRunning &&
                            runningVMIndex ==
                                (if (selectedVM != null) selectedVMIndex else 0),
                    qemuStatus = qemuStatus,
                    onCreateVM = {
                        showCreateVM = true
                    },
                    onVMClick = {
                        if (savedVMs.isNotEmpty()) {
                            if (selectedVMIndex !in savedVMs.indices) {
                                rememberSelectedVM(0)
                            }
                            showVMDetails = true
                        }
                    },
                    onVmsClick = {
                        selectedTab = 1
                    },
                    onQuickAccess = { category ->
                        selectedFileFilter = category
                        selectedTab = 2
                    }
                )

                1 -> RedBoxPremiumLibrary(
                    modifier = Modifier.padding(innerPadding),
                    savedVMs = savedVMs,
                    runningVMIndex = runningVMIndex,
                    isRunning = isVMRunning,
                    onCreateVM = {
                        showCreateVM = true
                    },
                    onVMClick = { index ->
                        if (index in savedVMs.indices) {
                            rememberSelectedVM(index)
                            showVMDetails = true
                        }
                    }
                )

                2 -> RedBoxFilesMaterial(
                    modifier = Modifier.padding(innerPadding),
                    createdVM = selectedVM ?: savedVMs.firstOrNull(),
                    selectedFilter = selectedFileFilter,
                    onFilterChanged = { selectedFileFilter = it }
                )

                else -> RedBoxSettingsMaterial(
                    modifier = Modifier.padding(innerPadding),
                    darkTheme = darkTheme,
                    onDarkThemeChanged = {
                        darkTheme = it
                    },
                    profileName = profileName,
                    profileAvatarUri = profileAvatarUri,
                    onDonate = onDonate,
                    onProfileAvatarChanged = { newUri ->
                        profileAvatarUri = newUri
                        profilePreferences
                            .edit()
                            .putString("profile_avatar_uri", newUri)
                            .apply()
                    },
                    onProfileNameChanged = { newName ->
                        val cleanName =
                            newName.trim().take(32).ifEmpty { "User" }

                        profileName = cleanName
                        profilePreferences
                            .edit()
                            .putString("profile_name", cleanName)
                            .apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun RedBoxFirstTimeSetup(
    initialName: String,
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onFinish: (String) -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf(initialName) }
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 42.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.redbox_logo),
            contentDescription = "RedBox logo",
            modifier = Modifier.size(82.dp)
        )
        Text("WELCOME TO REDBOX", color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Make it yours.", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Set up your personal PC emulator experience. No account or internet connection required.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("YOUR PROFILE", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(32) },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Text("APPEARANCE", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Dark mode", fontWeight = FontWeight.SemiBold)
                        Text("Choose your preferred appearance", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = darkTheme, onCheckedChange = onThemeChanged)
                }
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Button(
            onClick = { onFinish(displayName) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) { Text("Start using RedBox", fontWeight = FontWeight.Bold) }
        Text("Your virtual machines and local files remain on your device.",
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RedBoxWelcomeScreen(
    profileName: String,
    profileAvatarUri: String
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 7 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)) {
                Image(painter = painterResource(R.drawable.redbox_logo),
                    contentDescription = "RedBox", modifier = Modifier.size(104.dp))
                Spacer(Modifier.height(24.dp))
                Text("REDBOX", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Welcome back,", fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(profileName, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                RedBoxProfileAvatar(avatarUri = profileAvatarUri, size = 58,
                    fallbackLetter = profileName.firstOrNull()?.uppercase() ?: "R")
                Spacer(Modifier.height(30.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Preparing your workspace", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RedBoxMaterialTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFFF3345),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF5A111A),
        onPrimaryContainer = Color(0xFFFFDAD9),
        secondary = Color(0xFFBFC6D1),
        background = Color(0xFF07090C),
        surface = Color(0xFF101318),
        surfaceVariant = Color(0xFF1A1E24),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFC2C7D0)
    )

    val lightColors = lightColorScheme(
        primary = Color(0xFFD7192B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDADC),
        onPrimaryContainer = Color(0xFF4A0710),
        background = Color(0xFFF7F7F9),
        surface = Color.White,
        surfaceVariant = Color(0xFFE9E9EE),
        onBackground = Color(0xFF15161A),
        onSurface = Color(0xFF15161A),
        onSurfaceVariant = Color(0xFF555862)
    )

    MaterialTheme(
        colorScheme =
            if (darkTheme) darkColors else lightColors,
        typography = Typography(),
        content = content
    )
}

@Composable
private fun RedBoxBottomBar(
    selectedTab: Int,
    onSelected: (Int) -> Unit
) {
    val labels = listOf("Home", "VMs", "Files", "Settings")
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.List,
        Icons.Filled.Menu,
        Icons.Filled.Settings
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xF210141D),
            tonalElevation = 10.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x55FF2A42),
                        Color(0x1AFFFFFF),
                        Color(0x2A2979FF)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                labels.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    val highlight by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(durationMillis = 260),
                        label = "navHighlight"
                    )
                    val iconSize by animateDpAsState(
                        targetValue = if (selected) 25.dp else 22.dp,
                        animationSpec = tween(durationMillis = 260),
                        label = "navIcon"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(accent.copy(alpha = 0.14f * highlight))
                            .clickable { onSelected(index) }
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = label,
                            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 18.dp else 4.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(accent.copy(alpha = highlight))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedBoxHomeMaterial(
    modifier: Modifier,
    createdVM: VMModel?,
    isRunning: Boolean,
    qemuStatus: String,
    onCreateVM: () -> Unit,
    onVMClick: () -> Unit,
    onVmsClick: () -> Unit,
    onQuickAccess: (String) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val red = Color(0xFFFF454F)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = red) {
                Text("R", modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("REDBOX", fontWeight = FontWeight.Black, fontSize = 22.sp,
                    letterSpacing = 2.sp)
                Text("YOUR VIRTUAL WORKSPACE", fontSize = 9.sp,
                    letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text("v1.1.0", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(30.dp))
        AnimatedVisibility(visible = revealed, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 5 }) {
            Column {
                Text("YOUR COMMAND", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp, color = red)
                Text("Your PC.\nYour rules.", fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp)
                Spacer(Modifier.height(18.dp))
                Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF3A292D), shape = RoundedCornerShape(50)) {
                                Text(if (isRunning) "Ã¢â€”Â  RUNNING" else "Ã¢â€”Â  READY",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = red)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("Ã¢â€ â€”", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 23.sp)
                        }
                        Spacer(Modifier.height(30.dp))
                        Text("FEATURED MACHINE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                            letterSpacing = 2.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(createdVM?.name ?: "Your first PC awaits", color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(if (createdVM == null) "Build a virtual machine to get started."
                            else if (isRunning) "Your virtual machine is running"
                            else "Configured and ready for your next session",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(25.dp))
                        Button(onClick = if (createdVM == null) onCreateVM else onVMClick,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = red)) {
                            Text(if (createdVM == null) "+  CREATE FIRST MACHINE" else "OPEN MACHINE  Ã¢â€ â€™",
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QUICK ACTIONS", fontSize = 13.sp, letterSpacing = 1.7.sp,
                fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("EXPLORE  Ã¢â€ â€™", modifier = Modifier.clickable(onClick = onVmsClick),
                fontSize = 11.sp, color = red, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextActionTile("Ã¯Â¼â€¹", "New PC", "Create a VM", Modifier.weight(1f), onCreateVM)
            NextActionTile("Ã¢â€“Â£", "My VMs", "Manage library", Modifier.weight(1f), onVmsClick)
        }
        Spacer(Modifier.height(28.dp))
        Text("EXPLORE SYSTEMS", fontSize = 13.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Windows", "Linux", "Android").forEach { name ->
                Surface(modifier = Modifier.weight(1f).clickable { onQuickAccess(name) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(when (name) { "Windows" -> "Ã¢Å Å¾"; "Linux" -> "Ã¢Å’Ëœ"; else -> "Ã¢â€”Ë†" },
                            fontSize = 25.sp, color = red)
                        Spacer(Modifier.height(7.dp))
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("ENGINE STATUS", fontSize = 13.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ã¢â€”â€°", fontSize = 25.sp, color = red)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("RedBox Engine", fontWeight = FontWeight.Bold)
                    Text(qemuStatus.ifBlank { "Emulation core standing by" }, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NextActionTile(symbol: String, title: String, subtitle: String,
    modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(19.dp)) {
            Text(symbol, fontSize = 31.sp, color = Color(0xFFFF454F))
            Spacer(Modifier.height(20.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(subtitle, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RedBoxHeroCard(
    hasVm: Boolean,
    onCreateVM: () -> Unit,
    onVmsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "REDBOX",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasVm) "Ready when you are." else "Build your virtual PC.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = if (hasVm) {
                    "Continue your recent VM or create another machine."
                } else {
                    "Create a VM, attach your disk or ISO, and start emulating."
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateVM,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Ã¯Â¼â€¹  New VM",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasVm) {
                    OutlinedButton(
                        onClick = onVmsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(
                            text = "My VMs",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = "$action  Ã¢â‚¬Âº",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyVMCard(onCreateVM: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Ã¢â€“Â£",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = "No virtual machines yet",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Your saved machines will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(15.dp))

            OutlinedButton(
                onClick = onCreateVM,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create your first VM")
            }
        }
    }
}

@Composable
private fun VMHomeCard(
    vm: VMModel,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ã¢â€“Â£",
                            fontSize = 25.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vm.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = vm.architecture,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                StatusPill(isRunning)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    VMStat(
                        modifier = Modifier.weight(1f),
                        label = "Memory",
                        value = vm.ram
                    )
                    VMStat(
                        modifier = Modifier.weight(1f),
                        label = "CPU",
                        value = "${vm.cpuCores} core${if (vm.cpuCores == "1") "" else "s"}"
                    )
                    VMStat(
                        modifier = Modifier.weight(1f),
                        label = "Machine",
                        value = when (vm.machineType) {
                            "isapc" -> "ISA PC"
                            "q35" -> "Q35"
                            else -> "i440FX"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val storageName =
                when {
                    vm.diskImageName.isNotEmpty() -> vm.diskImageName
                    vm.isoImageName.isNotEmpty() -> vm.isoImageName
                    else -> "No disk image selected"
                }

            Text(
                text = storageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Open virtual machine  Ã¢â‚¬Âº",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RowScope.VMStat(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusPill(isRunning: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isRunning) Color(0xFF123D28) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isRunning) "Ã¢â€”Â Running" else "Stopped",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (isRunning) Color(0xFF62E59B) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = icon,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EngineCard(qemuStatus: String) {
    val engineReady = qemuStatus.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Q",
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QEMU Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (engineReady) "Engine loaded and ready" else "Checking emulator engine",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (engineReady) Color(0xFF123D28) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (engineReady) "Ã¢â€”Â Ready" else "Ã¢â‚¬Â¦",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (engineReady) Color(0xFF62E59B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun RedBoxSectionBanner(
    eyebrow: String,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = eyebrow.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RedBoxVMsMaterial(
    modifier: Modifier,
    savedVMs: List<VMModel>,
    runningVMIndex: Int,
    isRunning: Boolean,
    onCreateVM: () -> Unit,
    onVMClick: (Int) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val visibleVMs = savedVMs.mapIndexed { index, vm -> index to vm }
        .filter { (index, _) ->
            when (selectedFilter) {
                "Running" -> isRunning && runningVMIndex == index
                "Stopped" -> !(isRunning && runningVMIndex == index)
                else -> true
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Virtual machines",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (savedVMs.size) {
                        0 -> "Create your first PC in RedBox"
                        1 -> "1 machine in your library"
                        else -> "${savedVMs.size} machines in your library"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onCreateVM,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("+  New VM", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        RedBoxSectionBanner(
            eyebrow = "YOUR WORKSPACE",
            title = "All your virtual PCs",
            description = "Create, organize and launch machines from one place."
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Library",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a machine to view its hardware, storage and controls.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Running", "Stopped").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {
            savedVMs.isEmpty() -> EmptyVMCard(onCreateVM)

            visibleVMs.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedFilter == "Running") "Ã¢â€“Â¶" else "Ã¢â€“Â ",
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedFilter == "Running") {
                                "No running machines"
                            } else {
                                "No stopped machines"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (selectedFilter == "Running") {
                                "Start a VM and it will appear in this view."
                            } else {
                                "All saved VMs are currently running."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            else -> {
                visibleVMs.forEachIndexed { visibleIndex, (originalIndex, vm) ->
                    val running = isRunning && runningVMIndex == originalIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVMClick(originalIndex) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Ã¢â€“Â£",
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(13.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vm.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${vm.architecture}  Ã¢â‚¬Â¢  ${vm.ram}  Ã¢â‚¬Â¢  ${vm.cpuCores} core${if (vm.cpuCores == "1") "" else "s"}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (running) Color(0xFF123D28) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (running) "Ã¢â€”Â Running" else "Stopped",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = if (running) Color(0xFF62E59B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(13.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(11.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        vm.diskImageName.isNotBlank() -> "Disk: ${vm.diskImageName}"
                                        vm.isoImageName.isNotBlank() -> "ISO: ${vm.isoImageName}"
                                        else -> "No storage selected"
                                    },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Open  Ã¢â‚¬Âº",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (visibleIndex != visibleVMs.lastIndex) {
                        Spacer(modifier = Modifier.height(11.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun RedBoxFilesMaterial(
    modifier: Modifier,
    createdVM: VMModel?,
    selectedFilter: String,
    onFilterChanged: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 18.dp,
                vertical = 14.dp
            )
    ) {
        Text(
            text = "Files",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Disk images and ISO files",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(14.dp))
        RedBoxSectionBanner(
            eyebrow = "DISK LIBRARY",
            title = "Your operating system media",
            description = "Browse ISO images and virtual disks for your machines."
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Windows", "Android", "Linux").forEach { option ->
                FilterChip(
                    selected = selectedFilter == option,
                    onClick = { onFilterChanged(option) },
                    label = { Text(option) }
                )
            }
        }

        if (selectedFilter != "All") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$selectedFilter quick access",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        FileCategoryCard(
            title = "Disk Image",
            subtitle =
                createdVM?.diskImageName
                    ?.takeIf { it.isNotEmpty() }
                    ?: "No disk image selected",
            icon = Icons.Default.Storage
        )

        Spacer(modifier = Modifier.height(12.dp))

        FileCategoryCard(
            title = "ISO Image",
            subtitle =
                createdVM?.isoImageName
                    ?.takeIf { it.isNotEmpty() }
                    ?: "No ISO image selected",
            icon = Icons.Default.Album
        )

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Supported formats",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = ".img  \u2022  .qcow2  \u2022  .vhd  \u2022  .iso",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FileCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = Color(0xFFE50914)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconTint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun RedBoxProfileAvatar(
    avatarUri: String,
    size: Int,
    fallbackLetter: String
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            try {
                context.contentResolver
                    .openInputStream(Uri.parse(avatarUri))
                    ?.use { BitmapFactory.decodeStream(it) }
            } catch (error: Exception) {
                Log.w("RedBoxProfile", "Could not load profile avatar", error)
                null
            }
        }
    }

    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = "Profile avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = fallbackLetter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (size * 0.46f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RedBoxSettingsMaterial(
    modifier: Modifier,
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    profileName: String,
    profileAvatarUri: String,
    onDonate: () -> Unit,
    onProfileAvatarChanged: (String) -> Unit,
    onProfileNameChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("Tap Check Now to look for GitHub releases.") }
    val githubReleasesUrl = "https://github.com/RedBoxEmu/RedBox-PC-Emulator/releases"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    fun openUpdateLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (error: Exception) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
    fun checkGitHubRelease() {
        if (updateChecking) return
        updateChecking = true
        updateMessage = "Checking GitHub Releases..."
        Thread {
            val message = try {
                val connection = URL("https://api.github.com/repos/RedBoxEmu/RedBox-PC-Emulator/releases/latest")
                    .openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.setRequestProperty("User-Agent", "RedBox-Android-Update-Checker")
                    val status = connection.responseCode
                    if (status == 200) {
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val tag = json.optString("tag_name").trim()
                        val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
                        val latest = tag.removePrefix("v").removePrefix("V")
                        val installedParts = installed.split('.').mapNotNull { it.toIntOrNull() }
                        val latestParts = latest.split('.').mapNotNull { it.toIntOrNull() }
                        val newer = if (installedParts.isNotEmpty() && latestParts.isNotEmpty() && installedParts.size == installed.split('.').size && latestParts.size == latest.split('.').size) {
                            (0 until maxOf(installedParts.size, latestParts.size)).firstNotNullOfOrNull { index ->
                                val difference = latestParts.getOrElse(index) { 0 }.compareTo(installedParts.getOrElse(index) { 0 })
                                if (difference == 0) null else difference
                            }?.let { it > 0 }
                        } else null
                        "Installed: $installed\nLatest GitHub release: $tag\n\n" + when (newer) {
                            true -> "A newer release is available on GitHub."
                            false -> "You are up to date with the latest GitHub release."
                            null -> "Could not compare version numbers. Review the release page."
                        }
                    } else if (status == 404) {
                        "No published GitHub release was found yet. You can still check the repository or Google Play."
                    } else {
                        "GitHub update check failed (HTTP $status). Please try again later."
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                "Could not check GitHub. Check your internet connection and try again."
            }
            Handler(Looper.getMainLooper()).post {
                updateMessage = message
                updateChecking = false
            }
        }.start()
    }

    var showStorageDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showSupportLogDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var profileNameDraft by remember(profileName) {
        mutableStateOf(profileName)
    }

    val profileAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (error: Exception) {
                Log.w("RedBoxProfile", "Could not persist avatar permission", error)
            }
            onProfileAvatarChanged(uri.toString())
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = {
                profileNameDraft = profileName
                showProfileDialog = false
            },
            title = { Text("RedBox Profile") },
            text = {
                Column {
                    Text(
                        text = "Choose the name shown on the RedBox welcome screen. This is stored only on this device.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RedBoxProfileAvatar(
                            avatarUri = profileAvatarUri,
                            size = 72,
                            fallbackLetter = profileNameDraft.firstOrNull()?.uppercase() ?: "R"
                        )

                        Column {
                            OutlinedButton(
                                onClick = {
                                    profileAvatarPicker.launch(arrayOf("image/*"))
                                }
                            ) {
                                Text(
                                    if (profileAvatarUri.isBlank()) {
                                        "Choose picture"
                                    } else {
                                        "Change picture"
                                    }
                                )
                            }

                            if (profileAvatarUri.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        onProfileAvatarChanged("")
                                    }
                                ) {
                                    Text("Remove picture")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = profileNameDraft,
                        onValueChange = {
                            if (it.length <= 32) {
                                profileNameDraft = it
                            }
                        },
                        label = { Text("Profile name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onProfileNameChanged(profileNameDraft)
                        showProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        profileNameDraft = profileName
                        showProfileDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About RedBox") },
            text = {
                Text(
                    "RedBox PC Emulator\nVersion 1.1.0\n\n" +
                            "A QEMU-based PC emulator for Android.\n\n" +
                            "Run. Explore. Create."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = { Text("What's New in v1.1.0") },
            text = {
                Column(
                    modifier = Modifier
                        .height(420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Version 1.1.0 â€” Obsidian Redesign & Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Welcome to RedBox 1.1.0! A major upgrade introducing our Obsidian Cyberdeck UI, categorized VM studio, and enhanced system controls.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("âœ¨ Obsidian Cyberdeck Design", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Sleek OLED dark interface with Crimson accents, dynamic status indicators, and live hardware telemetry (CPU cores, available RAM, QEMU engine health).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("âš¡ Categorized 'Edit VM' Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("New modular 4-tab configuration (Compute, Storage, Display & Sound, System) featuring quick 1-tap selectors for RAM, Cores, and GPUs with an instant top-bar Save button.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("ðŸ–¥ï¸ Enhanced Viewport HUD & Safe Shutdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Floating translucent toolbar with keyboard/mouse shortcuts, screen scaling toggles, and safe ACPI shutdown prompts to protect guest disk state.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("ðŸ“š Premium VM Library & Refined Diagnostics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Instant search and filter chips across machines, updated diagnostic logs, and official RedBox v1.1.0 release alignment.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhatsNewDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Check for Update") },
            text = {
                Column {
                    Text(updateMessage)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (updateChecking) CircularProgressIndicator()
                    TextButton(onClick = { checkGitHubRelease() }, enabled = !updateChecking) {
                        Text("Check Now")
                    }
                    TextButton(onClick = { openUpdateLink(githubReleasesUrl) }) {
                        Text("Open GitHub Releases")
                    }
                    TextButton(onClick = { openUpdateLink(playStoreUrl) }) {
                        Text("Open Google Play")
                    }
                    Text("Google Play opens your app listing so users can check for updates there. Availability depends on publication in the Play Store.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Close") }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage Location") },
            text = {
                Text(
                    "RedBox internal storage:\n${context.filesDir.absolutePath}\n\n" +
                            "Temporary cache:\n${context.cacheDir.absolutePath}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSupportLogDialog) {
        val report = RedBoxSupportLog.buildReport()

        AlertDialog(
            onDismissRequest = { showSupportLogDialog = false },
            title = { Text("Support Diagnostic Log") },
            text = {
                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Copy or share this log when reporting a RedBox problem. It contains app/device details and recent RedBox events, but is designed not to include full disk or ISO content URIs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(report, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "RedBox 1.1.0 Support Log")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share RedBox Support Log")
                        )
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("RedBox Support Log", report)
                            )
                            Toast.makeText(
                                context,
                                "Support log copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text("Copy")
                    }

                    TextButton(
                        onClick = {
                            RedBoxSupportLog.clear()
                            Toast.makeText(
                                context,
                                "Support log cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                            showSupportLogDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Delete RedBox temporary cache files?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleared = try {
                            context.cacheDir.listFiles()?.forEach { file ->
                                file.deleteRecursively()
                            }
                            true
                        } catch (error: Exception) {
                            Log.e("RedBoxSettings", "Failed to clear cache", error)
                            false
                        }

                        Toast.makeText(
                            context,
                            if (cleared) "Cache cleared" else "Could not clear cache",
                            Toast.LENGTH_SHORT
                        ).show()

                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val installedVersion = remember(context.packageName) {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.0" }
        catch (_: Exception) { "1.1.0" }
    }
    var selectedCategory by rememberSaveable { mutableStateOf("Personal") }
    val categories = listOf("Personal", "App & tools", "Community")

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("REDBOX / CONTROL CENTER", color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Your settings.", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Your PC. Your rules.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp)
            }
            Image(painter = painterResource(id = R.drawable.redbox_logo),
                contentDescription = "RedBox logo", modifier = Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(18.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RedBoxProfileAvatar(avatarUri = profileAvatarUri, size = 58,
                        fallbackLetter = profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "R")
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("WELCOME BACK", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f))
                        Text(profileName, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("RedBox member", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                    }
                }
                Spacer(Modifier.height(15.dp))
                Button(onClick = { profileNameDraft = profileName; showProfileDialog = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Edit profile & avatar")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("PREFERENCES", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            categories.forEach { category ->
                FilterChip(selected = selectedCategory == category,
                    onClick = { selectedCategory = category }, label = {
                        Text(category, fontSize = 11.sp, maxLines = 1)
                    }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))

        androidx.compose.animation.AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(animationSpec = tween(220)) +
                    androidx.compose.animation.slideInVertically(animationSpec = tween(220)) { it / 8 })
                    .togetherWith(androidx.compose.animation.fadeOut(animationSpec = tween(120)))
            }, label = "Settings categories"
        ) { category ->
            Column {
                when (category) {
                    "Personal" -> {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Default.DarkMode,
                                title = "Appearance",
                                subtitle = if (darkTheme) "Dark mode enabled" else "Light mode enabled",
                                trailing = { Switch(checked = darkTheme, onCheckedChange = onDarkThemeChanged) },
                                onClick = { onDarkThemeChanged(!darkTheme) }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.Person,
                                title = "Profile & avatar",
                                subtitle = "Change your display name and photo",
                                onClick = { profileNameDraft = profileName; showProfileDialog = true }
                            )
                        }
                    }
                    "App & tools" -> {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Default.Refresh,
                                title = "Check for updates",
                                subtitle = "Installed version $installedVersion",
                                onClick = { showUpdateDialog = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.AutoAwesome,
                                title = "What's new",
                                subtitle = "See the release notes",
                                onClick = { showWhatsNewDialog = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.Folder,
                                title = "Storage location",
                                subtitle = "View RedBox's internal storage",
                                onClick = { showStorageDialog = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.Terminal,
                                title = "Diagnostic log",
                                subtitle = "Copy or share emulator events",
                                onClick = { showSupportLogDialog = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.DeleteSweep,
                                title = "Clear cache",
                                subtitle = "Remove temporary files safely",
                                onClick = { showClearCacheDialog = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.Info,
                                title = "About RedBox",
                                subtitle = "App information and credits",
                                onClick = { showAboutDialog = true }
                            )
                        }
                    }
                    else -> {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Default.PlayCircle,
                                iconTint = Color(0xFFFF0000),
                                title = "RimvydopPlus \u2014 YouTube",
                                subtitle = "@rimvydoplus",
                                onClick = {
                                    openUpdateLink("https://www.youtube.com/@rimvydoplus")
                                }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.PlayCircle,
                                iconTint = Color(0xFFFF0000),
                                title = "RedBox \u2014 YouTube",
                                subtitle = "@RedBox-pc",
                                onClick = {
                                    openUpdateLink("https://www.youtube.com/@RedBox-pc")
                                }
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Default.Forum,
                                iconTint = Color(0xFF5865F2),
                                title = "RedBox Discord",
                                subtitle = "Join our community",
                                onClick = {
                                    openUpdateLink("https://discord.com/invite/aDcg5mqFc5")
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.redbox_logo),
                    contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("RedBox PC Emulator", fontWeight = FontWeight.Bold)
                    Text("Version $installedVersion  \u2022  Powered by QEMU",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Run. Explore. Create.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }

}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color = Color(0xFFE50914),
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = 17.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}


@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun RedBoxLogo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            id = R.drawable.redbox_logo
        ),
        contentDescription = "RedBox PC Emulator",
        modifier = modifier
    )
}

private fun validateVMForStart(vm: VMModel): String? {
    if (vm.name.trim().isEmpty()) {
        return "VM name is required."
    }

    val ramMb = vm.ram.filter { it.isDigit() }.toIntOrNull()
    if (ramMb == null || ramMb <= 0) {
        return "Select a valid RAM amount before starting the VM."
    }

    val cores = vm.cpuCores.filter { it.isDigit() }.toIntOrNull()
    if (cores == null || cores <= 0) {
        return "Select a valid CPU core count before starting the VM."
    }

    return null
}

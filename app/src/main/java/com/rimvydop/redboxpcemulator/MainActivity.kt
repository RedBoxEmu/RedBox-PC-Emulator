package com.rimvydop.redboxpcemulator

import android.view.KeyEvent

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
            appendLine("App version: 0.2.1")
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
            Toast.makeText(this, "Loading donation…", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Thank you for supporting RedBox! ❤️", Toast.LENGTH_LONG).show()
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
        RedBoxSupportLog.add("RedBox 0.2.1 started; native QEMU status: $qemuStatus")

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

    var showWelcomeScreen by rememberSaveable {
        mutableStateOf(true)
    }

    LaunchedEffect(showWelcomeScreen) {
        if (showWelcomeScreen) {
            delay(1800L)
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
                            "Validation blocked VM start: ${selectedVM.name} — $validationError"
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
                0 -> RedBoxHomeMaterial(
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

                1 -> RedBoxVMsMaterial(
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
private fun RedBoxWelcomeScreen(
    profileName: String,
    profileAvatarUri: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RedBoxProfileAvatar(
                avatarUri = profileAvatarUri,
                size = 92,
                fallbackLetter = profileName.firstOrNull()?.uppercase() ?: "R"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = profileName,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "RedBox PC Emulator",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    Surface(
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RedBoxNavigationItem(
                        selected = selectedTab == 0,
                        icon = "⌂",
                        label = "Home",
                        onClick = { onSelected(0) }
                    )

                    RedBoxNavigationItem(
                        selected = selectedTab == 1,
                        icon = "▣",
                        label = "VMs",
                        onClick = { onSelected(1) }
                    )

                    RedBoxNavigationItem(
                        selected = selectedTab == 2,
                        icon = "□",
                        label = "Files",
                        onClick = { onSelected(2) }
                    )

                    RedBoxNavigationItem(
                        selected = selectedTab == 3,
                        icon = "⚙",
                        label = "Settings",
                        onClick = { onSelected(3) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RedBoxNavigationItem(
    selected: Boolean,
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 3.dp),
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = if (selected) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight =
                    if (selected) FontWeight.Bold else FontWeight.Medium,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            )
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
            RedBoxLogo(modifier = Modifier.size(48.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RedBox PC Emulator",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your virtual PCs in one place",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "v0.2.1",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        RedBoxHeroCard(
            hasVm = createdVM != null,
            onCreateVM = onCreateVM,
            onVmsClick = onVmsClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(
            title = if (createdVM != null) "Continue" else "Virtual Machines",
            action = if (createdVM != null) "View all" else null,
            onAction = onVmsClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (createdVM == null) {
            EmptyVMCard(onCreateVM)
        } else {
            VMHomeCard(
                vm = createdVM,
                isRunning = isRunning,
                onClick = onVMClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quick Access",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                icon = "▣",
                title = "Windows",
                subtitle = "PC images",
                onClick = { onQuickAccess("Windows") }
            )

            QuickAccessCard(
                modifier = Modifier.weight(1f),
                icon = "●",
                title = "Android",
                subtitle = "x86 images",
                onClick = { onQuickAccess("Android") }
            )

            QuickAccessCard(
                modifier = Modifier.weight(1f),
                icon = "◈",
                title = "Linux",
                subtitle = "Distributions",
                onClick = { onQuickAccess("Linux") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "System")

        Spacer(modifier = Modifier.height(10.dp))

        EngineCard(qemuStatus)

        Spacer(modifier = Modifier.height(18.dp))
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
                        text = "＋  New VM",
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
                    text = "$action  ›",
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
                        text = "▣",
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
                            text = "▣",
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
                text = "Open virtual machine  ›",
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
            text = if (isRunning) "● Running" else "Stopped",
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
                    text = if (engineReady) "● Ready" else "…",
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
                            text = if (selectedFilter == "Running") "▶" else "■",
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
                                            text = "▣",
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
                                        text = "${vm.architecture}  •  ${vm.ram}  •  ${vm.cpuCores} core${if (vm.cpuCores == "1") "" else "s"}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (running) Color(0xFF123D28) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (running) "● Running" else "Stopped",
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
                                    text = "Open  ›",
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
            icon = "□"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FileCategoryCard(
            title = "ISO Image",
            subtitle =
                createdVM?.isoImageName
                    ?.takeIf { it.isNotEmpty() }
                    ?: "No ISO image selected",
            icon = "◉"
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
                    text = ".img  •  .qcow2  •  .vhd  •  .iso",
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
    icon: String
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
                color =
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = icon,
                        fontSize = 23.sp,
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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

            Text(
                text = "›",
                fontSize = 27.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
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
                    "RedBox PC Emulator\nVersion 0.2.1\n\n" +
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
            title = { Text("What's New in v0.2.1") },
            text = {
                Column(
                    modifier = Modifier
                        .height(420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Profile avatar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Choose your own profile picture for the RedBox welcome screen, change it later, or remove it at any time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("ISA PC (Legacy)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "A new legacy ISA PC machine type is available alongside PC (i440FX) and Q35 for older guest operating systems.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Redesigned Edit VM", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Edit VM has been reorganized into a cleaner configuration screen while keeping the existing CPU, memory, machine, display, storage, network, audio, boot, firmware, and advanced options.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Mouse sensitivity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Change VM mouse sensitivity between Low, Normal, and High from the runtime settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Display Refresh", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "A Refresh control is available in the VM fullscreen toolbar to refresh the active RedBox display surface without restarting the VM.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Start without disk or ISO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "RedBox can now start a VM even when no main disk image or installation ISO is attached.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Donate Me", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "A one-time Donate Me option has been added through Google Play Billing to support RedBox development.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
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
                Text(
                    "Installed version: 0.2.1\n\n" +
                            "The update button is working. An online update source can be connected later when RedBox has a release page or update server."
                )
            },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("OK")
                }
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
                            putExtra(Intent.EXTRA_SUBJECT, "RedBox 0.2.1 Support Log")
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
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingsCard {
            SettingsRow(
                icon = "☺",
                title = "Profile",
                subtitle = "$profileName • Name & avatar",
                onClick = {
                    profileNameDraft = profileName
                    showProfileDialog = true
                }
            )

            SettingsDivider()

            SettingsRow(
                icon = "♥",
                title = "Donate Me — €4.99",
                subtitle = "Support RedBox development with a one-time donation",
                onClick = onDonate
            )

            SettingsDivider()

            SettingsRow(
                icon = "☼",
                title = "Dark Theme",
                subtitle =
                    if (darkTheme) {
                        "Dark appearance"
                    } else {
                        "Light appearance"
                    },
                trailing = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange =
                            onDarkThemeChanged
                    )
                }
            )

            SettingsDivider()

            SettingsRow(
                icon = "ⓘ",
                title = "About RedBox",
                subtitle = "RedBox PC Emulator • Version 0.2.1",
                onClick = { showAboutDialog = true }
            )

            SettingsDivider()

            SettingsRow(
                icon = "★",
                title = "What's New",
                subtitle = "See what's new in version 0.2.1",
                onClick = { showWhatsNewDialog = true }
            )

            SettingsDivider()

            SettingsRow(
                icon = "↻",
                title = "Check for Update",
                subtitle = "Version 0.2.1",
                onClick = { showUpdateDialog = true }
            )

            SettingsDivider()

            SettingsRow(
                icon = "□",
                title = "Storage Location",
                subtitle = "Internal Storage",
                onClick = { showStorageDialog = true }
            )

            SettingsDivider()

            SettingsRow(
                icon = "≡",
                title = "Support Diagnostic Log",
                subtitle = "Copy or share recent RedBox events",
                onClick = { showSupportLogDialog = true }
            )

            SettingsDivider()

            SettingsRow(
                icon = "⌫",
                title = "Clear Cache",
                subtitle = "Manage temporary files",
                onClick = { showClearCacheDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                RedBoxLogo(
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "RedBox PC Emulator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Version 0.2.1",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Run. Explore. Create.",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Material Design 3  •  RedBox",
            modifier = Modifier.fillMaxWidth(),
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
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
    icon: String,
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
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 23.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(34.dp)
        )

        Spacer(modifier = Modifier.width(9.dp))

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
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        if (trailing != null) {
            trailing()
        } else {
            Text(
                text = "›",
                fontSize = 25.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
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
            id = R.drawable.ic_redbox
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VMDetailsScreen(
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
    RedBoxMaterialTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "VM details",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(
                                text = "‹",
                                fontSize = 30.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onEditVM,
                            enabled = !isRunning
                        ) {
                            Text(if (isRunning) "Locked" else "Edit")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(66.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "▣",
                                        fontSize = 30.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vm.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${vm.architecture}  •  ${vm.ram}  •  ${vm.cpuCores} core${if (vm.cpuCores == "1") "" else "s"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isRunning) Color(0xFF123D28) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isRunning) "● Running" else "Stopped",
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    color = if (isRunning) Color(0xFF62E59B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when {
                                vm.diskImageName.isNotBlank() && vm.isoImageName.isNotBlank() ->
                                    "Disk and installation media are attached."
                                vm.diskImageName.isNotBlank() ->
                                    "Disk image attached. No primary ISO selected."
                                vm.isoImageName.isNotBlank() ->
                                    "Installation media attached. No disk image selected."
                                else ->
                                    "No boot storage or installation media selected."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isRunning) {
                            Button(
                                onClick = onReturnToVM,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("▶  Return to VM", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(9.dp))

                            OutlinedButton(
                                onClick = onStopVM,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF5350)
                                )
                            ) {
                                Text("Stop VM", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = onStartVM,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("▶  Start VM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (qemuRuntimeStatus.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isRunning) "QEMU engine" else "VM status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = qemuRuntimeStatus,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Hardware", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(9.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        DetailRowMaterial("▤", "Memory", vm.ram)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("C", "Processor", "${vm.cpuCores} core${if (vm.cpuCores == "1") "" else "s"} • ${vm.cpuModel}")
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial(
                            "M",
                            "Machine",
                            when (vm.machineType) {
                                "isapc" -> "ISA PC (Legacy)"
                                "q35" -> "Q35"
                                else -> "PC (i440FX)"
                            }
                        )
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("G", "Display", vm.displayAdapter)
                        DetailRowMaterial(
                            "3D",
                            "3D Acceleration",
                            if (vm.displayAdapter == "VirtIO VGA" && vm.threeDAcceleration) "Enabled" else "Disabled"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Storage", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(9.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        StorageRowMaterial("□", "Disk image", vm.diskImageName.ifBlank { "Not selected" })
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        StorageRowMaterial("◉", "Primary ISO", vm.isoImageName.ifBlank { "Not selected" })
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        StorageRowMaterial("◌", "Driver ISO", vm.driverIsoImageName.ifBlank { "Not selected" })
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        StorageRowMaterial("▱", "Shared hard drive", vm.sharedDiskImageName.ifBlank { "Not selected" })
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("D", "Disk interface", vm.diskInterface)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Removable media", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(9.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Primary installation ISO",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            vm.isoImageName.ifBlank { "No ISO mounted" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (vm.isoImage.isBlank()) "Mount ISO" else "Change ISO")
                            }
                            OutlinedButton(
                                onClick = onEjectPrimaryIso,
                                enabled = !isRunning && vm.isoImage.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Eject")
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        Text(
                            "Driver / tools ISO",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            vm.driverIsoImageName.ifBlank { "No driver ISO mounted" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (vm.driverIsoImage.isBlank()) "Mount ISO" else "Change ISO")
                            }
                            OutlinedButton(
                                onClick = onEjectDriverIso,
                                enabled = !isRunning && vm.driverIsoImage.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Eject")
                            }
                        }

                        if (isRunning) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Stop the VM before changing removable media.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Devices & engine", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(9.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        DetailRowMaterial("◈", "Performance", "${vm.performancePreset} • ${vm.tcgCache}")
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("T", "Multi-threaded TCG", if (vm.multiThreadedTcg) "Enabled" else "Disabled")
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("N", "Network", if (vm.networkEnabled) "${vm.networkAdapter} • ${vm.networkMode}" else "Disabled")
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("♪", "Sound", vm.soundCard)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        DetailRowMaterial("A", "Custom QEMU parameters", if (vm.qemuParams.isBlank()) "None" else "Active")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onDuplicateVM,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("⧉  Duplicate VM")
                }

                Spacer(modifier = Modifier.height(9.dp))

                OutlinedButton(
                    onClick = onEditVM,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isRunning) "Edit VM (stop VM first)" else "✎  Edit configuration")
                }

                Spacer(modifier = Modifier.height(9.dp))

                OutlinedButton(
                    onClick = onDeleteVM,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    Text(if (isRunning) "Delete VM (stop VM first)" else "Delete VM")
                }

                Spacer(modifier = Modifier.height(9.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to VM library")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsGroupTitle(
    title: String,
    subtitle: String
) {
    Spacer(modifier = Modifier.height(18.dp))
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = subtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVMScreen(
    vm: VMModel,
    onBack: () -> Unit,
    onSave: (VMModel) -> Unit
) {
    val context = LocalContext.current

    var name by remember(vm) { mutableStateOf(vm.name) }
    var architecture by remember(vm) { mutableStateOf(vm.architecture) }
    var ram by remember(vm) { mutableStateOf(vm.ram) }
    var cpuCores by remember(vm) { mutableStateOf(vm.cpuCores) }

    var performancePreset by remember(vm) { mutableStateOf(vm.performancePreset) }
    var cpuModel by remember(vm) { mutableStateOf(vm.cpuModel) }
    var cpuFlags by remember(vm) { mutableStateOf(vm.cpuFlags) }
    var tcgCache by remember(vm) { mutableStateOf(vm.tcgCache) }
    var multiThreadedTcg by remember(vm) { mutableStateOf(vm.multiThreadedTcg) }
    var machineType by remember(vm) { mutableStateOf(vm.machineType) }
    var showCpuOptions by remember(vm) { mutableStateOf(false) }
    var showRamOptions by remember(vm) { mutableStateOf(false) }
    var showMachineOptions by remember(vm) { mutableStateOf(false) }
    var diskInterface by remember(vm) { mutableStateOf(vm.diskInterface) }
    var displayAdapter by remember(vm) { mutableStateOf(vm.displayAdapter) }
    var threeDAcceleration by remember(vm) { mutableStateOf(vm.threeDAcceleration) }

    // RedBox blank disk creator.
    var showCreateDiskDialog by remember(vm) { mutableStateOf(false) }
    var newDiskName by remember(vm) { mutableStateOf("RedBoxDisk.img") }
    var newDiskSizeGb by remember(vm) { mutableIntStateOf(32) }

    var networkEnabled by remember(vm) { mutableStateOf(vm.networkEnabled) }
    var networkAdapter by remember(vm) { mutableStateOf(vm.networkAdapter) }
    var networkMode by remember(vm) { mutableStateOf(vm.networkMode) }
    var qemuParams by remember(vm) { mutableStateOf(vm.qemuParams) }
    var biosDate by remember(vm) { mutableStateOf(vm.biosDate) }
    var soundCard by remember(vm) { mutableStateOf(vm.soundCard) }
    var bootPriority by remember(vm) { mutableStateOf(vm.bootPriority) }
    var firmwareMode by remember(vm) { mutableStateOf(vm.firmwareMode) }
    var highPriority by remember(vm) { mutableStateOf(vm.highPriority) }

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
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
                } ?: throw IllegalStateException("Could not open the new disk file")

                persistUri(uri)
                diskImage = uri.toString()
                diskImageName = finalName

                Toast.makeText(
                    context,
                    "Created $finalName (${newDiskSizeGb} GB)",
                    Toast.LENGTH_LONG
                ).show()
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    "Could not create disk: ${error.message ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
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

            Toast.makeText(
                context,
                "Added $safeName to RedBox Shared Folder",
                Toast.LENGTH_SHORT
            ).show()
        } catch (error: Exception) {
            Toast.makeText(
                context,
                "Could not add file: ${error.message ?: "unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val sharedFolderFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importFileToSharedFolder(uri)
        }
    }

    if (showCpuOptions) {
        AlertDialog(
            onDismissRequest = { showCpuOptions = false },
            title = { Text("CPU Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CPU Cores", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1", "2", "4", "6", "8").forEach { option ->
                            FilterChip(
                                selected = cpuCores == option,
                                onClick = { cpuCores = option },
                                label = { Text(option) }
                            )
                        }
                    }

                    Text("Preset", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Compatibility", "Balanced").forEach { option ->
                                FilterChip(
                                    selected = performancePreset == option,
                                    onClick = {
                                        performancePreset = option
                                        when (option) {
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
                                        }
                                    },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Performance", "Custom").forEach { option ->
                                FilterChip(
                                    selected = performancePreset == option,
                                    onClick = {
                                        performancePreset = option
                                        if (option == "Performance") {
                                            cpuModel = "Default"
                                            tcgCache = "512 MB"
                                            multiThreadedTcg = true
                                            machineType = "pc"
                                        }
                                    },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }

                    Text("CPU Model", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            listOf("Default", "qemu64", "core2duo"),
                            listOf("Nehalem", "SandyBridge", "Haswell"),
                            listOf("max")
                        ).forEach { options ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { option ->
                                    FilterChip(
                                        selected = cpuModel == option,
                                        onClick = {
                                            cpuModel = option
                                            performancePreset = "Custom"
                                        },
                                        label = { Text(option) }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = when (cpuModel) {
                            "qemu64" -> "QEMU's generic 64-bit x86 CPU model for broad guest compatibility."
                            "core2duo" -> "Emulates an older Intel Core 2 Duo-class CPU for legacy operating systems."
                            "Nehalem" -> "Emulates an Intel Nehalem-generation CPU."
                            "SandyBridge" -> "Emulates an Intel Sandy Bridge-generation CPU."
                            "Haswell" -> "Emulates an Intel Haswell-generation CPU."
                            "max" -> "Exposes a broad set of CPU features supported by QEMU's emulation."
                            else -> "Uses RedBox's default CPU model selection."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Text("CPU Flags", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional CPU features. Default CPU Model becomes qemu64 when flags are enabled.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    val selectedCpuFlags =
                        cpuFlags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    listOf(
                        "SSE3" to "sse3",
                        "SSSE3" to "ssse3",
                        "SSE4.1" to "sse4.1",
                        "SSE4.2" to "sse4.2",
                        "POPCNT" to "popcnt",
                        "AES" to "aes",
                        "AVX" to "avx"
                    ).forEach { (label, flag) ->
                        FilterChip(
                            selected = flag in selectedCpuFlags,
                            onClick = {
                                val updated = selectedCpuFlags.toMutableSet()
                                if (flag in updated) updated.remove(flag) else updated.add(flag)
                                cpuFlags = updated.joinToString(",")
                                performancePreset = "Custom"
                            },
                            label = { Text(label) }
                        )
                    }

                    Text("TCG Cache", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("128 MB", "256 MB", "512 MB").forEach { option ->
                            FilterChip(
                                selected = tcgCache == option,
                                onClick = {
                                    tcgCache = option
                                    performancePreset = "Custom"
                                },
                                label = { Text(option) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Multi-threaded TCG", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (multiThreadedTcg) "Enabled" else "Disabled",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = multiThreadedTcg,
                            onCheckedChange = {
                                multiThreadedTcg = it
                                performancePreset = "Custom"
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCpuOptions = false }) { Text("Done") }
            }
        )
    }

    if (showRamOptions) {
        AlertDialog(
            onDismissRequest = { showRamOptions = false },
            title = { Text("RAM Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose how much memory the virtual machine can use.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    listOf(
                        listOf("128 MB", "256 MB", "384 MB"),
                        listOf("512 MB", "640 MB", "768 MB"),
                        listOf("1024 MB", "2048 MB"),
                        listOf("4096 MB", "6144 MB", "8192 MB")
                    ).forEach { options ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { option ->
                                FilterChip(
                                    selected = ram == option,
                                    onClick = { ram = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRamOptions = false }) { Text("Done") }
            }
        )
    }

    if (showMachineOptions) {
        AlertDialog(
            onDismissRequest = { showMachineOptions = false },
            title = { Text("Machine Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Choose the emulated PC chipset used by this virtual machine.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    FilterChip(
                        selected = machineType == "pc",
                        onClick = {
                            machineType = "pc"
                            performancePreset = "Custom"
                        },
                        label = { Text("PC (i440FX)") }
                    )
                    FilterChip(
                        selected = machineType == "q35",
                        onClick = {
                            machineType = "q35"
                            if (diskInterface == "IDE") diskInterface = "AHCI"
                            performancePreset = "Custom"
                        },
                        label = { Text("Q35") }
                    )
                    FilterChip(
                        selected = machineType == "isapc",
                        onClick = {
                            machineType = "isapc"
                            diskInterface = "IDE"
                            performancePreset = "Custom"
                        },
                        label = { Text("ISA PC (Legacy)") }
                    )
                    Text(
                        text = when (machineType) {
                            "q35" -> "Q35 emulates a newer Intel chipset and is generally suited to newer guest operating systems."
                            "isapc" -> "ISA PC (Legacy) emulates a very old ISA-based PC for legacy guest operating systems."
                            else -> "PC (i440FX) emulates the classic QEMU PC chipset and is useful for broad and older guest compatibility."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMachineOptions = false }) { Text("Done") }
            }
        )
    }

    if (showCreateDiskDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDiskDialog = false },
            title = { Text("Create New Disk") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDiskName,
                        onValueChange = { newDiskName = it },
                        label = { Text("Disk name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Disk size",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(16, 32).forEach { size ->
                            OutlinedButton(
                                onClick = { newDiskSizeGb = size },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (newDiskSizeGb == size) "✓ $size GB" else "$size GB")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(64, 128).forEach { size ->
                            OutlinedButton(
                                onClick = { newDiskSizeGb = size },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (newDiskSizeGb == size) "✓ $size GB" else "$size GB")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sparse RAW uses little space at first and grows as the guest writes data.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeName = newDiskName
                            .trim()
                            .ifBlank { "RedBoxDisk.img" }
                            .let { if (it.endsWith(".img", ignoreCase = true)) it else "$it.img" }

                        newDiskName = safeName
                        showCreateDiskDialog = false
                        createDiskLauncher.launch(safeName)
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDiskDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    RedBoxMaterialTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Edit VM",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${vm.name} · RedBox configuration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(
                                text = "‹",
                                fontSize = 30.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = name.ifBlank { vm.name },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "$architecture · $ram · $cpuCores core${if (cpuCores == "1") "" else "s"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (machineType) {
                                "isapc" -> "ISA PC (Legacy) · $diskInterface · $displayAdapter"
                                "q35" -> "Q35 · $diskInterface · $displayAdapter"
                                else -> "PC (i440FX) · $diskInterface · $displayAdapter"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                SettingsGroupTitle(
                    title = "General",
                    subtitle = "Name and guest architecture"
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("VM Name") },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Architecture",
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("x86_64", "x86").forEach { option ->
                            FilterChip(
                                selected = architecture == option,
                                onClick = { architecture = option },
                                label = { Text(option) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ARM64", "ARM").forEach { option ->
                            FilterChip(
                                selected = architecture == option,
                                onClick = { architecture = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsGroupTitle(
                    title = "System",
                    subtitle = "CPU, memory and emulated machine hardware"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCpuOptions = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CPU", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "$cpuCores Cores · $cpuModel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "$performancePreset preset · ${if (multiThreadedTcg) "Multi-threaded TCG" else "Single-threaded TCG"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text("›", fontSize = 30.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRamOptions = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RAM", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                ram,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Guest memory allocation",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text("›", fontSize = 30.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMachineOptions = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Machine", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                when (machineType) {
                                    "isapc" -> "ISA PC (Legacy)"
                                    "q35" -> "Q35"
                                    else -> "PC (i440FX)"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                when (machineType) {
                                    "isapc" -> "Legacy ISA machine · very old guest operating systems"
                                    "q35" -> "Modern chipset · newer guest operating systems"
                                    else -> "Classic chipset · broad legacy compatibility"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text("›", fontSize = 30.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Display",
                    subtitle = "Graphics adapter and acceleration"
                )

                Text(
                    text = "Choose the virtual graphics adapter presented to the guest operating system.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = displayAdapter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (displayAdapter) {
                                "Bochs Display" ->
                                    "Modern software framebuffer. Useful to test for smoother desktop display behavior when the guest supports it."
                                "VirtIO VGA" ->
                                    "Paravirtualized graphics adapter. It can reduce emulated-device overhead, but guest driver support may be required."
                                "Cirrus VGA" ->
                                    "Legacy graphics adapter intended mainly for older guest operating systems that work better with older virtual hardware."
                                else ->
                                    "Standard VGA provides broad compatibility and is a good fallback when another display adapter causes guest display problems."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Standard VGA",
                        "Bochs Display",
                        "VirtIO VGA",
                        "Cirrus VGA"
                    ).forEach { option ->
                        FilterChip(
                            selected = displayAdapter == option,
                            onClick = { displayAdapter = option },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "3D Acceleration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (displayAdapter == "VirtIO VGA")
                                    "Enable RedBox's existing VirtIO VirGL/OpenGL ES path. Guest driver support is required."
                                else
                                    "Available only when VirtIO VGA is selected.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = threeDAcceleration && displayAdapter == "VirtIO VGA",
                            onCheckedChange = { threeDAcceleration = it },
                            enabled = displayAdapter == "VirtIO VGA"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Disk Interface",
                    subtitle = "How the primary virtual disk connects to the machine"
                )

                Text(
                    text = "Choose how the virtual hard disk is connected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = diskInterface == "AHCI",
                            onClick = {
                                diskInterface = "AHCI"
                                performancePreset = "Custom"
                            },
                            label = { Text("AHCI") }
                        )

                        FilterChip(
                            selected = diskInterface == "IDE",
                            onClick = {
                                diskInterface = "IDE"
                                if (machineType == "q35") machineType = "pc"
                                performancePreset = "Custom"
                            },
                            label = { Text("IDE") }
                        )

                        FilterChip(
                            selected = diskInterface == "VirtIO Block",
                            onClick = {
                                diskInterface = "VirtIO Block"
                                performancePreset = "Custom"
                            },
                            label = { Text("VirtIO Block") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (diskInterface) {
                        "IDE" -> "IDE is the legacy compatibility option. RedBox automatically uses PC (i440FX) machine mode with IDE. This can be useful for older guest operating systems."
                        "VirtIO Block" -> "VirtIO Block reduces emulated storage-device overhead, but the guest must have a VirtIO storage driver. Windows may fail to detect or boot from the disk until the driver is installed."
                        else -> "AHCI connects the virtual disk as a SATA-style device. It is a good general choice for guest operating systems with AHCI/SATA support."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Firmware",
                    subtitle = "Boot firmware used by the virtual machine"
                )

                Text(
                    text = "Choose Legacy BIOS or RedBox's bundled EDK2 UEFI firmware.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = firmwareMode == "Legacy BIOS",
                        onClick = { firmwareMode = "Legacy BIOS" },
                        label = { Text("Legacy BIOS") }
                    )

                    FilterChip(
                        selected = firmwareMode == "UEFI (EDK2)",
                        onClick = { firmwareMode = "UEFI (EDK2)" },
                        label = { Text("UEFI (EDK2)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (firmwareMode == "UEFI (EDK2)")
                        "Uses edk2-x86_64-code.fd with EDK2 variable storage."
                    else
                        "Uses the existing legacy BIOS path for maximum compatibility.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Performance",
                    subtitle = "Android scheduling priority for the QEMU worker"
                )

                Text(
                    text = "Requests a higher Android scheduling priority for the QEMU VM worker.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (highPriority) "Enabled" else "Disabled",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Android can still limit CPU scheduling and background execution.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = highPriority,
                        onCheckedChange = { highPriority = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Boot",
                    subtitle = "Choose which boot device RedBox tries first"
                )

                Text(
                    text = "Choose whether the virtual hard disk or Install ISO is tried first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = bootPriority == "Hard Disk First",
                        onClick = { bootPriority = "Hard Disk First" },
                        label = { Text("Hard Disk First") }
                    )

                    FilterChip(
                        selected = bootPriority == "CD/DVD ISO First",
                        onClick = { bootPriority = "CD/DVD ISO First" },
                        label = { Text("CD/DVD ISO First") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (bootPriority == "CD/DVD ISO First")
                        "The Install ISO is first when one is attached. The hard disk remains the fallback."
                    else
                        "The virtual hard disk is tried before the Install ISO.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Storage",
                    subtitle = "Virtual disks, installation media and shared files"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "Disk Image",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Primary virtual hard disk used by this VM.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (diskImageName.isEmpty()) "Not selected" else "Selected file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = diskImageName.ifEmpty { "No disk image selected" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                diskPicker.launch(
                                    arrayOf(
                                        "application/octet-stream",
                                        "application/x-qemu-disk",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose Disk Image")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showCreateDiskDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create New Disk")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Creates a blank sparse RAW .img disk for IDE, AHCI or VirtIO Block.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ISO Image",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Optional CD/DVD image for installing or booting a guest operating system.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isoImageName.isEmpty()) "Not selected" else "Selected file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = isoImageName.ifEmpty { "No ISO image selected" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                isoPicker.launch(
                                    arrayOf(
                                        "application/x-iso9660-image",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose ISO Image")
                        }

                        if (isoImage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    isoImage = ""
                                    isoImageName = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remove ISO")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CD-ROM 2 / Driver ISO",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Optional second CD/DVD image, useful for guest drivers such as VirtIO.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (driverIsoImageName.isEmpty()) "Not selected" else "Selected file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = driverIsoImageName.ifEmpty { "No driver ISO selected" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                driverIsoPicker.launch(
                                    arrayOf(
                                        "application/x-iso9660-image",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose Driver ISO")
                        }

                        if (driverIsoImage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    driverIsoImage = ""
                                    driverIsoImageName = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remove Driver ISO")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Shared Hard Drive",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Attach an additional disk image for exchanging files or extra guest storage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (sharedDiskImageName.isEmpty()) "Not selected" else "Selected file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sharedDiskImageName.ifEmpty { "No shared drive selected" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                sharedDiskPicker.launch(
                                    arrayOf(
                                        "application/octet-stream",
                                        "application/x-qemu-disk",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (sharedDiskImage.isEmpty()) "Choose Shared Drive" else "Change Shared Drive")
                        }

                        if (sharedDiskImage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    sharedDiskImage = ""
                                    sharedDiskImageName = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remove Shared Drive")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Shared Folder",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = if (sharedFolderLastFileName.isNotEmpty()) {
                                "Last added: $sharedFolderLastFileName"
                            } else {
                                "Import Android files into a Windows-readable shared drive."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (sharedFolderEnabled) "Enabled" else "Disabled")

                            Switch(
                                checked = sharedFolderEnabled,
                                onCheckedChange = {
                                    sharedFolderEnabled = it
                                    if (it) {
                                        sharedDiskImage = ""
                                        sharedDiskImageName = ""
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                sharedFolderFilePicker.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add File to Shared Folder")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Network",
                    subtitle = "Guest network adapter and connection mode"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Network",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (networkEnabled) "Enabled" else "Disabled",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = networkEnabled,
                                onCheckedChange = { networkEnabled = it }
                            )
                        }

                        if (networkEnabled) {
                            Spacer(modifier = Modifier.height(18.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Network Adapter",
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Intel E1000",
                                    "Intel E1000E",
                                    "Realtek RTL8139",
                                    "AMD PCnet"
                                ).forEach { option ->
                                    FilterChip(
                                        selected = networkAdapter == option,
                                        onClick = { networkAdapter = option },
                                        label = { Text(option) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Network Mode",
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FilterChip(
                                selected = networkMode == "User (NAT)",
                                onClick = { networkMode = "User (NAT)" },
                                label = { Text("User (NAT)") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "User (NAT) shares the Android device's connection without root.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Audio",
                    subtitle = "Emulated sound hardware"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(text = "Sound Card", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Intel HDA", "AC97", "Sound Blaster 16").forEach { option ->
                                FilterChip(
                                    selected = soundCard == option,
                                    onClick = { soundCard = option },
                                    label = { Text(option) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (soundCard) {
                                "AC97" -> "Legacy AC97 audio for older Windows and Linux guests."
                                "Sound Blaster 16" -> "Classic ISA Sound Blaster 16 for legacy operating systems."
                                else -> "Intel HD Audio using RedBox's existing SDL audio backend."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))

                SettingsGroupTitle(
                    title = "Advanced",
                    subtitle = "Guest date and custom QEMU parameters"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "BIOS / Guest Date",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Set the guest RTC date for Windows beta builds. Default uses QEMU's normal current date.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        listOf("Default", "2001-07-01", "2003-10-01", "2005-04-01").forEach { option ->
                            FilterChip(
                                selected = biosDate == option,
                                onClick = { biosDate = option },
                                label = {
                                    Text(
                                        when (option) {
                                            "Default" -> "Default"
                                            "2001-07-01" -> "2001 (Whistler era)"
                                            "2003-10-01" -> "2003 (Longhorn era)"
                                            else -> "2005 (Longhorn era)"
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = if (biosDate == "Default") "" else biosDate,
                            onValueChange = { biosDate = it.trim().ifBlank { "Default" } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Custom date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Example: 2005-04-01.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "QEMU Parameters",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Advanced option for adding extra QEMU command-line parameters. Leave this empty unless you know the parameter you need.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Use with care",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Extra parameters are passed to QEMU in addition to RedBox's generated VM configuration. Invalid, duplicate, or conflicting options can prevent the VM from starting or change normal RedBox behavior.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = qemuParams,
                            onValueChange = { qemuParams = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Extra QEMU parameters") },
                            placeholder = { Text("-rtc base=localtime") },
                            supportingText = {
                                Text(
                                    if (qemuParams.isBlank()) {
                                        "No custom parameters — RedBox will use its normal generated configuration."
                                    } else {
                                        "Custom parameters are active for this VM."
                                    }
                                )
                            },
                            minLines = 2,
                            maxLines = 5
                        )

                        if (qemuParams.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { qemuParams = "" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Custom Parameters")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
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
                    },
                    enabled = name.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun DetailRowMaterial(
    icon: String,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun StorageRowMaterial(
    icon: String,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

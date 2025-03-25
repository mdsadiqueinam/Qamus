package io.github.mdsadiqueinam.qamus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.ui.components.KalimaOverlayContent
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import io.github.mdsadiqueinam.qamus.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Service for displaying Kalima entries in an overlay window.
 */
@AndroidEntryPoint
class KalimaOverlayService : LifecycleService(), SavedStateRegistryOwner {
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var savedStateRegistryController: SavedStateRegistryController
    private lateinit var wakeLock: PowerManager.WakeLock

    @Inject
    lateinit var kalimaatRepository: KalimaatRepository

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val TAG = "KalimaOverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "KalimaOverlayChannel"
        private const val NOTIFICATION_ID = 1

        fun createIntent(context: Context): Intent {
            return Intent(context, KalimaOverlayService::class.java)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)

        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "Qamus:KalimaOverlayWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!PermissionUtils.canDrawOverlays(this)) {
            Log.w(TAG, applicationContext.getString(R.string.log_cannot_show_kalima_overlay))
            stopSelf()
            return START_NOT_STICKY
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val randomKalima = kalimaatRepository.getRandomEntry()
                if (randomKalima != null) {
                    withContext(Dispatchers.Main) {
                        showKalimaOverlay(randomKalima)
                    }
                } else {
                    Log.w(TAG, applicationContext.getString(R.string.log_no_kalima_found))
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, applicationContext.getString(R.string.log_error_getting_random_kalima), e)
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        hideOverlay()
        savedStateRegistryController.performSave(Bundle())
        super.onDestroy()
    }

    private fun showKalimaOverlay(kalima: Kalima) {
        hideOverlay()

        // Acquire wake lock
        if (!wakeLock.isHeld) {
            wakeLock.acquire(5 * 60 * 1000L) // 5 minutes max
        }

        try {
            val view = createView(kalima)

            val layoutParams = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }
            windowManager.addView(view, layoutParams)
            overlayView = view
            Log.d(TAG, applicationContext.getString(R.string.log_kalima_overlay_shown))
        } catch (e: Exception) {
            Log.e(TAG, applicationContext.getString(R.string.log_error_showing_kalima_overlay), e)
            stopSelf()
        }
    }

    /**
     * Hide the current overlay, if any.
     */
    private fun hideOverlay() {
        // Release wake lock
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, applicationContext.getString(R.string.log_error_removing_overlay), e)
            }
            overlayView = null
        }
    }

    private fun createView(kalima: Kalima): ComposeView = ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setViewTreeLifecycleOwner(this@KalimaOverlayService)
        setViewTreeSavedStateRegistryOwner(this@KalimaOverlayService)
        setContent {
            QamusTheme {
                KalimaOverlayContent(
                    kalima = kalima,
                    onClose = {
                        hideOverlay()
                        stopSelf()
                    }
                )
            }
        }
    }
}

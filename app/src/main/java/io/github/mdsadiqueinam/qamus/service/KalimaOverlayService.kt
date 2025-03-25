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
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.mdsadiqueinam.qamus.util.checkAnswer
import io.github.mdsadiqueinam.qamus.util.mapArabicToUrdu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlin.or

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!PermissionUtils.canDrawOverlays(this)) {
            Log.w(TAG, applicationContext.getString(R.string.log_cannot_show_kalima_overlay))
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.kalima_overlay_notification))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)

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
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Kalima Overlay Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
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
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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

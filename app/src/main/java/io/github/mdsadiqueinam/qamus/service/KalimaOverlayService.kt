package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import io.github.mdsadiqueinam.qamus.util.PermissionUtils
import io.github.mdsadiqueinam.qamus.util.mapArabicToUrdu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlin.io.normalize

/**
 * Service for displaying Kalima entries in an overlay window.
 */
@AndroidEntryPoint
class KalimaOverlayService : LifecycleService(), SavedStateRegistryOwner {
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    @Inject
    lateinit var kalimaatRepository: KalimaatRepository

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val TAG = "KalimaOverlayService"

        /**
         * Create an intent to start the KalimaOverlayService.
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, KalimaOverlayService::class.java)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Initialize SavedStateRegistryController
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Launch a coroutine to get a random Kalima
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get a random Kalima entry from the repository
                val randomKalima = kalimaatRepository.getRandomEntry()

                if (randomKalima != null) {
                    Log.d(TAG, applicationContext.getString(R.string.log_found_random_kalima, randomKalima.huroof))

                    // Switch to the main dispatcher to show the overlay
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

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        hideOverlay()

        // Save state before destroying
        savedStateRegistryController.performSave(Bundle())

        super.onDestroy()
    }

    /**
     * Show a Kalima entry in an overlay window.
     * This requires the SYSTEM_ALERT_WINDOW permission to be granted.
     */
    private fun showKalimaOverlay(kalima: Kalima) {
        // Check if we have the permission to draw overlays
        if (!PermissionUtils.canDrawOverlays(this)) {
            Log.w(TAG, applicationContext.getString(R.string.log_cannot_show_kalima_overlay))
            stopSelf()
            return
        }

        // Remove any existing overlay
        hideOverlay()

        try {
            // Create a new ComposeView for the overlay
            val view = ComposeView(this).apply {
                // Set the composition strategy to dispose when detached from window or released from pool
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

                // Set the lifecycle owner and saved state registry owner
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

            // Create layout parameters for the overlay window
            val layoutParams = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }

            // Add the view to the window manager
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
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, applicationContext.getString(R.string.log_error_removing_overlay), e)
            }
            overlayView = null
        }
    }

    /**
     * Composable function for the overlay content.
     */
    @Composable
    private fun KalimaOverlayContent(
        kalima: Kalima, onClose: () -> Unit
    ) {
        var answer by remember { mutableStateOf("") }
        var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }

        Surface(
            modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(8.dp), shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surface)
            ) {
                // Display the Arabic word (huroof)
                Text(
                    text = kalima.huroof,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isAnswerCorrect != null) {
                    Text(
                        text = kalima.meaning,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


                if (isAnswerCorrect !== null) {
                    Text(
                        text = if (isAnswerCorrect == true) stringResource(R.string.correct_answer) else stringResource(R.string.wrong_answer),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isAnswerCorrect == true) Color.Green else Color.Red,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text(stringResource(R.string.meaning)) },
                            placeholder = { Text(stringResource(R.string.meaning)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAnswerCorrect != null) Arrangement.Center else Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onClose,
                    ) {
                        Text(stringResource(R.string.close))
                    }

                    if (isAnswerCorrect == null) {
                        Button(onClick = {
                            isAnswerCorrect = checkAnswer(answer, kalima.meaning)
                        }) {
                            Text(stringResource(R.string.submit))
                        }
                    }
                }
            }
        }
    }
}

fun checkAnswer(answer: String, meaning: String): Boolean {
    val meanings = meaning.split(Regex("[,،]")).map {
        Normalizer.normalize(mapArabicToUrdu(it.trim()), Normalizer.Form.NFKD).lowercase(Locale.ROOT)
    }
    val normalizedAnswer = Normalizer.normalize(mapArabicToUrdu(answer.trim()), Normalizer.Form.NFKD).lowercase(Locale.ROOT)
    return meanings.contains(normalizedAnswer)
}

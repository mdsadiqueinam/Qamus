package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for displaying Kalima entries in an overlay window.
 */
@Singleton
class KalimaOverlayManager @Inject constructor(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    /**
     * Show a Kalima entry in an overlay window.
     */
    fun showKalimaOverlay(kalima: Kalima) {
        // Remove any existing overlay
        hideOverlay()

        // Create a new ComposeView for the overlay
        val view = ComposeView(context).apply {
            setContent {
                KalimaOverlayContent(
                    kalima = kalima, onClose = { hideOverlay() })
            }
        }

        // Create layout parameters for the overlay window
        val layoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }

        // Add the view to the window manager
        windowManager.addView(view, layoutParams)
        overlayView = view
    }

    /**
     * Hide the current overlay, if any.
     */
    fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
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
        Surface(
            modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(8.dp), shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surface)
            ) {
                // Display the Arabic word (huroof)
                Text(
                    text = kalima.huroof,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onClose) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
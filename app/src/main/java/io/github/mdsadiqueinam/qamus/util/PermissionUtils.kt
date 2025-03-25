package io.github.mdsadiqueinam.qamus.util

import android.content.Context
import android.provider.Settings

/**
 * Utility class for handling runtime permissions.
 */
object PermissionUtils {

    /**
     * Checks if the SYSTEM_ALERT_WINDOW permission is granted.
     * This permission is required to draw over other apps.
     *
     * @param context The context to check the permission for.
     * @return True if the permission is granted, false otherwise.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if all the required permissions for the app are granted.
     * Currently, this only checks for the SYSTEM_ALERT_WINDOW permission.
     *
     * @param context The context to check the permissions for.
     * @return True if all required permissions are granted, false otherwise.
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        // FOREGROUND_SERVICE and RECEIVE_BOOT_COMPLETED are normal permissions
        // that are automatically granted at install time, so we only need to check
        // for SYSTEM_ALERT_WINDOW
        return canDrawOverlays(context)
    }
}

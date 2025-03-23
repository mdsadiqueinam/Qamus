package io.github.mdsadiqueinam.qamus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Qamus.
 * 
 * The @HiltAndroidApp annotation triggers Hilt's code generation and
 * serves as the application-level dependency container.
 */
@HiltAndroidApp
class QamusApplication : Application()
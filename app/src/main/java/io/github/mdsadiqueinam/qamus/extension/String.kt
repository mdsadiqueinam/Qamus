package io.github.mdsadiqueinam.qamus.extension

import android.icu.text.Bidi
import androidx.compose.ui.text.style.TextDirection

val String.isLtr get(): Boolean {
    val bidi = Bidi(this, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
    return bidi.baseIsLeftToRight()
}

val String.textDirection get(): TextDirection {
    return if (isLtr) TextDirection.Ltr else TextDirection.Rtl
}
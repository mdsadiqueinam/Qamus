package io.github.mdsadiqueinam.qamus.util


fun formatTime(minutes: Float): String {
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()

    return if (hours > 0) {
        "$hours hours $mins minutes"
    } else {
        "$mins minutes"
    }
}
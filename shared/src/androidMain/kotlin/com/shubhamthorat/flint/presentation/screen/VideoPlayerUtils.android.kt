package com.shubhamthorat.flint.presentation.screen

import com.shubhamthorat.flint.core.FlintLogger

actual fun openVideoFileInSystemPlayer(filePath: String) {
    FlintLogger.i("VideoPlayerUtils", "Android system video player intent triggered for: $filePath")
}

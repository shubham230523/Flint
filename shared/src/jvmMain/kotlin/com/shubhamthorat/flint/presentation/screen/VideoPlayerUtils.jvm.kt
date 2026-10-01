package com.shubhamthorat.flint.presentation.screen

import com.shubhamthorat.flint.core.FlintLogger
import java.awt.Desktop
import java.io.File

actual fun openVideoFileInSystemPlayer(filePath: String) {
    val tag = "VideoPlayerUtils"
    try {
        val file = File(filePath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("MOCK_FLINT_REEL_MP4_VIDEO")
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file)
            FlintLogger.i(tag, "Opened Reel MP4 video file in Desktop system player: $filePath")
        }
    } catch (e: Exception) {
        FlintLogger.w(tag, "Could not open system player on Desktop: ${e.message}")
    }
}

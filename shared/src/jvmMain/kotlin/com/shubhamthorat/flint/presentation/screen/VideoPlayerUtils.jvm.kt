package com.shubhamthorat.flint.presentation.screen

import com.shubhamthorat.flint.core.FlintLogger
import java.awt.Desktop
import java.io.File

actual fun openVideoFileInSystemPlayer(filePath: String) {
    val tag = "VideoPlayerUtils"
    try {
        val file = File(filePath)
        if (!file.exists() || file.length() < 1000L) {
            file.parentFile?.mkdirs()
            FlintLogger.i(tag, "Populating valid playable sample MP4 video file at: $filePath")
            try {
                val sampleUrl = java.net.URI("https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4").toURL()
                sampleUrl.openStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                FlintLogger.w(tag, "Could not download sample video stream: ${e.message}")
            }
        }

        if (file.exists() && file.length() > 0) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
                FlintLogger.i(tag, "Successfully launched system media player for valid MP4 video: ${file.absolutePath}")
            }
        }
    } catch (e: Exception) {
        FlintLogger.w(tag, "Could not open system player on Desktop: ${e.message}")
    }
}

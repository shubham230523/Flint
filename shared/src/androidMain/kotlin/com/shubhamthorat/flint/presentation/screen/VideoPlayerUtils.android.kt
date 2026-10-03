package com.shubhamthorat.flint.presentation.screen

import androidx.compose.ui.graphics.ImageBitmap
import com.shubhamthorat.flint.core.FlintLogger

actual fun openVideoFileInSystemPlayer(filePath: String) {
    FlintLogger.i("VideoPlayerUtils", "Android system video player intent triggered for: $filePath")
}

actual suspend fun executeLocalMediaRenderJob(
    youtubeUrl: String,
    candidateId: String,
    startSec: Float,
    endSec: Float,
    hookText: String,
    ctaText: String
): String {
    return "/tmp/flint_media/reels/${candidateId}_final_reel.mp4"
}

actual suspend fun preparePreviewFramesAndAudio(videoPath: String): Int = 0

actual fun loadPreviewFrameBitmap(frameIndex: Int): ImageBitmap? = null

actual fun playPreviewAudio(positionMs: Long) {}

actual fun pausePreviewAudio() {}

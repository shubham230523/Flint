package com.shubhamthorat.flint.presentation.screen

import com.shubhamthorat.flint.core.FlintLogger

actual fun openVideoFileInSystemPlayer(filePath: String) {
    FlintLogger.i("VideoPlayerUtils", "Web system video playback triggered for: $filePath")
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

package com.shubhamthorat.flint.presentation.screen

import androidx.compose.ui.graphics.ImageBitmap

expect fun openVideoFileInSystemPlayer(filePath: String)

expect suspend fun executeLocalMediaRenderJob(
    youtubeUrl: String,
    candidateId: String,
    startSec: Float,
    endSec: Float,
    hookText: String,
    ctaText: String
): String

expect suspend fun preparePreviewFramesAndAudio(videoPath: String): Int

expect fun loadPreviewFrameBitmap(frameIndex: Int): ImageBitmap?

expect fun playPreviewAudio(positionMs: Long = 0L)

expect fun pausePreviewAudio()

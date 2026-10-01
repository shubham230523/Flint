package com.shubhamthorat.flint.presentation.screen

expect fun openVideoFileInSystemPlayer(filePath: String)

expect suspend fun executeLocalMediaRenderJob(
    youtubeUrl: String,
    candidateId: String,
    startSec: Float,
    endSec: Float,
    hookText: String,
    ctaText: String
): String

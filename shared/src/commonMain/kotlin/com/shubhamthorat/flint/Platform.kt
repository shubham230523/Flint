package com.shubhamthorat.flint

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
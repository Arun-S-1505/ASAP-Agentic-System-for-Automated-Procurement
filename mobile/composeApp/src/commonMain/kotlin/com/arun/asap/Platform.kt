package com.arun.asap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
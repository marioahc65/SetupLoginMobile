package com.supabase.setuplogin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
package com.supabase.setuplogin

/**
 * Replace these with your project's values.
 * Keeping them centralized makes it easier to wire environment-specific config later.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://wlpscdbyoztpppzdhnan.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_Hk4KlCWxH5PrL1jCIoLfMQ__PyJLJOb"

    // OAuth redirect for Google sign-in. Must match Android intent-filter and Supabase Auth settings.
    const val REDIRECT_SCHEME = "com.supabase.setuplogin"
    const val REDIRECT_HOST = "login-callback"
    const val REDIRECT_URL = "$REDIRECT_SCHEME://$REDIRECT_HOST"
}

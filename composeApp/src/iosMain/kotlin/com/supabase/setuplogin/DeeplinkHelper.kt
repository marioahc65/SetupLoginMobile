package com.supabase.setuplogin

import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

/**
 * Exposed for iOS to hand OAuth deep links back to Supabase Auth.
 */
object DeeplinkHelper {
    fun handle(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        SupabaseClientProvider.client.handleDeeplinks(nsUrl)
    }
}

package com.supabase.setuplogin

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

/**
 * Shared Supabase client configured with Auth.
 * Replace the values in [SupabaseConfig] with your project's URL and anon key before running.
 */
object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = SupabaseConfig.REDIRECT_SCHEME
                host = SupabaseConfig.REDIRECT_HOST
            }
        }
    }
}

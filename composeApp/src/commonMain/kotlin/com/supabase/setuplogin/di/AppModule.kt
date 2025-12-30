package com.supabase.setuplogin.di

import com.supabase.setuplogin.SupabaseClientProvider
import com.supabase.setuplogin.auth.AuthRepository
import com.supabase.setuplogin.auth.AuthViewModel
import com.supabase.setuplogin.auth.SupabaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val appModule = module {
    single<AuthRepository> { SupabaseAuthRepository(SupabaseClientProvider.client) }
    factory { AuthViewModel(get(), CoroutineScope(Dispatchers.Main)) }
}

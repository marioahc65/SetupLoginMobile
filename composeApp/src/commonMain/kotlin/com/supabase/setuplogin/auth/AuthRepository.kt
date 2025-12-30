package com.supabase.setuplogin.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import com.supabase.setuplogin.SupabaseConfig
import io.github.jan.supabase.auth.providers.Facebook
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


interface AuthRepository {
    val sessionStatus: Flow<SessionStatus>
    suspend fun currentEmail(): String?
    suspend fun initialize(): Boolean
    suspend fun refreshSession(): String?
    suspend fun currentAccessToken(): String?
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, fullName: String?, phone: String?): UserInfo?
    suspend fun signOut()
    suspend fun signInWithGoogle()
    suspend fun signInWithFacebook()
}

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {
    private val auth = client.auth
    override val sessionStatus: Flow<SessionStatus> = auth.sessionStatus

    override suspend fun currentEmail(): String? =
        auth.currentSessionOrNull()?.user?.email

    // Clear any stored session on app start so closing the app signs out.
    override suspend fun initialize(): Boolean {
        auth.clearSession()
        return false
    }

    override suspend fun refreshSession(): String? {
        auth.refreshCurrentSession()
        auth.startAutoRefreshForCurrentSession()
        return auth.currentAccessTokenOrNull()
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        auth.startAutoRefreshForCurrentSession()
    }

    override suspend fun signUp(email: String, password: String, fullName: String?, phone: String?): UserInfo? {
        return runCatching {
            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    fullName?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                    phone?.takeIf { it.isNotBlank() }?.let {
                        put("phone", it)
                        put("Phone", it)
                    }
                }
            }
        }.onSuccess { println("Supabase signUp success: $it") }
            .onFailure { println("Supabase signUp error: ${it.message}") }
            .getOrThrow()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun signInWithGoogle() {
        auth.signInWith(
            provider = Google,
            redirectUrl = SupabaseConfig.REDIRECT_URL
        ) {
            scopes.addAll(listOf("email", "profile"))
        }
        auth.startAutoRefreshForCurrentSession()
    }

    override suspend fun signInWithFacebook() {
        auth.signInWith(
            provider = Facebook,
            redirectUrl = SupabaseConfig.REDIRECT_URL
        )
        auth.startAutoRefreshForCurrentSession()
    }

    override suspend fun currentAccessToken(): String? = auth.currentAccessTokenOrNull()
}

package com.supabase.setuplogin.auth

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import com.supabase.setuplogin.i18n.Language
import com.supabase.setuplogin.i18n.strings
import kotlin.math.max
import kotlin.math.max

enum class AuthScreenMode { LOGIN, SIGN_UP }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phone: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val statusMessage: String? = null,
    val loading: Boolean = false,
    val loggedInEmail: String? = null,
    val accessToken: String? = null,
    val tokenExpiryEpoch: Long? = null,
    val screenMode: AuthScreenMode = AuthScreenMode.LOGIN,
    val language: Language = Language.ES,
    val inactivityRemainingSeconds: Long? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    private var lastActivityEpoch: Long = Clock.System.now().toEpochMilliseconds() / 1000
    private var monitorJob: Job? = null

    init {
        scope.launch {
            repository.initialize()
            syncTokenAndUser()
            repository.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val token = status.session.accessToken
                        val exp = decodeExpiry(token)
                        _uiState.update {
                            it.copy(
                                loggedInEmail = status.session.user?.email,
                                accessToken = token,
                                tokenExpiryEpoch = exp,
                                inactivityRemainingSeconds = 180,
                                loading = false
                            )
                        }
                    }
                    is SessionStatus.NotAuthenticated -> _uiState.update {
                        it.copy(loggedInEmail = null, accessToken = null, tokenExpiryEpoch = null, inactivityRemainingSeconds = null, loading = false)
                    }
                    else -> Unit
                }
            }
        }
        monitorJob = scope.launch {
            while (true) {
                enforceInactivityAndMaybeRefresh()
                delay(1_000)
            }
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, passwordError = null, confirmPasswordError = null) }
    }

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value) }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun goToSignUp() {
        _uiState.update { AuthUiState(screenMode = AuthScreenMode.SIGN_UP, language = it.language) }
        markActivity()
    }

    fun goToLogin() {
        _uiState.update { AuthUiState(screenMode = AuthScreenMode.LOGIN, language = it.language) }
        markActivity()
    }

    fun setLanguage(language: Language) {
        _uiState.update { it.copy(language = language) }
    }

    fun signIn() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update {
                it.copy(
                    emailError = if (email.isEmpty()) "Ingresa un correo" else null,
                    passwordError = if (password.isEmpty()) "Ingresa la contraseña" else null,
                    statusMessage = "Completa correo y contraseña."
                )
            }
            return
        }
        _uiState.update { it.copy(emailError = null, passwordError = null) }
        launchAction("Sesión iniciada") {
            repository.signIn(email, password)
            syncTokenAndUser()
            markActivity()
        }
    }

    fun signUp() {
        val validationError = validateSignUp()
        if (!validationError) {
            return
        }
        _uiState.update { it.copy(emailError = null) }
        scope.launch {
            _uiState.update { it.copy(loading = true, statusMessage = null) }
            runCatching {
                repository.signUp(
                    email = uiState.value.email,
                    password = uiState.value.password,
                    fullName = uiState.value.fullName,
                    phone = uiState.value.phone
                )
            }.onSuccess { userInfo ->
                val duplicate = userInfo?.identities?.isEmpty() == true
                if (duplicate) {
                    _uiState.update {
                        it.copy(
                            emailError = "Correo ya registrado.",
                            statusMessage = "Correo ya registrado."
                        )
                    }
                } else {
                    _uiState.update {
                        AuthUiState(
                            statusMessage = "Cuenta creada, revisa tu email si tu proyecto requiere verificación.",
                            screenMode = AuthScreenMode.LOGIN,
                            language = it.language
                        )
                    }
                }
            }.onFailure { e ->
                val message = e.message ?: "Error desconocido"
                val errText = when (e) {
                    is AuthRestException -> e.error.lowercase()
                    else -> message.lowercase()
                }
                val emailError = errText.takeIf {
                    it.contains("email_address_invalid") ||
                        it.contains("invalid")
                }?.let { "Correo inválido." }

                _uiState.update {
                    it.copy(
                        statusMessage = emailError ?: message,
                        emailError = emailError
                    )
                }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun signOut() = launchAction("Sesión cerrada") {
        repository.signOut()
        lastActivityEpoch = Clock.System.now().toEpochMilliseconds() / 1000
    }

    fun signInWithGoogle() = launchAction("Continúa en Google...") {
        repository.signInWithGoogle()
        syncTokenAndUser()
        markActivity()
    }

    fun refreshSession() = launchAction("Sesión actualizada") {
        val token = repository.refreshSession()
        val newExp = token?.let { decodeExpiry(it) }
        if (token == null) {
            _uiState.update { it.copy(statusMessage = "No se pudo refrescar la sesión") }
            return@launchAction
        }
        _uiState.update { it.copy(accessToken = token, tokenExpiryEpoch = newExp) }
        markActivity()
    }

    fun signInWithFacebook() = launchAction("Continúa en Facebook...") {
        repository.signInWithFacebook()
        syncTokenAndUser()
        markActivity()
    }

    private fun launchAction(success: String, block: suspend () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(loading = true, statusMessage = null) }
            runCatching { block() }
                .onSuccess { _uiState.update { it.copy(statusMessage = success) } }
                .onFailure { e ->
                    val msg = mapErrorMessage(e)
                    _uiState.update { it.copy(statusMessage = msg) }
                }
            _uiState.update { it.copy(loading = false) }
        }
    }

    private fun validateSignUp(): Boolean {
        val email = uiState.value.email.trim()
        val password = uiState.value.password
        val confirm = uiState.value.confirmPassword
        var emailError: String? = null
        var passwordError: String? = null
        var confirmError: String? = null

        if (email.isBlank() || !email.contains("@")) {
            emailError = "Ingresa un correo válido."
        }
        if (password.length < 8) {
            passwordError = "Debe tener al menos 8 caracteres."
        } else if (!password.any { it.isDigit() } || !password.any { it.isLetter() }) {
            passwordError = "Incluye letras y números."
        }
        if (password != confirm) {
            val msg = "Las contraseñas no coinciden."
            passwordError = passwordError ?: msg
            confirmError = msg
        }

        val valid = emailError == null && passwordError == null && confirmError == null
        if (!valid) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    statusMessage = null
                )
            }
        }
        return valid
    }

    private suspend fun syncTokenAndUser() {
        val token = repository.currentAccessToken()
        val exp = token?.let { decodeExpiry(it) }
        _uiState.update {
            it.copy(
                loggedInEmail = repository.currentEmail(),
                accessToken = token,
                tokenExpiryEpoch = exp,
                inactivityRemainingSeconds = if (token != null) 180 else null
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeExpiry(token: String): Long? {
        val payload = token.substringAfter(".", "").substringBeforeLast(".", "")
        if (payload.isEmpty()) return null
        val padded = when (payload.length % 4) {
            2 -> payload + "=="
            3 -> payload + "="
            else -> payload
        }
        return runCatching {
            val decoded = Base64.UrlSafe.decode(padded).decodeToString()
            val json = Json.decodeFromString(JsonObject.serializer(), decoded)
            json["exp"]?.jsonPrimitive?.longOrNull
        }.getOrNull()
    }

    private suspend fun enforceInactivityAndMaybeRefresh() {
        val now = Clock.System.now().toEpochMilliseconds() / 1000
        val hasSession = _uiState.value.accessToken != null || _uiState.value.loggedInEmail != null
        if (!hasSession) {
            lastActivityEpoch = now
            _uiState.update { it.copy(inactivityRemainingSeconds = null) }
            return
        }
        val inactiveFor = now - lastActivityEpoch
        val remainingInactivity = max(0, 180 - inactiveFor)
        _uiState.update { it.copy(inactivityRemainingSeconds = remainingInactivity) }
        if (inactiveFor >= 180) {
            repository.signOut()
            _uiState.update { AuthUiState(screenMode = AuthScreenMode.LOGIN, statusMessage = "Sesión cerrada por inactividad", language = it.language) }
            lastActivityEpoch = now
            return
        }
        val exp = _uiState.value.tokenExpiryEpoch ?: return
        val remaining = exp - now
        if (remaining in 1..240 && inactiveFor < 180) {
            val token = repository.refreshSession()
            val newExp = token?.let { decodeExpiry(it) }
            _uiState.update {
                it.copy(
                    accessToken = token ?: it.accessToken,
                    tokenExpiryEpoch = newExp ?: it.tokenExpiryEpoch
                )
            }
        }
    }

    fun markActivity() {
        lastActivityEpoch = Clock.System.now().toEpochMilliseconds() / 1000
        _uiState.update { it.copy(inactivityRemainingSeconds = 180) }
    }

    private fun mapErrorMessage(e: Throwable): String {
        val currentStrings = strings(_uiState.value.language)
        val raw = when (e) {
            is AuthRestException -> e.error
            else -> e.message.orEmpty()
        }
        val lower = raw.lowercase()
        return when {
            lower.contains("invalid_credentials") ->
                currentStrings.errorInvalidCredentials
            lower.contains("email_address_invalid") || lower.contains("invalid email") ->
                currentStrings.errorInvalidEmail
            else -> raw.ifBlank { currentStrings.errorUnknown }
        }
    }
}

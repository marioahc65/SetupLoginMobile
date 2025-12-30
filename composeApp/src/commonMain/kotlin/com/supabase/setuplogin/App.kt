package com.supabase.setuplogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.supabase.setuplogin.auth.AuthUiState
import com.supabase.setuplogin.auth.AuthViewModel
import com.supabase.setuplogin.auth.AuthScreenMode
import com.supabase.setuplogin.di.appModule
import com.supabase.setuplogin.home.HomeScreen
import com.supabase.setuplogin.i18n.Language
import com.supabase.setuplogin.i18n.strings
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinproject.composeapp.generated.resources.ic_google
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        MaterialTheme {
            AuthScreen()
        }
    }
}

@Composable
private fun AuthScreen() {
    val viewModel = rememberAuthViewModel()
    val state by viewModel.uiState.collectAsState()
    val isSignUp = state.screenMode == AuthScreenMode.SIGN_UP

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D0E)),
        contentAlignment = Alignment.TopCenter
    ) {
        GradientHeader()

        if (state.loggedInEmail != null) {
            HomeScreen(
                email = state.loggedInEmail!!,
                loading = state.loading,
                onSignOut = viewModel::signOut,
                onRefresh = viewModel::refreshSession,
                accessToken = state.accessToken,
                tokenExpiryEpoch = state.tokenExpiryEpoch,
                inactivityRemainingSeconds = state.inactivityRemainingSeconds,
                onUserActivity = viewModel::markActivity
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 110.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(isSignUp)

                Spacer(modifier = Modifier.height(32.dp))

                GoogleButton(
                    loading = state.loading,
                    onClick = viewModel::signInWithGoogle
                )

                Spacer(modifier = Modifier.height(12.dp))

                FacebookButton(
                    loading = state.loading,
                    onClick = viewModel::signInWithFacebook
                )

                Spacer(modifier = Modifier.height(24.dp))

                DividerText()

                Spacer(modifier = Modifier.height(24.dp))

                if (isSignUp) {
                    RegisterFields(
                        state = state,
                        onFullNameChanged = viewModel::onFullNameChanged,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPhoneChanged = viewModel::onPhoneChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged
                    )
                } else {
                    LoginFields(
                        state = state,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = if (state.loading) "Cargando..." else if (isSignUp) "Crear cuenta" else "Ingresar",
                    enabled = !state.loading,
                    onClick = if (isSignUp) viewModel::signUp else viewModel::signIn
                )

                state.statusMessage?.takeIf { it.isNotBlank() && !it.contains("session not found", ignoreCase = true) }?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = if (msg.contains("error", ignoreCase = true) || msg.contains("inválid", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) Color(0xFFFF6B6B) else Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSignUp) {
                    TextButton(onClick = viewModel::goToLogin, enabled = !state.loading) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) {
                                    append("¿Ya tienes cuenta? ")
                                }
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                    append("Iniciar sesión")
                                }
                            }
                        )
                    }
                } else {
                    TextButton(onClick = viewModel::goToSignUp, enabled = !state.loading) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) {
                                    append("¿Quieres crear una cuenta? ")
                                }
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                    append("Crear cuenta")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun rememberAuthViewModel(): AuthViewModel {
    return koinInject()
}

@Composable
private fun GradientHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.35f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8E2DE2),
                        Color(0xFF4A00E0),
                        Color(0xFF0C0D0E)
                    )
                )
            )
    )
}

@Composable
private fun Header(isSignUp: Boolean) {
    Text(
        text = if (isSignUp) "Crea tu cuenta" else "Bienvenido de nuevo",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = if (isSignUp) "Completa tus datos para registrarte." else "Inicia sesión con Google o correo y contraseña.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Sign In With Google",
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun FacebookButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Sign In With Facebook",
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun DividerText() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        Text(
            text = "O",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun LoginFields(
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit
) {
    EmailField(state.email, onEmailChanged, state.emailError)
    Spacer(modifier = Modifier.height(16.dp))
    PasswordField(
        label = "Contraseña",
        value = state.password,
        onValueChange = onPasswordChanged,
        errorText = state.passwordError
    )
}

@Composable
private fun RegisterFields(
    state: AuthUiState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit
) {
    LabeledTextField(
        label = "Nombre",
        value = state.fullName,
        onValueChange = onFullNameChanged,
        placeholder = "Tu nombre completo"
    )
    Spacer(modifier = Modifier.height(12.dp))
    EmailField(state.email, onEmailChanged, state.emailError)
    Spacer(modifier = Modifier.height(12.dp))
    LabeledTextField(
        label = "Teléfono",
        value = state.phone,
        onValueChange = onPhoneChanged,
        placeholder = "+51 900 000 000",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, autoCorrectEnabled = false)
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordField(
        label = "Contraseña",
        value = state.password,
        onValueChange = onPasswordChanged,
        errorText = state.passwordError
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordField(
        label = "Confirmar contraseña",
        value = state.confirmPassword,
        onValueChange = onConfirmPasswordChanged,
        errorText = state.confirmPasswordError
    )
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit, errorText: String?) {
    LabeledTextField(
        label = "Correo",
        value = value,
        onValueChange = onValueChange,
        placeholder = "email@email.com",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        errorText = errorText
    )
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, errorText: String? = null) {
    var visible by remember { mutableStateOf(false) }
    LabeledTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = "password",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
        errorText = errorText,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = Color.White
                )
            }
        }
    )
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    errorText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.7f)) },
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = if (errorText != null) Color.Red else Color.Transparent,
                focusedIndicatorColor = if (errorText != null) Color.Red else Color.Transparent,
                focusedContainerColor = Color(0xFF1F2124),
                unfocusedContainerColor = Color(0xFF1F2124),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            isError = errorText != null,
            trailingIcon = trailingIcon
        )
        errorText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, color = Color(0xFF0C0D0E), modifier = Modifier.padding(vertical = 4.dp))
    }
}

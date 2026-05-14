package com.example.finfit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

// ─── Màu nội bộ ──────────────────────────────────────────────────────────────
private val BrandTeal    = Color(0xFF005F73)
private val BrandTealLight = Color(0xFF0A9396)
private val BrandSurface = Color(0xFFF0F4F8)
private val BrandCard    = Color(0xFFFFFFFF)
private val BrandText    = Color(0xFF1A2340)
private val BrandSub     = Color(0xFF6B7897)
private val BrandError   = Color(0xFFD62828)
private val BrandBorder  = Color(0xFFDDE3EF)
private val BrandGradStart = Color(0xFF005F73)
private val BrandGradEnd   = Color(0xFF0A9396)

// ─── Entry Point ──────────────────────────────────────────────────────────────
@Composable
fun AuthScreen(authRepository: AuthRepository, onLoginSuccess: (FirebaseUser) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }

    AnimatedContent(
        targetState = isLoginMode,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { -it } + fadeIn() togetherWith
                slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            }
        },
        label = "auth_transition"
    ) { loginMode ->
        if (loginMode) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = onLoginSuccess,
                onSwitchToRegister = { isLoginMode = false }
            )
        } else {
            RegisterScreen(
                authRepository = authRepository,
                onRegisterSuccess = onLoginSuccess,
                onSwitchToLogin = { isLoginMode = true }
            )
        }
    }
}

// ─── Login Screen ─────────────────────────────────────────────────────────────
@Composable
private fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: (FirebaseUser) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(BrandSurface)) {
        // Top gradient banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(BrandGradStart, BrandGradEnd)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                LogoBadge()
                Spacer(modifier = Modifier.height(20.dp))
                Text("Đăng nhập", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Quản lý tài chính thông minh mỗi ngày", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrandCard)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = "Email",
                        placeholder = "example@email.com",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = "Mật khẩu",
                        placeholder = "Nhập mật khẩu",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )

                    // Quên mật khẩu
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Quên mật khẩu?",
                            color = BrandTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {}
                        )
                    }

                    // Error message
                    AnimatedVisibility(visible = errorMsg != null) {
                        errorMsg?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandError.copy(alpha = 0.08f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = BrandError, modifier = Modifier.size(16.dp))
                                Text(it, color = BrandError, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Login button
                    GradientButton(
                        text = "Đăng nhập",
                        isLoading = isLoading,
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMsg = null
                                val finalEmail = mapDevEmail(email.trim(), password)
                                val finalPass = mapDevPass(email.trim(), password)
                                if (finalEmail.isEmpty() || finalPass.isEmpty()) {
                                    errorMsg = "Vui lòng nhập đầy đủ thông tin."
                                    isLoading = false
                                    return@launch
                                }
                                val user = authRepository.signIn(finalEmail, finalPass)
                                if (user != null) onLoginSuccess(user)
                                else errorMsg = "Email hoặc mật khẩu không đúng. Vui lòng thử lại."
                                isLoading = false
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch to register
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chưa có tài khoản? ", color = BrandSub, fontSize = 14.sp)
                Text(
                    text = "Đăng ký ngay",
                    color = BrandTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSwitchToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Register Screen ──────────────────────────────────────────────────────────
@Composable
private fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: (FirebaseUser) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Validation states
    val emailValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordStrength = getPasswordStrength(password)
    val passwordMatch = confirmPassword.isEmpty() || password == confirmPassword

    Box(modifier = Modifier.fillMaxSize().background(BrandSurface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(BrandGradStart, BrandGradEnd)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSwitchToLogin) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            }
            Column(
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                LogoBadge()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Tạo tài khoản", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Bắt đầu hành trình tài chính thông minh", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
            }

            // Card form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrandCard)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Section: Thông tin cá nhân
                    SectionLabel(text = "Thông tin cá nhân")

                    AuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMsg = null },
                        label = "Họ và tên",
                        placeholder = "Nguyễn Văn A",
                        icon = Icons.Default.Person
                    )

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = "Email",
                        placeholder = "example@email.com",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        isError = !emailValid,
                        errorText = if (!emailValid) "Email không hợp lệ" else null
                    )

                    HorizontalDivider(color = BrandBorder, thickness = 1.dp)

                    // Section: Bảo mật
                    SectionLabel(text = "Bảo mật tài khoản")

                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = "Mật khẩu",
                        placeholder = "Tối thiểu 8 ký tự",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )

                    // Password strength
                    AnimatedVisibility(visible = password.isNotEmpty()) {
                        PasswordStrengthIndicator(strength = passwordStrength)
                    }

                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = "Xác nhận mật khẩu",
                        placeholder = "Nhập lại mật khẩu",
                        icon = Icons.Default.LockReset,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible },
                        isError = !passwordMatch,
                        errorText = if (!passwordMatch) "Mật khẩu không khớp" else null
                    )

                    HorizontalDivider(color = BrandBorder, thickness = 1.dp)

                    // Terms checkbox
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { acceptedTerms = !acceptedTerms }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (acceptedTerms) BrandTeal.copy(alpha = 0.06f) else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandTeal,
                                uncheckedColor = BrandBorder,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tôi đồng ý với Điều khoản dịch vụ và Chính sách bảo mật của FinFit",
                                fontSize = 13.sp,
                                color = BrandText,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "Xem điều khoản →",
                                fontSize = 12.sp,
                                color = BrandTeal,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {}
                            )
                        }
                    }

                    // Error message
                    AnimatedVisibility(visible = errorMsg != null) {
                        errorMsg?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandError.copy(alpha = 0.08f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = BrandError, modifier = Modifier.size(16.dp))
                                Text(it, color = BrandError, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    GradientButton(
                        text = "Tạo tài khoản",
                        isLoading = isLoading,
                        enabled = acceptedTerms,
                        onClick = {
                            scope.launch {
                                when {
                                    fullName.isBlank() -> { errorMsg = "Vui lòng nhập họ và tên."; return@launch }
                                    !emailValid || email.isBlank() -> { errorMsg = "Vui lòng nhập email hợp lệ."; return@launch }
                                    password.length < 8 -> { errorMsg = "Mật khẩu phải có ít nhất 8 ký tự."; return@launch }
                                    !passwordMatch -> { errorMsg = "Mật khẩu xác nhận không khớp."; return@launch }
                                    else -> {
                                        isLoading = true
                                        errorMsg = null
                                        val user = authRepository.signUp(email.trim(), password)
                                        if (user != null) onRegisterSuccess(user)
                                        else errorMsg = "Đăng ký thất bại. Email có thể đã được sử dụng."
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đã có tài khoản? ", color = BrandSub, fontSize = 14.sp)
                Text(
                    text = "Đăng nhập",
                    color = BrandTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSwitchToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Components ───────────────────────────────────────────────────────────────

@Composable
private fun LogoBadge() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("💰", fontSize = 20.sp)
        }
        Text("FinFit", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = BrandTeal,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandSub)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BrandBorder, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(icon, null, tint = if (isError) BrandError else BrandTeal, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (isPassword && onTogglePassword != null) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = BrandSub,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandTeal,
                unfocusedBorderColor = BrandBorder,
                errorBorderColor = BrandError,
                focusedTextColor = BrandText,
                unfocusedTextColor = BrandText,
                errorTextColor = BrandText,
                cursorColor = BrandTeal,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = BrandSurface,
                errorContainerColor = BrandError.copy(alpha = 0.04f)
            ),
            shape = RoundedCornerShape(14.dp)
        )
        if (isError && errorText != null) {
            Text(errorText, color = BrandError, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (label, color, filled) = when (strength) {
        PasswordStrength.WEAK   -> Triple("Yếu", Color(0xFFD62828), 1)
        PasswordStrength.FAIR   -> Triple("Trung bình", Color(0xFFF4A261), 2)
        PasswordStrength.STRONG -> Triple("Mạnh", Color(0xFF2A9D8F), 3)
        PasswordStrength.VERY_STRONG -> Triple("Rất mạnh", Color(0xFF264653), 4)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { idx ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (idx < filled) color else BrandBorder)
                )
            }
        }
        Text("Độ mạnh: $label", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.5f, label = "btnAlpha")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled)
                    Brush.linearGradient(listOf(BrandGradStart, BrandGradEnd))
                else
                    Brush.linearGradient(listOf(BrandSub, BrandSub))
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        } else {
            Text(text, color = Color.White.copy(alpha = alpha), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private enum class PasswordStrength { WEAK, FAIR, STRONG, VERY_STRONG }

private fun getPasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        0, 1 -> PasswordStrength.WEAK
        2    -> PasswordStrength.FAIR
        3    -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

private fun mapDevEmail(email: String, password: String): String = when {
    email == "a" && password == "a" -> "a@a.com"
    email == "b" && password == "b" -> "b@b.com"
    else -> email
}

private fun mapDevPass(email: String, password: String): String = when {
    (email == "a" && password == "a") || (email == "b" && password == "b") -> "123456"
    else -> password
}

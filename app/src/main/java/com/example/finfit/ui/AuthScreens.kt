package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.data.repository.AuthReposeitory
import com.example.finfit.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(authRepository: AuthReposeitory, onLoginSuccess: (FirebaseUser) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
            modifier = Modifier.fillMaxSize().background(DarkBackground).padding(24.dp),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
        ) {
            // Logo / Icon
            Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CardBackground
            ) {
                Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                    text = if (isLoginMode) "Chào mừng trở lại!" else "Tạo tài khoản mới",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    text =
                            if (isLoginMode) "Đăng nhập để tiếp tục quản lý tài chính"
                            else "Bắt đầu hành trình tiết kiệm của bạn",
                    color = TextGray,
                    fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email Field
            OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = CardBackground,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = PrimaryBlue,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                            ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                    imageVector =
                                            if (isPasswordVisible) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextGray
                            )
                        }
                    },
                    visualTransformation =
                            if (isPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = CardBackground,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = PrimaryBlue,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                            ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Button
            Button(
                    onClick = {
                        scope.launch {
                            val user =
                                    if (isLoginMode) {
                                        authRepository.signIn(email, password)
                                    } else {
                                        authRepository.signUp(email, password)
                                    }

                            if (user != null) {
                                onLoginSuccess(user)
                            } else {
                                android.widget.Toast.makeText(
                                                context,
                                                if (isLoginMode) "Đăng nhập thất bại"
                                                else "Đăng ký thất bại",
                                                android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    },
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                            Brush.linearGradient(
                                                    listOf(PrimaryBlue, Color(0xFF1D4ED8))
                                            )
                                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(
                        text = if (isLoginMode) "Đăng nhập" else "Đăng ký",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch Mode
            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                        text =
                                if (isLoginMode) "Chưa có tài khoản? Đăng ký ngay"
                                else "Đã có tài khoản? Đăng nhập",
                        color = PrimaryBlue,
                        fontSize = 14.sp
                )
            }
        }
    }
}

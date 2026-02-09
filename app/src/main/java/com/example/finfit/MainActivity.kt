package com.example.finfit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.finfit.data.model.UserWallet
import com.example.finfit.data.repository.AuthReposeitory
import com.example.finfit.data.repository.FirestoreRepository
import com.example.finfit.ui.AuthScreen
import com.example.finfit.ui.DashboardScreen
import com.example.finfit.ui.theme.FinFitTheme
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authReposeitory = AuthReposeitory()
    private val fireStoreReponeitory = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinFitTheme {
                var currentUser by remember { mutableStateOf(authReposeitory.getCurrentUser()) }

                if (currentUser == null) {
                    AuthScreen(
                            authRepository = authReposeitory,
                            onLoginSuccess = { user -> currentUser = user }
                    )
                } else {
                    // Màn hình Dashboard mới
                    var wallet by remember {
                        mutableStateOf<com.example.finfit.data.model.UserWallet?>(null)
                    }

                    LaunchedEffect(currentUser!!.uid) {
                        val data = fireStoreReponeitory.getUserWallet(currentUser!!.uid)
                        if (data == null) {
                            val newWallet =
                                    com.example.finfit.data.model.UserWallet(
                                            uid = currentUser!!.uid
                                    )
                            fireStoreReponeitory.saveUserWallet(newWallet)
                            wallet = newWallet
                        } else {
                            wallet = data
                        }
                    }

                    DashboardScreen(
                            userEmail = currentUser!!.email ?: "User",
                            wallet = wallet,
                            onLogout = {
                                authReposeitory.signOut()
                                currentUser = null
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(authReposeitory: AuthReposeitory, onLoginSuccess: (FirebaseUser) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đăng Nhập", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật Khẩu") },
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val scope = rememberCoroutineScope()

        Button(
                onClick = {
                    scope.launch {
                        val user = authReposeitory.signIn(email, password)
                        if (user != null) {
                            android.widget.Toast.makeText(
                                            context,
                                            "Đăng nhập thành công",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            onLoginSuccess(user)
                        } else {
                            android.widget.Toast.makeText(
                                            context,
                                            "Đăng nhập thất bại",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
        ) { Text(text = "Đăng nhập") }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
                onClick = {
                    scope.launch {
                        val user = authReposeitory.signUp(email, password)
                        if (user != null) {
                            android.widget.Toast.makeText(
                                            context,
                                            "Đăng ký thành công",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            onLoginSuccess(user)
                        } else {
                            android.widget.Toast.makeText(
                                            context,
                                            "Đăng ký thất bại",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
        ) { Text(text = "Đăng ký") }
    }
}

@Composable
fun MainScreen(user: FirebaseUser, fireStoreRepository: FirestoreRepository, onLogout: () -> Unit) {
    var wallet by remember { mutableStateOf<UserWallet?>(null) }
    LaunchedEffect(user.uid) {
        val data = fireStoreRepository.getUserWallet(user.uid)
        if (data == null) {
            val newWallet = UserWallet(uid = user.uid)
            fireStoreRepository.saveUserWallet(newWallet)
            wallet = newWallet
        } else {
            wallet = data
        }
    }
    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Tài khoản: ${user.email}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))
        if (wallet != null) {
            Text(text = "Số tiền được dùng: ", style = MaterialTheme.typography.bodyLarge)
            Text(
                    text = " ${wallet!!.disposableAmount} đ",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = " ${wallet!!.savingsAmount} đ",
                    style = MaterialTheme.typography.headlineLarge
            )
        } else {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogout) { Text(text = "Đăng xuất") }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinFitTheme { Greeting("Android") }
}

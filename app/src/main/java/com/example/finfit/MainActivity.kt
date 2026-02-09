package com.example.finfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.finfit.data.model.PaymentMethod
import com.example.finfit.data.model.Transaction
import com.example.finfit.data.model.TransactionType
import com.example.finfit.data.model.UserWallet
import com.example.finfit.data.repository.FirestoreRepository
import com.example.finfit.ui.theme.FinFitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ví dụ: Gọi Repository để thêm dữ liệu mẫu
        val testUserId = "my_user_001"

        lifecycleScope.launch {
            // 1. Tạo ví ban đầu nếu chưa có
            val currentWallet = repository.getUserWallet(testUserId)
            if (currentWallet == null) {
                repository.saveUserWallet(
                        UserWallet(
                                uid = testUserId,
                                disposableAmount = 500000.0,
                                savingsAmount = 0.0
                        )
                )
            }

            // 2. Thêm một giao dịch chi tiêu mẫu (ví dụ ăn sáng)
            val expense =
                    Transaction(
                            amount = 35000.0,
                            type = TransactionType.EXPENSE,
                            category = "Ăn uống",
                            note = "Ăn sáng phở",
                            paymentMethod = PaymentMethod.CASH
                    )
            repository.addTransaction(testUserId, expense)

            // Sau lệnh này, số dư disposableAmount trên Firestore sẽ tự giảm đi 35.000
        }

        enableEdgeToEdge()
        setContent {
            FinFitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(name = "FinFit Team", modifier = Modifier.padding(innerPadding))
                }
            }
        }
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

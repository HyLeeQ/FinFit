package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CurrencyItem(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

val ALL_CURRENCIES = listOf(
    CurrencyItem("VND", "Vietnamese Dong",       "₫",   "🇻🇳"),
    CurrencyItem("USD", "United States Dollar",  "$",   "🇺🇸"),
    CurrencyItem("EUR", "Euro",                  "€",   "🇪🇺"),
    CurrencyItem("JPY", "Japanese Yen",          "¥",   "🇯🇵"),
    CurrencyItem("GBP", "British Pound",         "£",   "🇬🇧"),
    CurrencyItem("AUD", "Australian Dollar",     "$",   "🇦🇺"),
    CurrencyItem("CAD", "Canadian Dollar",       "$",   "🇨🇦"),
    CurrencyItem("CHF", "Switzerland Franc",     "CHF", "🇨🇭"),
    CurrencyItem("CNY", "China Yuan Renminbi",   "¥",   "🇨🇳"),
    CurrencyItem("HKD", "Hong Kong Dollar",      "$",   "🇭🇰"),
    CurrencyItem("NZD", "New Zealand Dollar",    "$",   "🇳🇿"),
    CurrencyItem("SEK", "Sweden Krona",          "kr",  "🇸🇪"),
    CurrencyItem("KRW", "South Korea Won",       "₩",   "🇰🇷"),
    CurrencyItem("SGD", "Singapore Dollar",      "$",   "🇸🇬"),
    CurrencyItem("NOK", "Norway Krone",          "kr",  "🇳🇴"),
    CurrencyItem("THB", "Thailand Baht",         "฿",   "🇹🇭"),
    CurrencyItem("INR", "Indian Rupee",          "₹",   "🇮🇳"),
    CurrencyItem("MYR", "Malaysian Ringgit",     "RM",  "🇲🇾"),
    CurrencyItem("PHP", "Philippine Peso",       "₱",   "🇵🇭"),
    CurrencyItem("IDR", "Indonesian Rupiah",     "Rp",  "🇮🇩"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupCurrencyScreen(
    initialCurrency: String = "VND",
    onBack: () -> Unit,
    onCurrencySelected: (CurrencyItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialCurrency) }

    val accent = Color(0xFF26B5A0)

    val filtered = remember(query) {
        if (query.isBlank()) ALL_CURRENCIES
        else ALL_CURRENCIES.filter {
            it.code.contains(query, ignoreCase = true) ||
            it.name.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color(0xFF1A2340))
            }
        }

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search", color = Color(0xFF8E9AB0)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF8E9AB0)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE8ECF4),
                focusedBorderColor = accent,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Currency list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filtered, key = { it.code }) { currency ->
                val isSelected = currency.code == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = currency.code
                            onCurrencySelected(currency)
                        }
                        .background(if (isSelected) accent.copy(alpha = 0.07f) else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Flag
                    Text(currency.flag, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    // Name column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currency.code,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A2340)
                        )
                        Text(
                            currency.name,
                            fontSize = 12.sp,
                            color = Color(0xFF8E9AB0)
                        )
                    }
                    // Symbol or check
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Text(
                            currency.symbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9AB0)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 0.5.dp)
            }
        }
    }
}

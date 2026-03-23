package com.example.finfit.finance.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TxCategory(val label: String, val icon: ImageVector, val color: Color)

val EXPENSE_CATEGORIES = listOf(
    TxCategory("Ăn uống",    Icons.Default.Restaurant,    Color(0xFFEF4444)),
    TxCategory("Di chuyển",  Icons.Default.DirectionsBus, Color(0xFF8B5CF6)),
    TxCategory("Mua sắm",   Icons.Default.ShoppingBag,   Color(0xFFF59E0B)),
    TxCategory("Giải trí",  Icons.Default.SportsEsports, Color(0xFF0EA5E9)),
    TxCategory("Y tế",       Icons.Default.LocalHospital, Color(0xFF10B981)),
    TxCategory("Giáo dục",  Icons.Default.School,         Color(0xFF6366F1)),
    TxCategory("Hóa đơn",   Icons.Default.Receipt,        Color(0xFFEC4899)),
    TxCategory("Nhà ở",     Icons.Default.Home,           Color(0xFF14B8A6)),
    TxCategory("Du lịch",   Icons.Default.Flight,         Color(0xFFF97316)),
    TxCategory("Khác",       Icons.Default.MoreHoriz,      Color(0xFF6B7280)),
)

val INCOME_CATEGORIES = listOf(
    TxCategory("Lương",     Icons.Default.AccountBalance,  Color(0xFF10B981)),
    TxCategory("Thưởng",    Icons.Default.CardGiftcard,    Color(0xFFF59E0B)),
    TxCategory("Đầu tư",   Icons.Default.TrendingUp,       Color(0xFF6366F1)),
    TxCategory("Bán hàng", Icons.Default.Storefront,       Color(0xFF0EA5E9)),
    TxCategory("Cho vay",  Icons.Default.PeopleAlt,        Color(0xFFEC4899)),
    TxCategory("Khác",      Icons.Default.MoreHoriz,        Color(0xFF6B7280)),
)

val TRANSFER_CATEGORIES = listOf(
    TxCategory("Chuyển khoản", Icons.Default.SwapHoriz, Color(0xFF6366F1)),
    TxCategory("Rút tiền",     Icons.Default.Atm,       Color(0xFF3B82F6)),
    TxCategory("Nạp tiền",     Icons.Default.AddCard,   Color(0xFF10B981))
)

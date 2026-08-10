package com.example.finfit.finance.model

import com.google.firebase.Timestamp

/**
 * Nhóm chia tiền cố định (bạn trọ, đồng nghiệp ăn trưa, nhóm đi chơi...)
 */
data class SavedSplitGroup(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)

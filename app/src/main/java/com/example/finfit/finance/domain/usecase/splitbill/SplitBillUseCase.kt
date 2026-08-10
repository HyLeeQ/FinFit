package com.example.finfit.finance.domain.usecase.splitbill

import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionParticipant

class SplitBillUseCase {
    operator fun invoke(
        totalAmount: Double,
        participants: List<String>,
        payerName: String
    ): List<TransactionParticipant> {
        if (participants.isEmpty()) return emptyList()
        val share = totalAmount / participants.size
        return participants.map { name ->
            TransactionParticipant(
                name = name,
                shareAmount = share,
                paidAmount = if (name == payerName) totalAmount else 0.0,
                isPaid = name == payerName
            )
        }
    }
}

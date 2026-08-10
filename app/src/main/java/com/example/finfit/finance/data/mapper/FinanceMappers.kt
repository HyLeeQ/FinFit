package com.example.finfit.finance.data.mapper

import com.example.finfit.finance.model.*
import com.google.firebase.Timestamp

object FinanceMappers {
    fun transactionToMap(tx: FinanceTransaction): Map<String, Any?> {
        return mapOf(
            "amount"            to tx.amount,
            "type"              to tx.type.name,
            "category"          to tx.category,
            "note"              to tx.note,
            "paymentMethod"     to tx.paymentMethod.name,
            "timestamp"         to tx.timestamp,
            "isFromOCR"         to tx.isFromOCR,
            "imageUrl"          to tx.imageUrl,
            "linkedGoalId"      to tx.linkedGoalId,
            "accountId"         to tx.accountId,
            "toAccountId"       to tx.toAccountId,
            "isGroupPrepayment" to tx.isGroupPrepayment,
            "personalAmount"    to tx.personalAmount,
            "groupAmount"       to tx.groupAmount,
            "participantCount"  to tx.participantCount,
            "participants"      to tx.participants.map { 
                mapOf(
                    "name" to it.name,
                    "shareAmount" to it.shareAmount,
                    "paidAmount" to it.paidAmount,
                    "isPaid" to it.isPaid
                )
            }
        )
    }

    fun budgetToMap(budget: FinanceBudget): Map<String, Any?> {
        return mapOf(
            "amount"            to budget.amount,
            "period"            to budget.period.name,
            "category"          to budget.category,
            "startDate"         to budget.startDate,
            "isRollover"        to budget.isRollover,
            "rolloverAmount"    to budget.rolloverAmount,
            "isEnvelope"        to budget.isEnvelope,
            "envelopeAllocated" to budget.envelopeAllocated
        )
    }
}

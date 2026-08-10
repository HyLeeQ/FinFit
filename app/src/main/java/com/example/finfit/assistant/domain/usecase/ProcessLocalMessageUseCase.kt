package com.example.finfit.assistant.domain.usecase

import com.example.finfit.finance.util.SmartTransactionParser
import com.example.finfit.finance.util.ParsedTransaction

class ProcessLocalMessageUseCase {
    operator fun invoke(message: String): ParsedTransaction? {
        return SmartTransactionParser.parse(message)
    }
}

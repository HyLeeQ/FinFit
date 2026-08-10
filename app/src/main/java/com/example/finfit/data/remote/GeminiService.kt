package com.example.finfit.data.remote

import com.example.finfit.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Extension functions to make tool parameter definitions more concise
private fun Schema.Companion.str(name: String, description: String, nullable: Boolean = false) =
    Schema(name = name, description = description, type = FunctionType.STRING, nullable = nullable)

private fun Schema.Companion.double(name: String, description: String, nullable: Boolean = false) =
    Schema(name = name, description = description, type = FunctionType.NUMBER, nullable = nullable)

private fun Schema.Companion.int(name: String, description: String, nullable: Boolean = false) =
    Schema(name = name, description = description, type = FunctionType.INTEGER, nullable = nullable)

private fun Schema.Companion.array(name: String, description: String, items: Schema<out Any>? = null) =
    Schema(
        name = name,
        description = description,
        type = FunctionType.ARRAY,
        items = items ?: Schema<String>(name = "item", description = "Item", type = FunctionType.STRING)
    )

class QuotaExceededException(message: String = "API quota exceeded") : Exception(message)

class GeminiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val addTransactionTool =
            defineFunction(
                    name = "addTransaction",
                    description = "Thêm một giao dịch chi tiêu hoặc thu nhập mới.",
                    parameters =
                            listOf(
                                    Schema.double(
                                            "amount",
                                            "Số tiền (VNĐ). Lưu ý: Không nhầm lẫn số thứ tự ngày trong tuần với số tiền (ví dụ: 'thứ 4' không phải là 4 triệu)."
                                    ),
                                    Schema.str("category", "Hạng mục (ví dụ: Ăn uống, Lương)"),
                                    Schema.str("note", "Ghi chú thêm"),
                                    Schema.str("type", "'INCOME' (thu), 'EXPENSE' (chi)"),
                                    Schema.str(
                                            "walletSource",
                                            "Tên ví nguồn nếu có (ví dụ: Momo, Vietcombank, Tiền mặt)"
                                    )
                            ),
                    requiredParameters = listOf("amount", "category", "type")
            )

    private val addDebtLoanTool =
            defineFunction(
                    name = "addDebtLoan",
                    description =
                            "Tạo khoản ghi nợ (mượn tiền ai đó) hoặc cho vay (đưa ai đó mượn tiền).",
                    parameters =
                            listOf(
                                    Schema.str(
                                            "personName",
                                            "Tên người liên hệ (ví dụ: 'Nam', 'Tùng')"
                                    ),
                                    Schema.double(
                                            "amount",
                                            "Số tiền (VNĐ). Cẩn thận với các số thứ tự ngày trong tuần."
                                    ),
                                    Schema.str(
                                            "type",
                                            "'DEBT' (mình nợ tiền họ), 'LOAN' (mình cho họ vay)"
                                    ),
                                    Schema.str("note", "Ghi chú")
                            ),
                    requiredParameters = listOf("personName", "amount", "type")
            )

    private val addSavingsGoalTool =
            defineFunction(
                    name = "addSavingsGoal",
                    description = "Tạo một mục tiêu tiết kiệm quỹ mới.",
                    parameters =
                            listOf(
                                    Schema.str("name", "Tên mục tiêu (ví dụ: 'Mua xe', 'Du lịch')"),
                                    Schema.double(
                                            "targetAmount",
                                            "Số tiền đích đến để đạt mục tiêu (VNĐ)"
                                    ),
                                    Schema.double(
                                            "autoSavingAmount",
                                            "Số tiền tự động trích ra mỗi tuần để đạt được mục tiêu này (nếu có yêu cầu, VNĐ)"
                                    ),
                                    Schema.str(
                                            "targetDate",
                                            "Hạn chót mục tiêu, định dạng YYYY-MM-DD (tuỳ chọn)"
                                    )
                            ),
                    requiredParameters = listOf("name", "targetAmount")
            )

    private val addBudgetTool =
            defineFunction(
                    name = "addBudget",
                    description = "Thiết lập hạn mức ngân sách chi tiêu hàng tháng hoặc tuần.",
                    parameters =
                            listOf(
                                    Schema.double("amount", "Số tiền hạn mức (VNĐ)"),
                                    Schema.str("category", "Hạng mục áp dụng (mặc định 'Tất cả')"),
                                    Schema.str("period", "'WEEKLY' hoặc 'MONTHLY'")
                            ),
                    requiredParameters = listOf("amount", "category", "period")
            )

    private val addGroupSplitBillTool =
            defineFunction(
                    name = "addGroupSplitBill",
                    description =
                            "Ghi nhận giao dịch chia tiền nhóm (Split Bill), khi người dùng trả tiền trước cho cả nhóm.",
                    parameters =
                            listOf(
                                    Schema.double(
                                            "totalAmount",
                                            "Tổng số tiền của hóa đơn nhóm (VNĐ)"
                                    ),
                                    Schema.int("participantCount", "Sĩ số nhóm (Gồm cả mình)"),
                                    Schema.str(
                                            "category",
                                            "Hạng mục chi tiêu (ví dụ: Ăn uống, Giải trí)"
                                    ),
                                    Schema.str("note", "Ghi chú thêm về cuộc chơi"),
                                    Schema.array("participants", "Danh sách tên những người tham gia (nếu biết, ví dụ: ['Nam', 'Tùng'])")
                            ),
                    requiredParameters = listOf("totalAmount", "participantCount", "category")
            )

    private val addAutoScheduleTool =
            defineFunction(
                    name = "addAutoSchedule",
                    description =
                            "Thiết lập một lịch trình chi tiêu tự động lặp lại hàng tuần (ví dụ: tiền trà sữa mỗi thứ 7).",
                    parameters =
                            listOf(
                                    Schema.double("amount", "Số tiền tự trừ mỗi tuần (VNĐ)"),
                                    Schema.int(
                                            "dayOfWeek",
                                            "Ngày trong tuần (1: Thứ 2, 2: Thứ 3, ..., 7: Chủ Nhật)"
                                    ),
                                    Schema.str("category", "Hạng mục (ví dụ: Ăn uống)"),
                                    Schema.str("note", "Ghi chú")
                            ),
                    requiredParameters = listOf("amount", "dayOfWeek", "category")
            )

    private val addHeldFundTool =
            defineFunction(
                    name = "addHeldFund",
                    description =
                            "Tạo một quỹ giữ hộ / quỹ thu hộ tập thể (Ví dụ: Thủ quỹ giữ tiền lớp).",
                    parameters =
                            listOf(
                                    Schema.str("fundName", "Tên quỹ giữ hộ"),
                                    Schema.double("amount", "Số tiền thu được")
                            ),
                    requiredParameters = listOf("fundName", "amount")
            )

    private val updateUserHabitTool =
            defineFunction(
                    name = "updateUserHabit",
                    description =
                            "Cập nhật thông tin thói quen, lịch trình và chi phí sinh hoạt của người dùng để AI ghi nhớ.",
                    parameters =
                            listOf(
                                    Schema.double(
                                            "minMealCost",
                                            "Giá tiền ăn tối thiểu mỗi bữa (VNĐ)"
                                    ),
                                    Schema.double(
                                            "maxMealCost",
                                            "Giá tiền ăn tối đa mỗi bữa (VNĐ)"
                                    ),
                                    Schema.str(
                                            "routineNotes",
                                            "Thông tin về lịch trình: địa điểm ở theo ngày, các quy tắc chi tiêu (ví dụ: T2-T4 ở trọ, T5 về quê, ở nhà ko tốn tiền ăn)"
                                    ),
                                    Schema.str("fixedCosts", "Các khoản chi cố định định kỳ nếu có")
                            )
            )

    private val proposeWeeklyPlanTool =
            defineFunction(
                    name = "proposeWeeklyPlan",
                    description =
                            "Đề xuất một kế hoạch chi tiêu toàn diện cho cả tuần dựa trên thói quen và các hoạt động đột xuất người dùng báo cáo.",
                    parameters =
                            listOf(
                                    Schema.str(
                                            "planDescription",
                                            "Mô tả tổng quát về kế hoạch này (ví dụ: Tuần học quân sự, Tuần thi cử tiết kiệm)"
                                    ),
                                    Schema.str(
                                            "itemsJson",
                                            "Danh sách các đầu mục chi tiêu dưới dạng JSON Array (mỗi item gồm dayOfWeek: 1-7, amount, category, note)"
                                    )
                            ),
                    requiredParameters = listOf("itemsJson")
            )

    private val depositSavingsTool =
            defineFunction(
                    name = "depositSavings",
                    description = "Nạp tiền vào một quỹ/mục tiêu tiết kiệm từ ví/tài khoản khả dụng.",
                    parameters =
                            listOf(
                                    Schema.str("goalName", "Tên mục tiêu tiết kiệm muốn nạp tiền vào (ví dụ: 'Mua xe', 'Du lịch')"),
                                    Schema.double("amount", "Số tiền nạp vào (VNĐ)"),
                                    Schema.str("walletSource", "Tên ví nguồn để lấy tiền nạp (ví dụ: Momo, Vietcombank, Tiền mặt) - Tuỳ chọn", nullable = true)
                            ),
                    requiredParameters = listOf("goalName", "amount")
            )

    private val withdrawSavingsTool =
            defineFunction(
                    name = "withdrawSavings",
                    description = "Rút tiền từ một quỹ tiết kiệm về ví/tài khoản HOẶC luân chuyển qua quỹ tiết kiệm khác.",
                    parameters =
                            listOf(
                                    Schema.str("goalName", "Tên mục tiêu tiết kiệm bị rút tiền (ví dụ: 'Mua xe')"),
                                    Schema.double("amount", "Số tiền cần rút (VNĐ)"),
                                    Schema.str("destinationWallet", "Tên ví/tài khoản đích để nhận tiền rút về (ví dụ: Momo, Vietcombank) - Tuỳ chọn", nullable = true),
                                    Schema.str("transferToSavingsGoal", "Tên quỹ tiết kiệm đích khác nếu muốn chuyển tiền từ quỹ này sang quỹ kia (ví dụ: 'Du lịch') - Tuỳ chọn", nullable = true)
                            ),
                    requiredParameters = listOf("goalName", "amount")
            )

    private val querySpendingAnalyticsTool =
            defineFunction(
                    name = "querySpendingAnalytics",
                    description = "Tra cứu và phân tích số liệu chi tiêu thực tế của người dùng theo kỳ (tháng này, tháng trước, tuần này, 7 ngày qua) và danh mục.",
                    parameters =
                            listOf(
                                    Schema.str("period", "Kỳ cần tra cứu: 'THIS_MONTH' (tháng này), 'LAST_MONTH' (tháng trước), 'THIS_WEEK' (tuần này), 'LAST_7_DAYS' (7 ngày qua)"),
                                    Schema.str("category", "Tên danh mục cụ thể cần lọc (VD: 'Ăn uống', 'Mua sắm', 'Di chuyển', 'Tất cả')", nullable = true)
                            ),
                    requiredParameters = listOf("period")
            )

    private val getFinancialHealthDetailsTool =
            defineFunction(
                    name = "getFinancialHealthDetails",
                    description = "Lấy bảng phân tích chi tiết Điểm Sức khỏe Tài chính (0-100) gồm 4 trụ cột: Tỷ lệ tiết kiệm, Kiểm soát nợ, Tuân thủ ngân sách, Quỹ dự phòng khẩn cấp.",
                    parameters = emptyList()
            )

    private val explainBudgetStatusTool =
            defineFunction(
                    name = "explainBudgetStatus",
                    description = "Kiểm tra và giải thích chi tiết trạng thái ngân sách tháng/tuần: danh mục nào an toàn, danh mục nào sắp chạm trần hoặc đã bội chi.",
                    parameters =
                            listOf(
                                    Schema.str("category", "Danh mục cần kiểm tra ngân sách (hoặc 'ALL' cho tất cả danh mục)", nullable = true)
                            )
            )

    private val compareSpendingPeriodsTool =
            defineFunction(
                    name = "compareSpendingPeriods",
                    description = "So sánh chi tiêu thực tế giữa 2 kỳ (VD: tháng này vs tháng trước, tuần này vs tuần trước) để tìm mức tăng giảm % và các danh mục biến động bất thường.",
                    parameters =
                            listOf(
                                    Schema.str("period1", "'THIS_MONTH' hoặc 'THIS_WEEK'"),
                                    Schema.str("period2", "'LAST_MONTH' hoặc 'LAST_WEEK'")
                            ),
                    requiredParameters = listOf("period1", "period2")
            )

    suspend fun getCompletion(
            sysInstruct: String,
            history: List<Content>
    ): GenerateContentResponse =
            withContext(Dispatchers.IO) {
                val generativeModel =
                        GenerativeModel(
                                modelName = "gemini-2.5-flash",
                                apiKey = apiKey,
                                generationConfig = generationConfig { temperature = 0.35f },
                                systemInstruction = content("system") { text(sysInstruct) },
                                tools =
                                        listOf(
                                                Tool(
                                                        listOf(
                                                                addTransactionTool,
                                                                addDebtLoanTool,
                                                                addSavingsGoalTool,
                                                                addBudgetTool,
                                                                addGroupSplitBillTool,
                                                                addAutoScheduleTool,
                                                                addHeldFundTool,
                                                                updateUserHabitTool,
                                                                proposeWeeklyPlanTool,
                                                                depositSavingsTool,
                                                                withdrawSavingsTool,
                                                                querySpendingAnalyticsTool,
                                                                getFinancialHealthDetailsTool,
                                                                explainBudgetStatusTool,
                                                                compareSpendingPeriodsTool
                                                        )
                                                )
                                        ),
                                safetySettings =
                                        listOf(
                                                SafetySetting(
                                                        HarmCategory.HARASSMENT,
                                                        BlockThreshold.ONLY_HIGH
                                                ),
                                                SafetySetting(
                                                        HarmCategory.HATE_SPEECH,
                                                        BlockThreshold.ONLY_HIGH
                                                ),
                                                SafetySetting(
                                                        HarmCategory.SEXUALLY_EXPLICIT,
                                                        BlockThreshold.ONLY_HIGH
                                                ),
                                                SafetySetting(
                                                        HarmCategory.DANGEROUS_CONTENT,
                                                        BlockThreshold.ONLY_HIGH
                                                )
                                        )
                        )
                try {
                    generativeModel.generateContent(*history.toTypedArray())
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    throw Exception("Chi tiết lỗi từ Google API: $msg")
                }
            }
}

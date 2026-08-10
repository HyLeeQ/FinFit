# CHI TIẾT CÁC CHỨC NĂNG MODULE TÀI CHÍNH & LUỒNG KỸ THUẬT - FINFIT

Tài liệu này phân tích 10 tính năng cốt lõi thuộc phân hệ Quản lý Tài chính (Finance Module). Mỗi tính năng sẽ đi kèm với mô tả, luồng xử lý, cơ chế hoạt động và vị trí lưu trữ source code tương ứng để phục vụ cho việc đọc hiểu và bảo vệ đồ án.

---

## 1. Quản Lý Đa Tài Khoản & Ví (Wallet Management)
- **Mô tả:** Hỗ trợ tạo và quản lý cùng lúc nhiều nguồn tiền (Tiền mặt, MoMo, Vietcombank...). Hỗ trợ "Chuyển tiền nội bộ" giữa các ví không làm thay đổi tổng tài sản.
- **Cơ chế hoạt động:** Dữ liệu tài khoản được gom chung vào object `AppUserWallet`. Khi tạo giao dịch "Chuyển tiền nội bộ", hệ thống sẽ tạo ra 1 giao dịch đặc biệt loại `TRANSFER`. Số dư của ví nguồn sẽ trừ đi và ví đích sẽ cộng vào qua một transaction SQL duy nhất.
- **Luồng xử lý (Data Flow):** `WalletManagementScreen` -> `FinanceViewModel.addAccount()` -> `FirestoreRepository.updateUserWallet()` -> Đồng bộ về Room DB -> `StateFlow` cập nhật UI.
- **Nơi chứa Code:** 
  - Giao diện: `finance/ui/screens/WalletManagementScreen.kt` và `InternalTransferScreen.kt`
  - Model: `finance/model/FinanceModels.kt` (class `AppBankAccount`, `AppUserWallet`)

## 2. Quản Lý Thu / Chi (Income & Expense)
- **Mô tả:** Nhập liệu thủ công thu chi, hỗ trợ đính kèm hình ảnh giao dịch (Photo Diary) và quét hóa đơn bằng AI OCR (nhận diện chữ).
- **Cơ chế OCR:** Không cần gọi API, ứng dụng sử dụng thư viện `ML Kit Text Recognition` (On-device) quét ảnh chụp từ CameraX, dùng biểu thức chính quy (Regex) để lọc ra con số lớn nhất (thường là tổng tiền thanh toán) và tự điền vào Form.
- **Luồng xử lý:** `AddTransactionScreen` -> (Nếu quét ảnh) `BillScannerScreen` dùng CameraX + ML Kit -> Trả kết quả số tiền -> `FinanceViewModel.addTransaction()` -> Ghi vào Room DB & Đẩy ảnh lên Firebase Storage (nếu có).
- **Nơi chứa Code:** 
  - Màn hình nhập: `finance/ui/screens/AddTransactionScreen.kt`
  - Quét OCR: `finance/ui/screens/BillScannerScreen.kt`
  - Nhận diện: `mlkit` tích hợp ngay trong BillScannerScreen.
  - Nhật ký ảnh: `finance/ui/screens/PhotoDiaryScreen.kt`

## 3. Chia Tiền Nhóm Thông Minh (Group Split Bill)
- **Mô tả:** Khi bạn trả trước 400k cho nhóm 4 người, ứng dụng chia mỗi người 100k, ghi nhận bạn chi 100k và tạo ra 3 khoản nợ chờ thu hồi (300k).
- **Cơ chế hoạt động:** 
  - Tạo 1 `FinanceTransaction` loại `GROUP_PREPAYMENT` với tổng số tiền 400k. 
  - Trong list `participants`, đánh dấu trạng thái trả của từng người. 
  - Tự động trigger tạo các bản ghi `DebtLoan` (loại `LOAN`) đối với người chưa trả.
- **Luồng xử lý:** Lệnh từ Assistant (AI) -> `GeminiService` bóc tách ra list `participants` -> Hiển thị `SplitBillCard` -> Người dùng Xác nhận -> `FinanceViewModel.addGroupSplitBill()` -> Thực thi transaction lưu song song bảng `transactions` và `debts`.
- **Nơi chứa Code:**
  - AI Parser: `ui/assistant/AssistantViewModel.kt` (xử lý logic Split Bill từ JSON)
  - UI Xác nhận: `ui/assistant/AssistantComponents.kt` (Component `SplitBillCard`)
  - Logic Database: `finance/repository/FirestoreRepository.kt`

## 4. Quản Lý Nợ & Cho Vay (Debt & Loan)
- **Mô tả:** Theo dõi 2 chiều: Ai đang nợ mình (Loan) và Mình đang nợ ai (Debt), trạng thái trả.
- **Cơ chế hoạt động:** Khi đánh dấu khoản nợ đã trả (`isPaid = true`), hệ thống tự động sinh ra một Giao dịch loại `INCOME` (nếu là Loan) hoặc `EXPENSE` (nếu là Debt) để hoàn tiền vào ví được chọn.
- **Luồng xử lý:** `DebtLoanScreen` -> `FinanceViewModel.addDebtLoan()` -> Room DB. Khi hoàn trả: `FinanceViewModel.markDebtPaid(debtId, walletId)` -> Cập nhật trạng thái + Tạo giao dịch mới.
- **Nơi chứa Code:**
  - Giao diện: `finance/ui/screens/DebtLoanScreen.kt`
  - Model: `finance/model/FinanceModels.kt` (class `DebtLoan`)

## 5. Quản Lý Ngân Sách (Budgeting)
- **Mô tả:** Đặt giới hạn chi tiêu tuần/tháng. Tự động cảnh báo đỏ khi sắp vượt mức 85%.
- **Cơ chế hoạt động:** Bằng việc tính toán `SUM(amount)` của các giao dịch loại `EXPENSE` nằm trong mốc thời gian (Tuần/Tháng hiện tại) và so sánh với hạn mức (`amount`) được định nghĩa trong bảng Budget.
- **Luồng xử lý:** Truy vấn `Flow<List<FinanceTransaction>>` (lọc theo ngày) kết hợp `Flow<List<FinanceBudget>>` -> `combine()` trong ViewModel để xuất ra tỷ lệ % -> UI vẽ thanh Progress Bar.
- **Nơi chứa Code:**
  - Giao diện: `finance/ui/screens/BudgetScreen.kt`
  - Logic tính toán: `finance/ui/wrappers/BudgetViewModelWrapper.kt` hoặc `FinanceViewModel`

## 6. Mục Tiêu Tiết Kiệm & Quỹ Giữ Hộ (Savings & Held Funds)
- **Mô tả:** Theo dõi tích lũy mua xe/du lịch. Hoặc quỹ giữ hộ lớp để tiền không bị cộng vào số dư thực.
- **Cơ chế hoạt động:** 
  - Quỹ giữ hộ (`HeldFundItem`) có biến đếm `amount` độc lập, tách biệt khỏi `accounts`. 
  - Quỹ tiết kiệm (`SavingsGoal`) ghi nhận luồng tiền nạp (`deposit`) từ ví chính. Khi nạp tiền, số dư ví chính sẽ giảm, số dư mục tiêu tiết kiệm sẽ tăng (dạng internal transfer).
- **Luồng xử lý:** Người dùng chọn nạp tiền -> `FinanceViewModel.depositSavings()` -> Trừ tiền ở ví hiện tại -> Cộng tiền vào `SavingsGoal`.
- **Nơi chứa Code:**
  - Tiết kiệm: `finance/ui/screens/SavingsGoalScreen.kt` và `GeneralSavingsScreen.kt`
  - Quỹ giữ hộ: `finance/ui/screens/HeldFundsManagementScreen.kt`

## 7. Lịch Trình Tự Động (Weekly Schedule)
- **Mô tả:** Lên lịch thu chi tự động hàng tuần (VD: tiền ăn sáng thứ 2).
- **Cơ chế hoạt động:** Sử dụng `WorkManager` (dạng `PeriodicWorkRequest`) chạy ngầm mỗi ngày 1 lần. Nó sẽ kiểm tra xem ngày hôm nay (VD: Thứ 2) có trùng với `dayOfWeek` của bất kỳ lịch trình nào trong database không. Nếu có, tự động Insert một `FinanceTransaction` mới.
- **Luồng xử lý:** Background `AutoScheduleWorker.doWork()` -> Đọc bảng Schedule -> Insert vào bảng Transaction -> Bắn Local Notification báo cho người dùng.
- **Nơi chứa Code:**
  - Giao diện thiết lập: `finance/ui/screens/WeeklyScheduleScreen.kt`
  - Background Worker: Có thể nằm trong package `service/` hoặc `work/` (Ví dụ `AutoScheduleWorker`).

## 8. Lắng Nghe Thông Báo Ngân Hàng (Auto SMS/App Parsing)
- **Mô tả:** Đọc lén notification từ MoMo, VCB... tự tạo sẵn giao dịch.
- **Cơ chế hoạt động:** Kế thừa class `NotificationListenerService` của Android. 
  - Điều kiện lọc: Chỉ nhận notification từ các package name nhất định (VD: `com.mservice.momotransfer`).
  - Xử lý Regex: Quét chuỗi dạng `GD: -50,000VND`, bóc tách con số 50000. Lọc bỏ các con số dư tài khoản (`SD: xxx`).
  - Gửi Intent thông qua BroadcastReceiver hoặc DataStore lên UI để hiện Pop-up.
- **Luồng xử lý:** Hệ thống OS -> `BankNotificationListener.onNotificationPosted()` -> Lọc Package -> Chạy Regex Regex -> Lưu Draft xuống Local -> Báo cho ViewModel hiện UI.
- **Nơi chứa Code:**
  - Service lắng nghe: `service/BankNotificationListener.kt`

## 9. Thống Kê & Phân Tích (Analytics)
- **Mô tả:** Vẽ biểu đồ hình tròn, hình cột theo thời gian.
- **Cơ chế hoạt động:** Sử dụng thư viện `Vico Compose` để vẽ đồ thị. Các giao dịch được `groupBy { it.category }` để tính tổng chi tiêu từng hạng mục.
- **Luồng xử lý:** `FinanceViewModel` xử lý tính toán GroupBy -> Đẩy Data dưới dạng `List<PieChartData>` -> `AnalyticsScreen` lấy Data vẽ biểu đồ.
- **Nơi chứa Code:**
  - Màn hình đồ thị: `finance/ui/screens/AnalyticsScreen.kt`
  - Biểu đồ: Thư viện Vico hoặc Canvas tự vẽ.

## 10. Trợ Lý AI Chuyên Sâu (Hybrid AI Assistant)
- **Mô tả:** Nhắn tin để hệ thống AI tự tạo form ghi nhận (VD: "ăn sáng 45k").
- **Cơ chế hoạt động:** Áp dụng **Hybrid AI** (AI 2 tầng):
  1. **Tầng Local**: Khi nhận câu chat, `LocalAIEngine` sẽ chạy Regex (VD: bóc các cụm từ "ăn", "uống", "mua", và con số đằng sau) để tạo giao dịch offline siêu tốc, tiết kiệm API.
  2. **Tầng API (Gemini)**: Nếu câu phức tạp (VD: Chia bill), ViewModel đẩy lên API Gemini 2.5 Flash kèm mảng **Function Calling** (11 tools như `addTransaction`, `addGroupSplitBill`). Gemini sẽ phản hồi bằng một cục JSON format chứa tên hàm và tham số.
- **Luồng xử lý:** Màn hình Chat -> Gửi Message -> (Thử Local) -> Nếu fail gửi lên `GeminiService.getCompletion()` -> Nhận JSON response -> Map JSON thành object UI Card (`TransactionCard`) -> Bấm nút Lưu -> Gọi hàm Insert Database.
- **Nơi chứa Code:**
  - Tầng API (định nghĩa Functions): `data/remote/GeminiService.kt`
  - Tầng Local: `finance/util/LocalAIEngine.kt`
  - Quản lý logic chat: `ui/assistant/AssistantViewModel.kt`
  - Màn hình chat: `ui/assistant/AssistantScreen.kt`

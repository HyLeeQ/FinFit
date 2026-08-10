e# FinFit — Mô Tả Project Toàn Diện

## 1. Tổng Quan Dự Án

**Tên ứng dụng:** FinFit  
**Nền tảng:** Android (Native)  
**Ngôn ngữ:** Kotlin  
**Phiên bản hiện tại:** 1.0 (đang phát triển)  
**Target SDK:** Android 34 (Android 14), Min SDK: Android 24 (Android 7.0+)

### Tầm nhìn (Vision)
FinFit là một **hệ sinh thái ứng dụng quản lý cá nhân toàn diện** dành cho người dùng Việt Nam, tập trung vào hai trụ cột chính:
1. **Tài chính cá nhân (Finance)** — theo dõi chi tiêu, tiết kiệm, nợ/vay, ngân sách
2. **Sức khỏe (Health)** — đếm bước chân, nước uống, calo, giấc ngủ

Triết lý cốt lõi: *"Cơ thể khoẻ mạnh là nền tảng của mọi thành công."*

---

## 2. Công Nghệ Sử Dụng (Tech Stack)

| Lớp | Công nghệ |
|-----|-----------|
| Ngôn ngữ | Kotlin (Coroutines + Flow) |
| UI Framework | Jetpack Compose + Material Design 3 |
| Kiến trúc | MVVM + Clean Architecture + Single Source of Truth |
| Database cục bộ | Room Database (SQLite, Offline-First) |
| Cloud & Auth | Firebase Firestore + Firebase Authentication + Firebase Storage |
| AI | Google Gemini 2.0 Flash (Function Calling) + LocalAIEngine (on-device) |
| OCR | ML Kit Text Recognition (on-device, không cần API) |
| Background Tasks | WorkManager + Foreground Service |
| Hardware | Android Sensor Manager (Step Counter, Step Detector) |
| DI | Thủ công (ViewModelProvider.Factory) |
| Charts | Vico Compose (Material 3) |
| Image Loading | Coil Compose |
| Animations | Lottie Compose |
| Build | Gradle (Groovy DSL) + KSP |

---

## 3. Kiến Trúc Hệ Thống

### 3.1 Mô Hình Phân Tầng

```
┌─────────────────────────────────────────┐
│           UI Layer (Jetpack Compose)    │
│   Screens → ViewModel → StateFlow/UI   │
├─────────────────────────────────────────┤
│         Domain / ViewModel Layer        │
│   Business Logic, Use Cases, AI Engine │
├─────────────────────────────────────────┤
│           Data Layer                    │
│  Repository Pattern → Room ← Firestore │
└─────────────────────────────────────────┘
```

**Luồng dữ liệu (Unidirectional Data Flow):**
- UI **chỉ đọc** từ Room Database (nguồn chân lý duy nhất)
- Firestore chịu trách nhiệm **đồng bộ ngầm** vào Room
- ViewModel nhận lệnh từ UI, gọi Repository xử lý, cập nhật StateFlow

### 3.2 Cấu Trúc Package (Package-by-Feature)

```
com.example.finfit/
├── MainActivity.kt              # Entry point, Navigation host
├── core/                        # Shared utilities
├── data/
│   ├── local/                   # Room DAO, Database
│   ├── model/                   # Shared data models
│   ├── remote/
│   │   └── GeminiService.kt     # Gemini AI API integration
│   └── repository/              # Data access layer
├── finance/
│   ├── model/
│   │   └── FinanceModels.kt     # Toàn bộ data class tài chính
│   ├── repository/
│   │   └── FirestoreRepository  # Firestore CRUD operations
│   ├── ui/
│   │   ├── screens/             # 15 màn hình Finance
│   │   ├── navigation/          # Finance NavGraph
│   │   ├── utils/               # Format tiền tệ, helpers
│   │   └── wrappers/            # ViewModel wrappers
│   └── util/
│       └── LocalAIEngine.kt     # AI on-device (không cần API)
├── health/
│   ├── data/                    # Health DAO, sensors
│   ├── model/                   # HealthData models
│   ├── repository/              # Health repository
│   └── ui/                      # Health screens
├── service/
│   └── BankNotificationListener.kt  # Tự động đọc SMS ngân hàng
└── ui/
    ├── MainActivity screens (Auth, Onboarding, Profile...)
    ├── assistant/
    │   ├── AssistantScreen.kt       # Màn hình chat AI
    │   ├── AssistantViewModel.kt    # Logic AI + Function Calling
    │   ├── AssistantComponents.kt   # Card UI cho từng action
    │   └── AssistantModels.kt       # ChatMessage sealed classes
    └── theme/                       # Design tokens, Colors, Typography
```

---

## 4. Module Tài Chính (Finance Module)

### 4.1 Màn Hình (15 Screens)

| Màn hình | Chức năng |
|----------|-----------|
| `DashboardScreen` | Tổng quan tài chính: số dư, biểu đồ, giao dịch gần đây |
| `AddTransactionScreen` | Thêm thu/chi thủ công + chụp hóa đơn OCR |
| `TransactionHistoryScreen` | Lịch sử giao dịch theo thời gian |
| `AnalyticsScreen` | Biểu đồ phân tích chi tiêu theo danh mục/kỳ |
| `BudgetScreen` | Thiết lập và theo dõi hạn mức ngân sách |
| `SavingsGoalScreen` | Quản lý mục tiêu tiết kiệm có mục đích |
| `GeneralSavingsScreen` | Tiết kiệm dự phòng không mục đích |
| `DebtLoanScreen` | Ghi nợ / cho vay theo tên người |
| `WalletManagementScreen` | Quản lý đa tài khoản ngân hàng / ví điện tử |
| `HeldFundsManagementScreen` | Quản lý quỹ giữ hộ / quỹ nhóm |
| `InternalTransferScreen` | Chuyển tiền nội bộ giữa các ví |
| `WeeklyScheduleScreen` | Lịch trình chi tiêu theo ngày trong tuần |
| `PhotoDiaryScreen` | Nhật ký ảnh hóa đơn / kỷ niệm chi tiêu |
| `FinanceCategories` | Quản lý danh mục thu/chi |
| `AssistantScreen` | Chat với AI trợ lý tài chính |

### 4.2 Data Models Chính

```kotlin
// Ví tổng hợp người dùng
data class AppUserWallet(
    val accounts: List<AppBankAccount>,     // Danh sách tài khoản
    val generalSavings: Double,             // Tiết kiệm dự phòng
    val heldFunds: List<HeldFundItem>,      // Quỹ giữ hộ
    val groupPrepaidAmount: Double,         // Tiền trả hộ nhóm chưa hoàn
    val autoSaveWeeklySurplus: Boolean      // Tự động tiết kiệm thặng dư
)

// Giao dịch
data class FinanceTransaction(
    val amount: Double,
    val type: TransactionType,              // EXPENSE, INCOME, TRANSFER, GROUP_PREPAYMENT
    val category: String,
    val accountId: String?,                 // Tài khoản thực hiện
    val isGroupPrepayment: Boolean,         // Có phải trả hộ nhóm không
    val participants: List<TransactionParticipant>  // Chi tiết từng người chia bill
)

// Mục tiêu tiết kiệm
data class SavingsGoal(
    val goalName: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Timestamp?,
    val autoSavingAmount: Double            // Tự động nạp tiền hàng tuần
)

// Nợ / Cho vay
data class DebtLoan(
    val personName: String,
    val amount: Double,
    val type: DebtLoanType,                 // DEBT (mình nợ) / LOAN (cho vay)
    val dueDate: Timestamp?,
    val isPaid: Boolean
)

// Ngân sách
data class FinanceBudget(
    val amount: Double,
    val period: BudgetPeriod,               // WEEKLY / MONTHLY
    val category: String                    // "Tất cả" hoặc hạng mục cụ thể
)
```

### 4.3 Hỗ Trợ Ngân Hàng Việt Nam

Ứng dụng tích hợp sẵn danh sách 16 ngân hàng và ví điện tử phổ biến tại Việt Nam:
- **Ví điện tử:** MoMo, ZaloPay
- **Ngân hàng nội địa:** MB Bank, Techcombank, Vietcombank, BIDV, VPBank, ACB, Sacombank, VietinBank, Agribank, TPBank, MSB
- **Ngân hàng nước ngoài:** Shinhan Bank
- **Khác:** Tiền mặt, Tài khoản khác

---

## 5. Trợ Lý AI (AI Assistant)

### 5.1 Kiến Trúc AI Hai Tầng (Hybrid AI)

FinFit sử dụng chiến lược **Hybrid AI** để tối ưu tốc độ phản hồi và tiết kiệm API quota:

```
Tin nhắn người dùng
        │
        ▼
┌─────────────────────────────────┐
│  TẦNG 1: LocalAIEngine (On-device) │
│  - Parse multi-transaction      │
│  - Trả lời câu hỏi đơn giản    │
│  - Budget alerts                │
│  - Smart suggestions            │
└──────────────┬──────────────────┘
               │ Không xử lý được
               ▼
┌─────────────────────────────────┐
│  TẦNG 2: Gemini 2.0 Flash API  │
│  - Function Calling             │
│  - Hiểu ngữ cảnh phức tạp      │
│  - Lập kế hoạch tuần           │
└─────────────────────────────────┘
```

### 5.2 Gemini Function Calling Tools (11 công cụ)

| Tool | Mô tả |
|------|-------|
| `addTransaction` | Thêm giao dịch thu/chi từ tin nhắn tự nhiên |
| `addDebtLoan` | Tạo khoản nợ hoặc cho vay |
| `addSavingsGoal` | Tạo mục tiêu tiết kiệm mới |
| `addBudget` | Thiết lập hạn mức ngân sách |
| `addGroupSplitBill` | Chia bill nhóm với tracking từng người |
| `addAutoSchedule` | Đặt lịch chi tiêu tự động hàng tuần |
| `addHeldFund` | Tạo quỹ giữ hộ/quỹ nhóm |
| `updateUserHabit` | Cập nhật thói quen chi tiêu để AI ghi nhớ |
| `proposeWeeklyPlan` | Đề xuất kế hoạch chi tiêu cả tuần |
| `depositSavings` | Nạp tiền vào quỹ tiết kiệm |
| `withdrawSavings` | Rút/chuyển tiền từ quỹ tiết kiệm |

### 5.3 LocalAIEngine (Không cần API)

Xử lý on-device hoàn toàn:
- **Parse multi-transaction:** Nhận dạng nhiều giao dịch trong một câu (vd: "ăn sáng 20k, cafe 15k")
- **Trả lời Q&A đơn giản:** Số dư, tổng chi tháng, giao dịch lớn nhất...
- **Budget alerts:** Cảnh báo khi sắp vượt 85% hạn mức
- **Smart suggestions (chips):** Gợi ý nhanh dựa trên giờ trong ngày và lịch sử danh mục
- **Spending insights:** Tổng chi hôm nay/tuần/tháng, so sánh tuần trước

### 5.4 Luồng Xử Lý AI

1. Người dùng nhắn tin → LocalAI thử xử lý trước
2. Nếu LocalAI không đủ → Gọi Gemini API với System Context (số dư, lịch sử, thói quen)
3. Gemini trả về **Function Call** → ViewModel hiển thị **Confirmation Card** trên UI
4. Người dùng bấm **Xác nhận** → Lưu vào Firestore + cập nhật số dư tức thì

### 5.5 Các Loại Confirmation Card

Mỗi action AI tạo ra một Card tương tác riêng biệt:
- **TransactionCard** — Xác nhận giao dịch thu/chi
- **DebtCard** — Xác nhận ghi nợ/cho vay
- **SavingsCard** — Xác nhận tạo mục tiêu tiết kiệm
- **BudgetCard** — Xác nhận hạn mức ngân sách
- **SplitBillCard** — Xác nhận chia bill nhóm (có tracking từng người)
- **ScheduleCard** — Xác nhận lịch trình tự động
- **HeldFundCard** — Xác nhận quỹ giữ hộ
- **HabitUpdateCard** — Xác nhận cập nhật thói quen
- **WeeklyPlanCard** — Xác nhận kế hoạch cả tuần
- **DepositSavingsCard** — Xác nhận nạp tiền tiết kiệm
- **WithdrawSavingsCard** — Xác nhận rút tiền tiết kiệm
- **BillCard** — Xác nhận hóa đơn chụp từ camera (OCR)

---

## 6. Module Sức Khỏe (Health Module)

### 6.1 Tính Năng

| Tính năng | Công nghệ |
|-----------|-----------|
| **Đếm bước chân** | `Sensor.TYPE_STEP_COUNTER` qua Foreground Service |
| **Theo dõi nước uống** | Room DB + Firestore sync |
| **Calo nạp vào (Thức ăn)** | Room DB + Firestore sync |
| **Calo tiêu thụ** | Tính toán dựa trên bước chân + cân nặng |
| **Thời gian ngủ** | Nhập thủ công + Room DB |

### 6.2 Offline-First & Đồng Bộ

- **Foreground Service** liên tục lắng nghe cảm biến bước chân ngay cả khi app đóng
- **Thuật toán Offset Sync:** Bù trừ tự động khi máy khởi động lại (hardware reset về 0)
- **WorkManager Periodic Sync:** Tự động đẩy dữ liệu lên Firebase mỗi 30 phút
- **Silent Sync:** Đồng bộ lần cuối khi người dùng vuốt tắt app
- **Merge Resolution:** Dùng SQL `MAX()` để bước chân không bao giờ bị giảm lùi

---

## 7. Tính Năng Đặc Biệt

### 7.1 Tự Động Đọc Thông Báo Ngân Hàng
File: `BankNotificationListener.kt`
- Lắng nghe thông báo SMS/App từ các ngân hàng
- Tự động parse số tiền giao dịch từ nội dung thông báo
- Hỗ trợ xử lý format MIUI (phân biệt "GD:" là giao dịch, "SD:" là số dư)
- Loại bỏ thông tin không liên quan (ngày giờ, số dư) để tránh nhầm lẫn

### 7.2 Chụp Hóa Đơn OCR (On-device)
- Sử dụng **ML Kit Text Recognition** — không cần API, hoạt động offline
- Camera khởi động trực tiếp từ nút Camera trong UI
- OCR extract số tiền tự động từ ảnh hóa đơn

### 7.3 Nhật Ký Ảnh (Photo Diary)
- Chụp ảnh kèm giao dịch để lưu kỷ niệm mua sắm
- Lưu ảnh lên Firebase Storage
- Hiển thị dạng timeline trong `PhotoDiaryScreen`

### 7.4 AI Ghi Nhớ Thói Quen Người Dùng
- Model `UserHabit` lưu chi phí bữa ăn tối thiểu/tối đa
- Lịch trình theo ngày (vd: T2-T4 ở trọ, T5 về quê)
- AI dùng thông tin này để lập kế hoạch tuần thông minh hơn
- Proactive message mỗi sáng Thứ Hai để lên kế hoạch tuần

### 7.5 Split Bill Thông Minh
- Chia bill cho nhóm người với tracking từng thành viên
- Theo dõi ai đã trả, ai chưa trả, trả bao nhiêu
- Tự động tạo DebtLoan cho phần chưa hoàn trả
- Cập nhật số dư ví tức thì sau khi xác nhận

---

## 8. Thiết Kế Giao Diện (Design System)

### 8.1 Triết Lý: "Luminescent Observer"
Giao diện FinFit được thiết kế theo phong cách **editorial, premium, dark-first**, không giống các app tài chính thông thường mang cảm giác lạnh và cứng nhắc.

### 8.2 Bảng Màu (Color Palette — "Deep Space")

| Token | Màu | Ý nghĩa |
|-------|-----|---------|
| `background` | #0E0E0E | Nền anchor tối chủ đạo |
| `surface-container` | #1A1A1A | Nền card chính |
| `primary` | #64B5F6 | Xanh dương — Nước / Sức sống |
| `secondary` | #EA73FB | Tím hồng — Giấc ngủ / Phục hồi |
| `tertiary` | #BBFFB3 | Xanh lá — Hoạt động / Tăng trưởng |

**Quy tắc "No-Line":** Tuyệt đối không dùng border 1px. Ranh giới giữa các vùng chỉ được tạo bằng cách thay đổi màu nền (tonal layering).

### 8.3 Typography
- **Display & Headlines:** Font **Manrope** — mạnh mẽ, có authority
- **Titles & Body:** Font **Inter** — dễ đọc, tối ưu cho data trên nền tối

### 8.4 Nguyên Tắc Thiết Kế
- **Glassmorphism** cho FAB và các phần nổi bật
- **Ambient Glow** (40px blur, primary color 6% opacity) thay cho drop shadow
- **Editorial Asymmetry** — Layout không đối xứng tạo cảm giác premium
- **Micro-animations** — Transitions mượt mà, tạo cảm giác sống động
- **Circular Progress Indicators** — Stroke 12-16px cho health metrics

---

## 9. Luồng Người Dùng (User Flows)

### 9.1 Onboarding
1. Splash Screen → Auth (Đăng ký/Đăng nhập Firebase)
2. Onboarding Screen (giới thiệu tính năng)
3. Setup Currency Screen (chọn đơn vị tiền tệ)
4. Setup Categories Screen (tùy chỉnh danh mục)
5. Main Screen (Dashboard)

### 9.2 Ghi Giao Dịch qua AI
1. Mở màn hình Chat AI
2. Nhắn: *"ăn tối 45k"*
3. LocalAI parse ngay → hiện **TransactionCard**
4. Bấm **Xác nhận** → Firestore + số dư ví cập nhật

### 9.3 Chia Bill Nhóm
1. Nhắn: *"chia bill lẩu 400k với Nam, Tùng, Linh"*
2. Gemini Function Call → `addGroupSplitBill`
3. Hiện **SplitBillCard** với từng người và phần chi
4. Đánh dấu ai đã trả, bấm Xác nhận
5. Tự động tạo khoản nợ cho những người chưa trả

---

## 10. Bảo Mật & Quy Chuẩn Xử Lý

| Vấn đề | Giải pháp |
|--------|-----------|
| **API Key** | Lưu trong `local.properties`, inject vào `BuildConfig` — không commit lên Git |
| **User Data** | Phân cấp theo `users/{userUID}/` trong Firestore |
| **Offline Safety** | Mọi thao tác viết vào Room trước, sync sau |
| **Error Handling** | `try/catch` toàn bộ network/IO trong `Dispatchers.IO`, xử lý `QuotaExceededException` |
| **ANR Prevention** | Tất cả tác vụ nặng chạy trong Coroutine scope, không block Main thread |

---

## 11. Các Màn Hình Xác Thực & Profile

| Màn hình | Chức năng |
|----------|-----------|
| `AuthScreens.kt` | Đăng nhập / Đăng ký Firebase Auth |
| `OnboardingScreen.kt` | Giới thiệu app lần đầu (4 slide) |
| `ProfileScreen.kt` | Xem và chỉnh sửa thông tin cá nhân |
| `EditProfileScreen.kt` | Form chỉnh sửa profile chi tiết |
| `SetupCurrencyScreen.kt` | Chọn đơn vị tiền tệ |
| `SetupCategoriesScreen.kt` | Tùy chỉnh danh mục thu/chi |

---

## 12. Điểm Khác Biệt So Với Các App Tài Chính Thông Thường

1. **AI thực sự hữu dụng, không chỉ là chatbot:** Mỗi tin nhắn AI có thể tạo giao dịch, thiết lập ngân sách, lên kế hoạch tuần — tất cả qua ngôn ngữ tự nhiên tiếng Việt.

2. **Hybrid AI thông minh:** Không lãng phí API call. LocalAIEngine xử lý 70-80% yêu cầu đơn giản ngay trên thiết bị, Gemini chỉ được gọi khi cần thiết.

3. **Confirmation Card UI:** Thay vì AI tự động thực thi, mỗi hành động đều hiển thị card để người dùng kiểm tra và xác nhận trước khi ghi — an toàn và minh bạch.

4. **Offline-First tuyệt đối:** App hoạt động 100% không cần mạng (trừ AI cloud). Dữ liệu sức khỏe không bao giờ bị mất dù tắt nguồn hay mất kết nối.

5. **Tích hợp sinh thái Việt Nam:** Hỗ trợ 16 ngân hàng Việt, đọc thông báo ngân hàng tự động, xử lý tiếng Việt trong AI.

6. **Split Bill thông minh:** Không chỉ tính toán, mà còn tự động tạo khoản nợ, theo dõi ai trả rồi và cập nhật số dư tức thì.

---

## 13. Trạng Thái Phát Triển Hiện Tại

### Đã Hoàn Thành ✅
- Toàn bộ 15 màn hình Finance
- AI Assistant với 11 Function Calling tools
- LocalAIEngine on-device
- Đọc thông báo ngân hàng tự động
- OCR chụp hóa đơn
- Sức khỏe: Bước chân, Nước, Calo, Ngủ
- Offline-First + Cloud Sync
- Xác thực Firebase, Profile, Onboarding
- Split Bill tracking cá nhân
- Nhật ký ảnh

### Tiềm Năng Phát Triển 🚀
- Widget màn hình chính
- Xuất báo cáo PDF
- Thông báo thông minh theo ngân sách
- Đồng bộ nhiều thiết bị real-time
- Phân tích AI nâng cao (dự báo chi tiêu)
- Apple Health / Google Fit integration

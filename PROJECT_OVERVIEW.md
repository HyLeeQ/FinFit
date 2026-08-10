# Tổng Quan Dự Án FinFit

**FinFit** (Finance & Fitness) là một ứng dụng di động toàn diện kết hợp giữa **Quản lý Tài chính cá nhân** và **Theo dõi Sức khỏe**, được viết bằng Kotlin (Jetpack Compose) theo kiến trúc hiện đại.

Dự án có sự tích hợp mạnh mẽ của **Trí tuệ Nhân tạo (AI)** (Gemini, ML Kit OCR, YOLO) để tự động hóa các tác vụ như quét hóa đơn, nhận diện thức ăn, đọc thông báo ngân hàng và tư vấn qua Assistant.

Dưới đây là cấu trúc kiến trúc và chi tiết các module chính của dự án:

---

## 🏗️ Cấu trúc thư mục cốt lõi
*(Đường dẫn: `app/src/main/java/com/example/finfit`)*

### 1. `core` (Lõi ứng dụng)
Chứa các thành phần được sử dụng chung xuyên suốt toàn bộ ứng dụng.
*   **`navigation`**: Quản lý điều hướng chung của app (`AppNavigation.kt`).
*   **`ui`**: Các UI Component dùng chung (`SharedComponents.kt`).
*   **`utils`**: Các hằng số (`Constants.kt`) và tiện ích cơ bản.

### 2. `data` (Tầng Dữ liệu chung)
Quản lý dữ liệu nội bộ và các kết nối ra bên ngoài không thuộc riêng Finance hay Health.
*   **`local`**: Lưu trữ cài đặt người dùng qua DataStore/SharedPreferences (`SetupPreferences.kt`, `ThemePreferences.kt`).
*   **`remote`**: Các dịch vụ gọi API bên ngoài, tiêu biểu là `GeminiService.kt` (Kết nối Google Gemini AI).
*   **`repository`**: Repository xử lý Đăng nhập / Đăng ký (`AuthRepository.kt`).

### 3. `finance` (Module Tài chính - *Fin*)
Module khổng lồ đảm nhiệm mọi tính năng liên quan đến tiền bạc.
*   **`model`**: Định nghĩa cấu trúc dữ liệu tài chính (Giao dịch, Ví, Ngân sách, Nợ/Cho vay...).
*   **`repository`**: Tương tác với cơ sở dữ liệu Firebase Firestore (`FirestoreRepository.kt`).
*   **`util`**: Các tiện ích thông minh như `SmartTransactionParser.kt` (phân tích thông báo ngân hàng ra dữ liệu giao dịch) và `LocalAIEngine.kt`.
*   **`ui/screens`**: Chứa toàn bộ giao diện tài chính như:
    *   `DashboardScreen.kt`: Màn hình tổng quan tài chính.
    *   `BillScannerScreen.kt`: Quét và nhận diện hóa đơn (OCR).
    *   `WalletManagementScreen.kt`: Quản lý nhiều ví/tài khoản.
    *   `BudgetScreen.kt` & `AnalyticsScreen.kt`: Ngân sách và Báo cáo thống kê.
    *   `DebtLoanScreen.kt`: Quản lý Nợ và Cho vay.
    *   `GeneralSavingsScreen.kt` & `SavingsGoalScreen.kt`: Quản lý tiết kiệm.

### 4. `health` (Module Sức khỏe - *Fit*)
Module lớn thứ hai, theo dõi các chỉ số sức khỏe và dinh dưỡng.
*   **`ai` & `api.vision`**: Tích hợp AI nhận diện thức ăn (YOLO - `YoloFoodDetector.kt`), kết nối Gemini Vision, tìm kiếm dinh dưỡng qua Edamam API.
*   **`model` & `data`**: Định nghĩa dữ liệu Bữa ăn, Giấc ngủ, Bước chân, Lượng nước uống. Có worker đồng bộ dữ liệu (`HealthSyncWorker.kt`).
*   **`manager`**: Các trình quản lý ngầm như đếm bước chân (`StepCounterManager.kt`) và nhắc nhở uống nước (`WaterReminderManager.kt`).
*   **`repository`**: Gồm Room Database (`HealthDatabase.kt`) và rất nhiều DAO, Repository, ViewModel xử lý logic nội bộ.
*   **`ui`**: Các giao diện chính:
    *   `HealthScreen.kt`: Tổng quan sức khỏe.
    *   `FoodCameraScreen.kt`: Chụp ảnh nhận diện món ăn tính calo.
    *   `StepCounterScreen.kt`: Đếm bước chân.
    *   `WaterTrackerScreen.kt`: Theo dõi uống nước.
    *   `SleepScheduleScreen.kt`: Lịch trình giấc ngủ.

### 5. `service` (Dịch vụ chạy ngầm)
*   **`BankNotificationListener.kt`**: Một Service cực kỳ quan trọng dùng Notification Listener API để "lắng nghe" thông báo trừ/cộng tiền từ các app Ngân hàng, sau đó chuyển sang `SmartTransactionParser` để tự động ghi chép giao dịch.

### 6. `ui` (Giao diện cấp cao & App Shell)
Chứa các màn hình gốc rễ của ứng dụng.
*   Các màn hình khởi đầu: `SplashScreen.kt`, `OnboardingScreen.kt`, `AuthScreens.kt`.
*   Thiết lập ban đầu: `SetupCurrencyScreen.kt`, `SetupCategoriesScreen.kt`.
*   Trang chính: `MainScreen.kt` (chứa Bottom Navigation).
*   **`assistant`**: Tính năng Chatbot AI (Tư vấn viên Tài chính - Sức khỏe ảo) (`AssistantScreen.kt`, `AssistantViewModel.kt`).
*   **`theme`**: Quản lý màu sắc, typography và giao diện (Dark/Light mode).

---

## 🚀 Các Tính Năng Nổi Bật (Highlights)
1. **Quản Lý Tiền Tự Động**: Tự động bắt thông báo biến động số dư ngân hàng và ghi nhận giao dịch mà không cần nhập tay.
2. **AI Quét Hóa Đơn (OCR)**: Sử dụng ML Kit để trích xuất số tiền từ ảnh chụp hóa đơn mua sắm.
3. **AI Dinh Dưỡng (Food Vision)**: Chụp ảnh bữa ăn, AI (YOLO/Gemini) sẽ tự động nhận dạng món ăn và ước tính lượng Calo/Dinh dưỡng.
4. **Trợ lý Ảo (Assistant)**: Tích hợp Gemini LLM để trả lời các câu hỏi về tài chính cá nhân, sức khỏe, hoặc phân tích thói quen tiêu dùng của chính người dùng.
5. **Theo dõi sức khỏe toàn diện**: Đếm bước, nhắc uống nước, theo dõi giấc ngủ tích hợp chung một nền tảng với tài chính (Sự kết nối giữa thói quen tiêu xài và sức khỏe).

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)
*   **Ngôn ngữ**: Kotlin
*   **Giao diện**: Jetpack Compose (100%)
*   **Kiến trúc**: MVVM + Clean Architecture principles
*   **Backend / DB**: Firebase (Authentication, Firestore), Room Database (Local DB cho Health).
*   **AI / Machine Learning**: Google ML Kit (OCR), Google Gemini API, YOLO (nhận diện hình ảnh cục bộ).
*   **Hình ảnh**: Coil (Load ảnh), Cloudinary (Lưu trữ ảnh đám mây).
*   **Dịch vụ ngầm**: Foreground Services (đếm bước), Notification Listener (ngân hàng), WorkManager (đồng bộ).

---
*File này được tạo tự động để giúp lập trình viên và người xem nắm bắt nhanh kiến trúc tổng thể của siêu ứng dụng FinFit.*

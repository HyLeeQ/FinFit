# TÀI LIỆU ÔN TẬP BẢO VỆ ĐỒ ÁN - FINFIT

Tài liệu này tổng hợp toàn bộ các thông tin cốt lõi của dự án FinFit, giúp bạn nắm vững kiến trúc, nguyên lý hoạt động, luồng xử lý và tự tin trả lời các câu hỏi phản biện trước Hội đồng bảo vệ đồ án/chuyên đề.

---

## 1. MÔ TẢ CHỨC NĂNG CỦA PROJECT

FinFit là một **hệ sinh thái ứng dụng quản lý cá nhân toàn diện** tập trung vào 2 trụ cột là **Tài chính** và **Sức khỏe**, được tích hợp **AI thông minh**.

### 1.1 Trụ cột Tài Chính (Finance)
- **Quản lý đa tài khoản/ví**: Theo dõi số dư từ nhiều nguồn (tiền mặt, MoMo, Vietcombank...).
- **Quản lý Thu/Chi**: Theo dõi chi tiêu và thu nhập hàng ngày. Hỗ trợ OCR chụp hóa đơn để tự động trích xuất số tiền.
- **Tiết kiệm & Mục tiêu**: Thiết lập và theo dõi các quỹ tiết kiệm (có mục tiêu như mua xe, du lịch).
- **Chia tiền nhóm (Split Bill)**: Chia tiền thông minh cho các cuộc đi chơi, theo dõi ai đã trả, tự động ghi nợ cho người chưa trả.
- **Tự động đọc SMS Ngân hàng**: Tự động lấy biến động số dư từ thông báo điện thoại mà không cần nhập tay.

### 1.2 Trụ cột Sức Khỏe (Health)
- **Đếm bước chân (Step Counter)**: Đếm bước ngầm (Foreground service), tính lượng calo tiêu thụ.
- **Theo dõi lượng nước & Caffeine**: Tính toán lượng nước nạp vào với hệ số bù nước (Hydration Index). Cảnh báo khi nạp quá nhiều Caffeine.
- **Tính lượng Calo ăn uống**: Sử dụng Camera AI (Gemini Vision) chụp món ăn để nhận diện tên món ăn và số Calo, Protein, Fat, Carbs nạp vào.
- **Theo dõi giấc ngủ**: Ghi nhận giờ đi ngủ, giờ thức dậy, tự động cài báo thức hệ thống.

### 1.3 Trợ Lý Thông Minh (AI Assistant)
- **Chat tự nhiên**: Cho phép người dùng nhập lệnh (VD: "ăn sáng 45k", "chia lẩu 400k với Nam, Tùng"). AI sẽ phân tích và tạo form để xác nhận ghi nhận.
- **Lên kế hoạch thông minh**: Ghi nhớ thói quen, đưa ra phân tích, cảnh báo khi chi tiêu vượt ngân sách.

---

## 2. NGUYÊN LÝ HOẠT ĐỘNG (ARCHITECTURE & PRINCIPLES)

### 2.1 Kiến trúc MVVM & Clean Architecture
Dự án được xây dựng theo mô hình MVVM (Model - View - ViewModel) với luồng dữ liệu một chiều (Unidirectional Data Flow):
- **View (Jetpack Compose)**: UI chỉ lấy dữ liệu (`StateFlow`) từ ViewModel để hiển thị, gửi sự kiện (Events) ngược lại cho ViewModel.
- **ViewModel**: Nhận lệnh từ View, xử lý logic, gọi các Repository, và phát lại trạng thái (State) mới cho UI.
- **Repository**: Trung gian kết nối dữ liệu giữa Data cục bộ (Room) và Data đám mây (Firestore/API).

### 2.2 Nguyên lý Offline-First
- **Tốc độ tuyệt đối**: Mọi dữ liệu đọc/ghi đều đi thẳng vào cơ sở dữ liệu cục bộ **Room Database** (SQLite) trước. Người dùng có thể sử dụng mượt mà không cần 3G/Wifi.
- **Đồng bộ ngầm (Background Sync)**: Ứng dụng dùng WorkManager/Coroutines đẩy dữ liệu từ Room lên Firebase Firestore khi có mạng.

### 2.3 Nguyên lý Hybrid AI
Hệ thống AI không đẩy trực tiếp mọi thứ lên API (tránh tốn quota mạng và tiền):
- **Tầng 1 (Local AI Engine)**: Xử lý nội bộ trên máy bằng thuật toán regex và logic cục bộ để hiểu các lệnh đơn giản (ví dụ nhận diện "ăn tối 30k").
- **Tầng 2 (Gemini API - Function Calling)**: Nếu quá phức tạp, câu lệnh sẽ được gửi lên Gemini. Gemini sẽ trả về 1 lệnh gọi hàm (ví dụ: `addTransaction`, `addGroupSplitBill`). ViewModel hứng hàm này và hiện bảng (Card) cho người dùng xác nhận chứ không tự ý lưu.

### 2.4 Cảm biến bước chân (Foreground Service)
- Để Android không "giết" app khi đóng, app sử dụng một `Foreground Service` chạy liên tục, gắn kèm một thông báo (Notification) trên thanh trạng thái. Nó đăng ký lắng nghe sự kiện từ phần cứng `Sensor.TYPE_STEP_COUNTER` và cộng dồn bước chân ghi vào Room DB.

---

## 3. CODE NẰM Ở ĐÂU? (ĐỂ MỞ RA CHỈ CHO HỘI ĐỒNG XEM)

Khi bảo vệ, thầy cô yêu cầu *"Mở file xử lý AI lên tôi xem"* hoặc *"Database viết ở đâu?"*, bạn hãy nhớ cấu trúc này:

- **Logic kết nối AI (Gemini)**: `app/src/main/java/com/example/finfit/data/remote/GeminiService.kt`
- **Quét hình thức ăn bằng AI**: `app/src/main/java/com/example/finfit/health/api/vision/GeminiVisionProvider.kt`
- **Bộ đếm bước chân ngầm**: `app/src/main/java/com/example/finfit/health/repository/StepCounterService.kt`
- **Màn hình AI Chat**: `app/src/main/java/com/example/finfit/ui/assistant/AssistantScreen.kt` và `AssistantViewModel.kt`
- **Database (Bảng dữ liệu)**: `app/src/main/java/com/example/finfit/data/local/` (Các class DAO và AppDatabase)
- **Màn hình giao diện (Sức khỏe)**: Nằm trong `app/src/main/java/com/example/finfit/health/ui/`
- **Đọc tin nhắn ngân hàng tự động**: `app/src/main/java/com/example/finfit/service/BankNotificationListener.kt`

---

## 4. CÁC CÂU HỎI HỘI ĐỒNG THƯỜNG HỎI & CÁCH TRẢ LỜI

**Câu 1: Dự án của em dùng cấu trúc gì? Tại sao không dùng MVC mà lại dùng MVVM?**
> **Trả lời**: Em dùng kiến trúc MVVM kết hợp với Jetpack Compose. MVVM chia tách rõ ràng UI và Data, ViewModel giúp giữ lại dữ liệu khi thiết bị xoay màn hình. Luồng dữ liệu một chiều (Unidirectional Data Flow) của MVVM kết hợp với `StateFlow` trong Kotlin rất hoàn hảo để tự động cập nhật UI trong Compose mỗi khi database có thay đổi, mà không cần phải gọi các hàm cập nhật tay như MVC.

**Câu 2: Nếu người dùng không có Internet, app của em có lưu được dữ liệu không?**
> **Trả lời**: Dạ hoàn toàn được. Dự án thiết kế theo nguyên lý Offline-First. Toàn bộ thông tin thu/chi, nước uống, bước chân... đều được lưu trực tiếp vào cơ sở dữ liệu Room (SQLite) trên điện thoại. Khi nào điện thoại có mạng, hệ thống (WorkManager) sẽ tự động chạy ngầm và đồng bộ dữ liệu đó lên Firebase để sao lưu. Trừ các chức năng gọi API AI (như chat, quét ảnh), còn lại app dùng offline bình thường.

**Câu 3: Đếm bước chân em lấy dữ liệu từ đâu? Nếu tắt app đi (kill app) thì nó có đếm tiếp không?**
> **Trả lời**: Em lấy từ cảm biến vật lý `Sensor.TYPE_STEP_COUNTER` của điện thoại. Khi người dùng tắt hẳn app, hệ thống vẫn đếm được vì em khởi tạo một **Foreground Service** (Service chạy ngầm ưu tiên cao), nó gắn chặt với 1 Notification hiển thị trên điện thoại. Do đó Android OS sẽ không đóng Service này, đảm bảo bước chân không bị sót.

**Câu 4: Khi dùng AI Gemini, nếu AI sinh ra kết quả bậy bạ hoặc sai thì sao?**
> **Trả lời**: Để giải quyết rủi ro AI tự ý ghi sai dữ liệu tiền bạc của người dùng, hệ thống em dùng **Function Calling** kết hợp với **Confirmation Card**. Tức là AI không được quyền ghi trực tiếp vào Database. Khi AI hiểu ý người dùng, nó chỉ trả về một hàm kèm tham số dạng JSON. App em sẽ render một cái "Card Xác Nhận" (VD: Card Thu/Chi, Card Chia tiền). Người dùng xem lại bằng mắt thường, tự bấm nút "Lưu" thì tiền mới được cập nhật vào ví.

**Câu 5: Khi nhận dạng món ăn từ hình ảnh, em dùng mô hình tự train hay API? Tốc độ ra sao?**
> **Trả lời**: Em sử dụng Gemini Vision API để quét và nhận dạng thành phần món ăn vì nó có độ chính xác cao và hiểu được đa dạng món ăn Việt Nam hơn các mô hình tự train nhỏ gọn. Để giải quyết tốc độ và quota, em dùng model `gemini-2.5-flash` có tốc độ phản hồi cực nhanh, đồng thời kết quả lượng calo của món ăn đó sẽ được cache (lưu trữ) cục bộ, lần sau người dùng nhập cùng món đó sẽ lấy từ DB ra không cần gọi mạng nữa.

**Câu 6: Em có nói app có chức năng Split Bill (Chia tiền), nó hoạt động thế nào?**
> **Trả lời**: Dạ, khi người dùng (A) trả trước tiền bữa ăn cho 1 nhóm (B, C, D). A sẽ tạo 1 hóa đơn chia nhóm tổng là 400k. AI sẽ chia đều mỗi người 100k. App ghi nhận A chi 400k trong ngày hôm nay, đồng thời tự động tạo ra 3 khoản `DebtLoan` (khoản nợ) là B, C, D đang nợ A mỗi người 100k. Khi nào B, C, D trả lại, A chỉ cần tick vào đã trả, tiền sẽ tự cộng lại vào số dư.

**Câu 7: Tính năng tự động đọc thông báo SMS ngân hàng làm sao hoạt động được? Có vi phạm quyền riêng tư không?**
> **Trả lời**: Ứng dụng đăng ký `NotificationListenerService` của Android, yêu cầu người dùng **phải cấp quyền** cho phép đọc thông báo thì mới hoạt động được. App chỉ lọc các thông báo đến từ các package ngân hàng (như MoMo, MBBank, VCB...), trích xuất số tiền biến động qua biểu thức chính quy (Regex) `(+/- 10.000 VND)` và bỏ qua hoàn toàn nội dung cá nhân khác. Logic này hoàn toàn chạy offline trên máy (On-device) và không gửi tin nhắn về server, nên bảo mật quyền riêng tư của người dùng ạ.

---
*Chúc bạn tự tin và đạt điểm tối đa trong buổi bảo vệ đồ án!*

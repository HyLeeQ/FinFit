# 02. Kiến trúc & Công nghệ (Architecture & Tech Stack)

---

## 🏗 Kiến trúc Tổng thể (Overall Architecture)
Dự án FinFit tuân thủ chặt chẽ nguyên lý **Clean Architecture** kết hợp với **MVVM (Model-View-ViewModel)**. Điều này giúp tối ưu hóa khả năng mở rộng ứng dụng, phân tách rõ ràng trách nhiệm giữa Tầng điều khiển giao diện (UI) và Tầng xử lý Logic dữ liệu (Data).

Mô hình phân tầng cụ thể bao gồm:
1. **UI Layer (Presentation Layer):** Được triển khai hoàn toàn bằng **Jetpack Compose**. Quan sát dữ liệu một chiều (Unidirectional Data Flow) thông qua các biến trạng thái như `StateFlow` hay `LiveData` từ ViewModel. Không bao giờ trực tiếp thay đổi Data.
2. **Domain/ViewModel Layer:** ViewModel đóng vai trò như một cầu nối, chứa toàn bộ Business Logic. Nó ra lệnh cho Repository để lấy hoặc thay đổi dữ liệu, đồng thời phơi bày State (UiState) cho Compose UI render.
3. **Data Layer (Repository Layer):** Tuân thủ **Repository Pattern**, nơi đây quy định việc ứng dụng sẽ gọi dữ liệu từ Bộ nhớ cục bộ (Room) hay qua Mạng (Firebase). Hệ thống được thiết kế theo tiêu chí **"Single Source of Truth"** (Nguồn chân lý duy nhất): UI chỉ đọc kết quả từ Room Database; còn Firestore Firebase có trách nhiệm đồng bộ vào Room ở chế độ nền.

---

## 📂 Nguyên tắc Tổ chức Mã nguồn (Package Structure)
Cấu trúc Thư mục của dự án áp dụng mô hình **Package-by-Feature** (Nhóm theo Tính năng) thay vì Package-by-Layer. Sự cô lập này giúp các module nhỏ (ví dụ module đếm bước chân, module theo dõi nước) không dính chéo bug vào nhau.

## 🛠 Công nghệ Sử dụng (Tech Stack)

### 1. Ngôn Ngữ & UI (Language & Presentation)
* **Kotlin:** Ngôn ngữ cốt lõi, sử dụng triệt để *Coroutines* và *Flow* để xử lý đa luồng (Async Tasks) một cách hiệu quả mà không làm đứng giao diện.
* **Jetpack Compose:** Xây dựng toàn bộ giao diện bằng phương pháp Declarative UI với Material Design 3. Giúp animation mượt mà và loại bỏ hoàn toàn hệ thống XML rườm rà truyền thống.
* **Navigation Compose:** Điều khiển luồng di chuyển đa nền tảng, tích hợp tính năng tách/chuyển View nhánh bằng BottomNavigation bar nhanh gọn.

### 2. Xử Lý Bề Nền & Hardware (Background Services & Hardware)
* **Android Service (Foreground Service):** Luôn duy trì một quy trình có hiển thị thông báo (Notification) để lắng nghe sự kiện Đếm Bước Chân (`Sensor.TYPE_STEP_COUNTER`) từ chip định vị ngay cả khi xoá app.
* **WorkManager:** Xây dựng cơ chế lên kế hoạch đồng bộ (Periodic Sync) ngầm mỗi 30 phút. WorkManager cũng xử lý tác vụ "Silent Sync" cuối cùng khi người dùng lỡ tắt ứng dụng đi (OneTimeWorkRequest).

### 3. Lưu Trữ Dữ Tại (Local & Remote Storage)
* **Room Database:** Bộ nhớ SQLite cục bộ trên thiết bị, sử dụng mô hình DAO (Data Access Object). Nhờ Room phản hồi bằng Flow, bất cứ thao tác Thêm/Sửa/Xóa nào cũng lập tức chớp lên UI hiển thị mà không cần Load lại (Re-fetch) màn hình. Cùng với SQL thuần cung cấp tính năng "Cập nhật từng phần (Partial Update)" tinh vi.
* **Firebase Firestore & Authentication:** Xử trị việc đồng bộ dữ liệu chéo thiết bị định dạng NoSQL. Database Document chia chuẩn theo cấu trúc bảo mật `users/{userUID}/health/{date}`.

### 4. Thuật Toán Xử Lý & An Toàn Bộ Nhớ (Algorithms)
* **Offset Sync Strategy (Thuật toán bù trừ Data cảm biến):** 
Nhiệm vụ Đếm bước phụ thuộc vào sự chênh lệch (Offset) nhằm chống lại tác động thiết lập lại 0 độ rủi ro phần cứng mỗi khi máy Reset. Đảm bảo UI luôn hiển thị số chuỗi logic và Database chỉ có thăng cấp chứ không có giảm lùi.
* **Reactive Error Handling:** Quản lý mọi tác vụ Mạng và IO bằng `try/catch` đặt trong `Dispatchers.IO`, ngăn ngừa mọi nguy cơ treo App (ANR - Application Not Responding).

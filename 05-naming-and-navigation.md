# 05. Quy tắc Đặt tên & Dẫn hướng - Chống Xung đột (Naming & Navigation Controls)

Với mục tiêu phát triển hệ thống ngày càng đồ sộ thuộc lĩnh vực `Health`, rủi ro lớn là sự **Xung đột định danh (Name Collision)** và **Lạc luồng điều hướng (Navigation Spaghetti)** giữa các sub-module bên trong Sức Khoẻ (Ví dụ: Bước chân và Lượng nước). Tài liệu này ấn định nguyên tắc chuẩn hoá.

---

## 🚫 1. Chống Xung Đột Đặt Tên Lớp & Dữ Liệu (Naming Isolation)

* **Nguyên tắc "Tiền tố Logic" (Logic Prefixing):** Không được đặt tên trung lập cho những nhân tố chủ chốt. Mọi Entity, ViewModel, DAO, Repository bắt buộc phải gắn tiền tố định vị sự chức năng của chúng để nhìn phát phân biệt luôn.
  * ❌ *Sai:* `MainViewModel`, `ItemEntity`, `DashboardScreen`.
  * ✅ *Đúng:* `HealthViewModel` / `StepCounterViewModel`, `StepEntity` / `WaterEntity`, `HealthDashboard` / `WaterTrackerScreen`.
* **String Keys & SharedPrefs:** Khi truy xuất SharedPreferences hoặc Firestore, tiền tố là mạng sống. Tránh tình trạng biến lưu trữ đè lên nhau.
  * ✅ *Đúng ở Firestore:* `users/{uid}/health/{date}/steps` phân định rõ với `users/{uid}/health/{date}/water`.

---

## 🧭 2. Quy Chuẩn Điều Hướng (Navigation Standards)

Dự án sử dụng **Jetpack Navigation Compose**. Thay vì điều hướng bằng tên các màn hình (String literal), bắt buộc phải đi qua Trạm Kiểm Soát Tập Trung:

### 2.1 Tuyệt đối không Hardcode (No String Literals)
Mọi tên đường đi (Route) phải được kê khai dưới cấu trúc `object Routes` trong tệp `AppNavigation.kt` hoặc `HealthNavGraph.kt`. Cấm hành vi hardcode dạng `"health_screen"` lọt thỏm giữa View.
* *Định nghĩa đúng:*
```kotlin
object Routes {
    // Sức Khoẻ Routes
    const val HEALTH_DASHBOARD = "health_dashboard" 
    const val WATER_TRACKER = "water_tracker"
    const val STEP_COUNTER = "step_counter"
}
```

### 2.2 Quản Lý Trạng Thái Của Bottom Navigation Bar
Thanh điều hướng dưới cùng (Bottom Navigation) nhảy qua nhảy lại giữa các phân hệ nhỏ trong Sức Khoẻ phải tuân thủ nghiêm ngặt 3 cờ (Flags) trạng thái để hệ thống không sinh ra hàng tỷ màn hình bị đè lên nhau trong bộ nhớ (Back Stack Memory Leak):
1. **`launchSingleTop = true`**: Tránh việc tạo ra 2 màn hình giống hệt nhau khi ấn đúp.
2. **`restoreState = true`**: Trả lại chính xác ô người dùng đang gõ dở nếu họ qua Tab khác rồi quay lại.
3. **`popUpTo(navGraph.findStartDestination().id) { saveState = true }`**: Giải phóng toàn bộ tàn dư của lộ trình cũ trước khi nhảy Tab mới.

### 2.3 Phân Luồng Root Bằng Auth
Mũi tên định hướng đầu tiên khi mở App luôn thuộc về `AuthRepository` tại `MainActivity`.
Nếu chưa đăng nhập: Ném vào nhánh `Routes.AUTH`. Nếu đã có `currentUser`: Ném vào nhánh `Routes.MAIN`. `Routes.MAIN` lúc này mới chính là lớp phủ (Host) vẽ ra Bottom Bar và điều hướng các Sub-screen của dữ liệu Sức khoẻ.

---

## 🧱 3. Quy Tắc Giao Tiếp (Component Communication Rules)
Các sub-features của Health (như Quản lý Nước vs Quản lý Nhịp Tim, Bước chân) là các vùng logic chia sẻ.
* **Component Độc lập:** Các tính năng phải được tách lập trong khả năng có thể về mặt View để không cản trở nhau hoạt động.
* **Cầu Nối Trung Gian:** Nếu một trường hợp yêu cầu thông tin chung (Vd: UI kết hợp lượng calo đốt từ bước chân cộng với năng lượng nạp từ nước), cả hai phải nói chuyện với nhau thông qua một Interactor/Phễu Trung Gian đại diện tại `HealthRepository` hay `HealthViewModel` gốc, kết hợp các luồng `Flow` thành một trạng thái dùng chung (`HealthUiState`) để Compose tái cấu trúc khung nhìn.

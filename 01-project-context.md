# 01. Bối cảnh Dự án & Chức năng lõi (Project Context)

---

## 🚀 Tư Vấn & Định Hướng (Project Vision)
**FinFit** là một hệ sinh thái ứng dụng quản lý toàn diện dành cá nhân, mang sứ mệnh giải quyết một trong những nhu cầu thiết yếu nhất của đời sống hiện đại: **Sức khỏe (Health)**. Ứng dụng đem đến một không gian theo dõi liền mạch và mạnh mẽ bằng công nghệ Jetpack Compose hiện đại trên nền tảng Android. Ứng dụng hướng tới triết lý: "Cơ thể khoẻ mạnh là nền tảng của mọi thành công."

## 🛠 Nền Tảng Công Nghệ Cốt Lõi (Tech Stack)
* **Ngôn ngữ:** Kotlin
* **UI Framework:** Android Jetpack Compose (Material Design 3)
* **Kiến trúc (Architecture):** MVVM, Clean Architecture, Single Source of Truth
* **Database (Offline-first):** Room Database
* **Cloud & Sync, Auth:** Firebase (Firestore, Authentication)
* **Background Tasks:** WorkManager, Foreground Services
* **Hardware:** Android Sensor Manager (Type_Step_Counter, Type_Step_Detector)

---

## 🎯 Chức Năng Lõi (Core Features)

Dự án được xây dựng tập trung xoay quanh trung tâm **Module Sức Khoẻ (Health Module)**, sử dụng chung hệ thống Định tuyến (Navigation) và Xác thực (Authentication):

### Module Sức Khoẻ (Health Module)
Được tái cấu trúc theo mô hình "Offline-First", cho phép ghi nhận liên tục dữ liệu sinh học mà không cần kết nối mạng.
* **Theo dõi Số Bước Chân (Step Counter):** Tích hợp sâu vào Kernel của điệu thoại với hệ thống *Foreground Service* lắng nghe Cảm Biến Hardware. Sử dụng thuật toán *Offset* tự động bù đắp dữ liệu ngay cả khi Tắt nguồn Khởi động lại (Reboot) để số bước không bao giờ bị khuyết.
* **Theo Dõi Chuyển Hoá Cấu Trúc (Water, Calories & Sleep):** Quản lý toàn diện lượng Nước uống, Calo nạp vào (Food), Calo tiêu thụ và Thời gian ngủ hàng ngày.
* **Khoá Đồng Bộ Định Kỳ (Cloud Sync):** Cơ chế chạy nền ngầm tự động gom dữ liệu Room và đẩy lên Firebase mỗi 30 phút, hạn chế xung đột đè data (Merge/Partial Updates). 
* **Quản Lý Phiên Phi Trạng Thái (Silent/Manual Sync & Swipe-to-Kill):** Tự động bắt tín hiệu tắt App (Vuốt lên, ấn Back) để nén dữ liệu lên đám mây, đảm bảo trên Firebase luôn là phiên bản hoàn mỹ và mới nhất.

---

## 🛡 Đặc trưng Bảo mật & Quy Chuẩn Xử Lý (Protocol)
1. **Offline-First:** Tất cả hoạt động đều diễn ra trên `Room Database` ở RAM/Storage, đảm bảo độ trễ = 0s. 
2. **Merge Resolution:** Logic kết hợp dữ liệu tinh vi; dùng hàm SQL `MAX()` để bước chân không bị reset hoặc giảm lùi, cơ chế Partial Update để không xóa đè Nước/Calo khi đang đồng bộ Bước chân.
3. **Smart Notifications:** Thông báo ngầm Low-profile, cảnh báo khéo léo thông qua Snackbar/Popup khi hệ thống cần Đồng Bộ với Cloud. 
4. **Clean UI State:** Hệ thống Model hoàn thiện dựa trên Sealed Classes, giúp UI tách biệt hoàn toàn khỏi Logic xử lý data dưới nền.

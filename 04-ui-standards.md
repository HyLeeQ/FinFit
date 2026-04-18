# 04. Quy chuẩn Giao diện & Trải nghiệm UX (UI/UX Standards)

Tài liệu này hệ thống hóa cách tiếp cận của FinFit đối với giao diện ứng dụng và trải nghiệm người dùng, đảm bảo mọi thành phần trực quan đều thống nhất, thân thiện và đem lại cảm giác năng động chuẩn Health app.

---

## 🎨 1. Hệ thống Ngôn ngữ Thiết kế (Design Language)
Mọi thiết kế trong ứng dụng đều được áp dụng **Material Design 3 (M3)** thông qua nền tảng Jetpack Compose.
* **Component cơ sở:** Sử dụng `Scaffold` làm Wrapper cho tất cả các giao diện (`Screen`), hỗ trợ tích hợp có trật tự các TopBar, BottomBar, FloatingActionButton và SnackbarHost.
* **Màu sắc (Colors):** Token hóa toàn bộ bảng màu qua lớp `Theme`.
   * **Health Layout:** Ứng dụng chạy theo không gian màu `Health Mode` (Sức khoẻ - thiên về tone năng động, ví dụ Xanh lá/Xanh lơ mượt mà hoặc Trắng/Tím nhạt).
   * **Light/Dark Mode:** Tự động hỗ trợ tuỳ chỉnh giao diện Tối/Sáng theo cài đặt hệ thống. Không bao giờ hardcode mã màu tĩnh dạng Hex (`#FFFFFF`), bắt buộc sử dụng màu Context (`MaterialTheme.colorScheme.background`).
* **Kích thước & Căn chỉnh (Sizing & Typography):** 
   * Font chữ và Icon thống nhất kích cỡ đo lường bằng chuẩn `sp` (Scale-independent Pixels) và `dp` (Density-independent Pixels) để tự chống lại biến dạng trên các kích cỡ màn hình khác nhau.

---

## 🎛 2. Điểm Trạm Giao Tiếp (UI Feedback & Flow)
Một nguyên tắc cốt lõi của UX trong FinFit là *"Không bao giờ để người dùng bơ vơ sau khi hành động"*.

* **Nguyên lý Snackbar/Toast:** Bất kỳ thao tác tạo mới (Thêm Nước), xoá dữ liệu (Xoá Bước Chân ngày hôm nay) hay Đồng bộ Cloud đều phải kích hoạt phản hồi hiển thị bên dưới màn hình. `SnackbarHost` phải đính kèm trong `Scaffold`.
* **Empty States (Giao diện Rỗng):** Nếu người dùng chưa có Dữ liệu sức khỏe nào hoặc số Liệu trống, không được thể hiện trang trắng bóc. Phải luôn thiết kế một hình minh hoạ (Illustration/Icon) mờ đi và lời đề nghị: *"Bạn chưa cập nhật nước uống ngày hôm nay. Bấm nút Thêm nước để bắt đầu"*.
* **Xác nhận tác vụ phá huỷ:** Tuyệt đối không để xảy ra việc Xóa ngay lập tức cho các dữ liệu quan trọng như thống kê sức khỏe. Luôn sử dụng `AlertDialog` để hỏi "Bạn có chắc chắn muốn xoá...?"

---

## 💡 3. Các Xử lý Trải Nghiệm Thâm Sâu (Micro-Interactions)
Ngoài mỹ quan thì hành vi phản xạ của ứng dụng là cốt lõi chinh phục người dùng.

* **Chặn thoát đột ngột (Anti-Exit Trap):** 
Tại màn hình trang chủ (`MainScreen`/`Dashboard`), việc ấn nút "Trở về" (Back) được hệ thống dùng `BackHandler` chèn ép nhằm kích hoạt một Dialog xác nhận việc thoái lui. Người dùng sẽ được hỏi "Có muốn Đồng bộ lên Đám mây không?". Đây là tiêu chuẩn bảo vệ dữ liệu Cloud khỏi tình trạng bỏ rơi.
* **Swipe To Kill Sync:** Vuốt ném Ứng dụng ra khỏi khay đa nhiệm. Đây là kịch bản rất dễ gây Crash mạng và mất Data ở Module Đếm Bước Chân. FinFit yêu cầu mọi Service nền khi bị huỷ chớp nhoáng (Kill Task) đều phải ngầm tranh thủ nén 1 túi dữ liệu và Dispatch/Push lên đám mây trước khi ngủm hẳn.
* **Tối giản Nhập Dữ Liệu:** Nếu cần người dùng nhập Data, hạn chế bắt mở nhiều form màn hình liên tiếp, ưu tiên mở các `BottomSheet` hoặc `Dialogs` gọn nhẹ (ví dụ: màn hình Nạp Nước chỉ cần 1 popup chạm).

---

## 🚀 4. Phân Cấp Dữ Liệu (Progressive Disclosure)
* Ứng dụng chứa hàng tá chức năng nặng về Thống kê nên không nhồi nhét tất cả ra Home Screen. Tính năng cốt lõi (Vd: Tổng lượng Calories, Tổng Số Bước Chân hiện tại) được đặt phía trên (Top Surface).
* Các tính năng sâu hơn (Phân tích sức khoẻ chi tiết, Đồ thị bước chân) được đặt trong các thẻ lướt (Cards) có nút `See All` (Xem tất cả) hoặc các Tab Detail riêng biệt trỏ sang màn hình tiếp theo. Mọi biểu đồ nên có Loading State (Skeleton/CircularProgressBar) khi chờ fetch từ Realm/Room.

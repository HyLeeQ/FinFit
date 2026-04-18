# 03. Tiêu chuẩn Code Sạch (Clean Code Rules)

Đề cương bộ quy chuẩn lập trình nhằm duy trì dự án FinFit luôn ở trạng thái sạch sẽ, dễ bảo trì, dễ mở rộng và chặn đứng các nguy cơ thoái hoá mã nguồn (Code Decay). Mọi lập trình viên khi tham gia đều phải tuân thủ nghiêm ngặt các nguyên tắc dưới đây.

---

## 📝 1. Quy chuẩn Đặt tên (Naming Conventions)

Ngôn ngữ lập trình là tiếng Anh, chú thích (Comment/KDoc) dùng tiếng Việt.

* **Classes, Interfaces, Sealed Classes:** Sử dụng `PascalCase`.
  * *Ví dụ:* `HealthViewModel`, `StepCounterManager`, `HealthUiState`.
* **Functions, Methods, Variables:** Sử dụng `camelCase`.
  * *Ví dụ:* `updateStepData()`, `currentSensorValue`, `todaySteps`.
* **Constants & Enum values (Hằng số tĩnh):** Sử dụng `UPPER_SNAKE_CASE`. Đặt bên trong `companion object` hoặc file riêng.
  * *Ví dụ:* `NOTIFICATION_ID`, `CHANNEL_ID`, `TYPE_STEP_COUNTER`.
* **Database & File Naming:** Bảng (Table) trong SQLite sử dụng chữ thường, ngăn cách bằng dấu gạch dưới `snake_case`. Tên DB truyền tải đúng mục đích.
  * *Ví dụ:* `health_history`, `step_records`.
* **Compose UI Functions:** Các hàm tạo giao diện phải trả về Unit (`@Composable fun`) và đặt tên theo `PascalCase` giống như một Noun (Danh từ).
  * *Ví dụ:* `StepCounterScreen`, `MovementCard`, `WaterIntakeDialog`.

---

## 🏛 2. Unidirectional Data Flow (Dòng chảy Dữ liệu Một chiều)
Tuyệt đối không một UI Component nào (View/Composable) được phép nắm giữ và tự ý thay đổi dữ liệu cấu trúc cục bộ.
* **UI chỉ để Đọc (Read-only):** UI sẽ quan sát (Observe) `StateFlow` hoặc `LiveData` từ ViewModel. Khi cần thay đổi dữ liệu (vd: Bấm nút Thêm Nước), UI sẽ "phát ra 1 sự kiện gán" (Event Trigger) thông qua việc gọi hàm của ViewModel.
* **ViewModel điều phối:** ViewModel nhận lệnh, chuyển về dạng Asynchronous Tasks qua `viewModelScope.launch(Dispatchers.IO)`, tương tác với Repository, Repository báo cáo lại DAO, và Flow tự động update ngược lại lên UI.
* **Quy tắc Vùng Đóng Gói (Encapsulation):** Trong ViewModel, biến StateFlow phơi bày ra bên ngoài phải là `val` (chỉ đọc), State nội bộ phải là `private val _state = MutableStateFlow()`.
  * *Code chuẩn:* 
    ```kotlin
    private val _healthUiState = MutableStateFlow(HealthUiState())
    val healthUiState: StateFlow<HealthUiState> = _healthUiState.asStateFlow()
    ```

---

## 🛡 3. An Toàn Dữ Liệu & Chống Race Condition (Defensive Programming)
Module chứa các tính năng cực kỳ nhạy cảm và xung đột cao như: Đếm bước tự động + Nhập nước thủ công = Dễ gây đè bẹp hệ lưu trữ.
* **Không lưu đè Toàn bộ Row (No REPLACE All):** Không truyền một Object to bự để ghi đè mọi trường. Phải sử dụng **Cập Nhật Từng Phần (Partial Update SQL)**. Mọi thứ được độc lập thông qua DAO.
  * *Ví dụ:* Cập nhật nước gọi riêng `@Query("UPDATE health_history SET waterConsumed = ...")`.
* **Max-Rule Constraint:** Đối với bước chân đếm tiến lên, để tránh các event mạng hoặc event reset xoá ngược dữ liệu, luân phiên sử dụng Hàm `MAX()` trong SQL để bảo vệ dữ liệu vĩnh cửu.
  * *Ví dụ:* `steps = MAX(steps, :newSteps)`

---

## 🪡 4. Xử Lý Bất Đồng Bộ (Concurrency / Coroutines)
Không bao giờ được thực hiện các thao tác Disk I/O, Network hay Database Query trên `Main Thread` (Luồng giao diện).
* Mọi hành động thao tác với `WorkManager`, `Repository`, `Room` phải sử dụng `suspend fun` phối hợp `Dispatchers.IO`.
* Khi đang ở `Dispatchers.IO` mà cần gọi Callback về màn hình (UI Feedback kiểu Snackbar/Toast), bắt buộc phải đổi luồng thông qua ngữ cảnh tĩnh: `withContext(Dispatchers.Main) { onComplete() }`.

---

## 🧹 5. Bảo Trì Nguyên Trạng Dự Án (Code Maintenance)
Dự án cấm kị lưu giữ "Rác Kỹ Thuật":
1. Không lồng ép (Nest) quá 3 vòng `if`/`for` lùi vào trong. Sửa refactor bằng cách Return sớm (Early return).
2. Xoá mọi file Log debug (`hs_err_pid*, logcat.txt`) trước khi Push/Review Code.
3. Chặn mã Comment-out code chết (`// val oldStep = ...`). Nếu không dùng, hãy xoá hẳn. Git đã lưu lại lịch sử cho bạn.
4. Ưu tiên giữ lại các **KDoc** (`/** ... */`) để tài liệu hoá hàm chức năng, phục vụ cho AI Auto-detect và đồng nghiệp đọc về sau. Mọi tham số và kết quả trả ra ở Repository phải rõ ràng.
5. **No Single God Class:** File trên 500 dòng code bắt buộc phải xem xét để chẻ nhỏ (Helper Class / Utility Class).

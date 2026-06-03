# 📄 Tài Liệu Kỹ Thuật: Module Sức Khỏe (Health Module) - FinFit

Tài liệu này mô tả chi tiết kiến trúc, thuật toán, và logic xử lý của từng chức năng trong Module Sức khỏe của dự án FinFit. Được thiết kế tối ưu cho cả lập trình viên và các mô hình AI có thể đọc hiểu toàn bộ hệ thống ngay lập tức.

---

## 1. Tổng Quan Kiến Trúc (Architecture Overview)

Module Health áp dụng kiến trúc **MVVM (Model-View-ViewModel)** chuẩn của Android, kết hợp với mô hình **Offline-First** (ưu tiên ngoại tuyến) dựa trên **Room Database**.

*   **Model**: Định nghĩa cấu trúc dữ liệu (`HealthUiState`, `WaterScreenData`, `SleepLogUiItem`).
*   **Database (Room)**: Lưu trữ cục bộ thông qua `HealthDatabase` (đã nâng cấp lên Version 9), tối ưu hóa tốc độ truy vấn tính bằng mili-giây, không phụ thuộc kết nối mạng.
*   **Repository**: Làm nhiệm vụ trung gian xử lý nghiệp vụ dữ liệu (`HealthRepository`, `WaterRepository`, `SleepRepository`, `VisionAiRepository`).
*   **ViewModel**: Quản lý trạng thái giao diện UI (`HealthViewModel`, `FoodCameraViewModel`), kết nối dữ liệu từ Repository ra các `StateFlow` để Compose UI quan sát trực tiếp (Reactive Programming).
*   **UI (Jetpack Compose)**: Các màn hình giao diện phẳng được tối ưu hóa theo ngôn ngữ thiết kế tối giản, hiện đại "Luminescent Observer".

---

## 2. Các Chức Năng Chi Tiết (Detailed Features)

### 📌 2.1. Bộ Đếm Bước Chân (Step Counter)

*   **Cơ chế hoạt động**: Sử dụng cảm biến đếm bước vật lý của thiết bị (`Sensor.TYPE_STEP_COUNTER`).
*   **Foreground Service (`StepCounterService`)**: Chạy ngầm liên tục ngay cả khi người dùng tắt app hoặc khóa màn hình để đảm bảo không bỏ sót bước chân nào. Service được khai báo kiểu `foregroundServiceType="health"` tương thích Android 14+.
*   **Đồng bộ & Lưu trữ (`StepCounterManager`)**:
    *   Cảm biến đếm bước của Android trả về tổng số bước tích lũy từ lúc khởi động máy. 
    *   `StepCounterManager` tính toán lượng bước nhảy (delta) giữa các lần nhận sự kiện cảm biến và cộng dồn vào bản ghi ngày hiện tại (`YYYY-MM-DD`) trong bảng cơ sở dữ liệu `steps`.
    *   Tự động phát thông báo chúc mừng độc lập qua `SharedPreferences` khi người dùng chạm mốc 1.000 bước chân đầu tiên trong ngày.

---

### 📌 2.2. Theo Dõi Giấc Ngủ (Sleep Tracker)

*   **Giao diện Chọn giờ (`CircularSleepPicker`)**: Trình quay số 2D trực quan trên Canvas giúp chọn giờ đi ngủ (Bedtime) và giờ thức dậy (Wakeup) tiện lợi.
*   **Lưu trữ & Phục hồi**:
    *   Dữ liệu được lưu vào bảng `sleep_logs` với hai mốc thời gian dạng `Timestamp` (Long).
    *   Hỗ trợ cơ chế **kế thừa giấc ngủ qua đêm**: Nếu người dùng đi ngủ trước 12h đêm hôm trước và thức dậy vào sáng hôm sau, bản ghi giấc ngủ sẽ chỉ chính thức được tính điểm và đồng bộ vào ngày mới sau khi người dùng bấm nút "Thức dậy".
*   **Đặt Báo Thức Hệ Thống (System Alarm Intent)**:
    *   Khi người dùng tạo/sửa phiên ngủ và bấm **"Lưu & Đặt Báo Thức"**, FinFit sẽ gửi một tường minh `Intent(AlarmClock.ACTION_SET_ALARM)` đến hệ thống.
    *   Tham số `EXTRA_SKIP_UI = true` giúp bỏ qua giao diện thiết lập của hệ thống, đặt báo thức ngầm cực kỳ gọn gàng.
    *   Hệ thống sẽ sử dụng nhạc chuông báo thức mặc định đã được cài sẵn trên máy người dùng, đảm bảo tin cậy 100%.

---

### 📌 2.3. Quét Món Ăn & Dinh Dưỡng (Food Scanner & Nutrition)

*   **Quét hình ảnh bằng AI (`VisionAiRepository`)**:
    *   Người dùng chụp ảnh món ăn hoặc chọn từ thư viện.
    *   App sử dụng **Gemini Vision API** để phân tích hình ảnh và bóc tách các thành phần dinh dưỡng: Tên món, lượng Calo (kcal), Protein (g), Carbs (g), Fat (g).
*   **Cơ chế Dự phòng (Fallback & Cache)**:
    *   Nếu API Gemini gặp lỗi hoặc hết hạn Key, hệ thống tự động chuyển sang chế độ đối chiếu database cục bộ hoặc truy vấn dữ liệu từ Airtable API để trả kết quả.
*   **Cập nhật Dinh Dưỡng**:
    *   Sau khi xác nhận món ăn, lượng Calo nạp vào (`caloriesIn`) cùng các chỉ số đa lượng (Carbs, Protein, Fat) được lưu lại vào cơ sở dữ liệu và cộng dồn trực tiếp vào tổng lượng calo nạp trong ngày của màn hình chính.

---

### 📌 2.4. Theo Dõi Nước Uống & Cảnh Báo Caffeine (Water & Caffeine Tracker)

*   **Hệ số Hydrat hóa (Hydration Index)**:
    Không phải mọi loại nước đều có khả năng cấp nước như nhau. App áp dụng hệ số nhân thực tế cho từng loại thức uống:
    *   `Nước lọc`: Hệ số 1.0 (200ml nước lọc = 200ml lượng nước thực nhận).
    *   `Sữa/Nước ép`: Hệ số 0.9.
    *   `Trà`: Hệ số 0.85 (Có tính lợi tiểu nhẹ).
    *   `Cà phê`: Hệ số 0.8.
    *   `Soda`: Hệ số 0.75.
*   **Cảnh báo Caffeine thông minh**:
    *   Mỗi khi uống trà hoặc cà phê, hàm lượng caffeine sẽ được tích lũy vào hệ thống.
    *   **Ngưỡng 200mg (Cảnh báo chạy chữ - Marquee Ticker)**: Tự động xuất hiện thông điệp chạy từ phải sang trái. Tốc độ cuộn được tính toán động (72px/s) theo chiều dài văn bản giúp hiển thị rõ trên màn hình nhỏ (Redmi 10). Nội dung cá nhân hóa theo loại đồ uống uống nhiều nhất (Trà vs Cà phê).
    *   **Ngưỡng 400mg (Mức nguy hiểm)**: Hiển thị Dialog cảnh báo khẩn cấp một lần duy nhất trong phiên. Các icon cốc nước uống sẽ xuất hiện viền đỏ cảnh báo.
*   **Đồng bộ Logic xóa (Delete Log Sync)**:
    *   Khi người dùng xóa nhầm một bản ghi uống nước, hàm `deleteWaterLog` sẽ chạy lại lập tức `sumEffectiveHydrationMlByDate` để cập nhật cơ sở dữ liệu Room, hạ ngay lập tức màu sắc cảnh báo trên giao diện mà không bị trễ.

---

## 3. Hệ Thống Chấm Điểm Sức Khỏe Hàng Ngày (Daily Health Score)

Để tạo động lực sống khỏe cho người dùng, FinFit tích hợp bộ tiêu chí chấm điểm tự động, công bằng theo thang điểm **100**:

$$\text{Tổng điểm (100đ)} = \text{Dinh dưỡng (30đ)} + \text{Nước uống (20đ)} + \text{Giấc ngủ (25đ)} + \text{Vận động (25đ)}$$

### 📊 Tiêu chí chấm điểm chi tiết:
1.  **Dinh dưỡng (Tối đa 30 điểm)**:
    $$\text{Điểm} = \left( \frac{\text{Calo nạp vào}}{\text{Mục tiêu Calo}} \right) \times 30 \quad (\text{Giới hạn } [0, 30])$$
2.  **Nước uống (Tối đa 20 điểm)**:
    $$\text{Điểm} = \left( \frac{\text{Lượng nước đã uống (ml)}}{\text{Mục tiêu nước (ml)}} \right) \times 20 \quad (\text{Giới hạn } [0, 20])$$
3.  **Giấc ngủ (Tối đa 25 điểm)**:
    $$\text{Điểm} = \left( \frac{\text{Số giờ ngủ thực tế}}{8 \text{ giờ}} \right) \times 25 \quad (\text{Giới hạn } [0, 25])$$
4.  **Vận động (Tối đa 25 điểm)**:
    $$\text{Điểm} = \left( \frac{\text{Số bước chân thực tế}}{\text{Mục tiêu số bước}} \right) \times 25 \quad (\text{Giới hạn } [0, 25])$$

### 🔄 Cơ chế tự động Reset lúc 00:00:
Hệ thống **không xóa** dữ liệu cũ để phục vụ việc lưu trữ lịch sử lâu dài. Lúc 00:00 hàng ngày, biến truy vấn ngày hôm nay (`:today`) tự động chuyển sang ngày mới. Do đó, Room Database sẽ trả về giá trị $0$ cho các chỉ số bước chân, nước, calo nạp. Thuật toán chấm điểm nhận giá trị $0$ và tự động đưa điểm số hiển thị về $0/100$ một cách tự nhiên mà không cần chạy bất cứ tác vụ nền nào.

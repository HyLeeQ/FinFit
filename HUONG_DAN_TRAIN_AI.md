# 🧠 Hướng dẫn "Train" AI cho FinFit

> **Không cần kéo API, không cần code phức tạp.**  
> AI của FinFit hoạt động bằng cách đọc một đoạn **hướng dẫn (System Prompt)** mỗi khi bạn nhắn tin.  
> Bạn chỉ cần **chỉnh sửa đoạn đó** để thay đổi cách AI phản ứng.

---

## 📍 Nơi chỉnh sửa

**File:** `app/src/main/java/com/example/finfit/ui/assistant/AssistantViewModel.kt`

**Hàm cần tìm:** `buildSystemContext()` (khoảng dòng 448)

```kotlin
private fun buildSystemContext(): String {
    ...
    return "Bạn là Trợ lý FinFit. Thân thiện, bằng tiếng Việt.\n" +
           "..."  // ← CHỈNH SỬA PHẦN NÀY
}
```

---

## 🏗️ Cấu trúc System Prompt hiện tại

System prompt được chia thành 2 phần:

### Phần 1 — Dữ liệu tự động (không cần sửa)
AI tự lấy từ dữ liệu thực của user trong app:

| Biến | Ý nghĩa |
|------|---------|
| `totalBal` | Tổng số dư ví hiện tại |
| `debtsStr` | Danh sách nợ/cho vay hiện có |
| `goalsStr` | Các mục tiêu tiết kiệm |
| `habitContext` | Thói quen ăn uống, lịch trình của user |

### Phần 2 — Luật hành vi (BẠN CHỈNH SỬA PHẦN NÀY)
Các câu lệnh bạn viết để dạy AI cư xử đúng.

---

## ✍️ Cách viết luật (Rule)

Mỗi luật là **1 dòng** bắt đầu bằng `"- "`. Viết rõ ràng, ngắn gọn.

### 🔴 Khi nào dùng Tool (Công cụ)?
```
- Lệnh tạo giao dịch, nợ, ngân sách → Gọi Tool ngay lập tức thay vì phân tích dài dòng.
```
> Viết kiểu này để AI không nói suông mà phải bấm hành động ngay.

### 🟡 Khi nào KHÔNG dùng Tool?
```  
- NẾU người dùng chỉ kể chuyện hoặc hỏi thông tin → Không gọi Tool, chỉ trả lời bình thường.
- NẾU nội dung không liên quan tài chính → Không tạo giao dịch giả mạo.
```

### 🟢 Dạy AI xử lý tình huống cụ thể
```
- NẾU người dùng nói "Đã trả nợ cho X" → Tự tìm số tiền của X trong danh sách nợ, không hỏi lại.
- NẾU người dùng báo kế hoạch tương lai → Không dùng addTransaction (chỉ dành cho đã xảy ra).
```

---

## 🛠️ Danh sách Tool AI có thể gọi

Khi bạn muốn AI làm gì, hãy đề cập tên Tool tương ứng trong luật:

| Tên Tool | Làm gì |
|----------|--------|
| `addTransaction` | Tạo 1 giao dịch thu/chi |
| `addDebtLoan` | Ghi nợ hoặc cho vay |
| `addSavingsGoal` | Tạo mục tiêu tiết kiệm mới |
| `addBudget` | Thiết lập hạn mức chi tiêu |
| `addGroupSplitBill` | Chia tiền nhóm |
| `addAutoSchedule` | Lập lịch chi tiêu tự động hàng tuần |
| `addHeldFund` | Tạo quỹ giữ hộ |
| `updateUserHabit` | Ghi nhớ thói quen của user |
| `proposeWeeklyPlan` | Đề xuất kế hoạch chi tiêu cả tuần |
| `depositSavings` | Nạp tiền vào quỹ tiết kiệm từ ví |
| `withdrawSavings` | Rút tiền từ quỹ tiết kiệm ra ví hoặc qua quỹ khác |

---

## 📝 Ví dụ thực tế — Thêm luật mới

### Ví dụ 1: Dạy AI nhận ra khi user báo lương về

Thêm dòng này vào `buildSystemContext()`:

```kotlin
"- NẾU người dùng nói 'lương về', 'nhận lương', 'lĩnh lương' → " +
"Gọi addTransaction với type=INCOME, category='Lương', hỏi xác nhận số tiền nếu chưa có.\n" +
```

---

### Ví dụ 2: Dạy AI hỏi lại khi không rõ số tiền

```kotlin
"- NẾU người dùng không nói rõ số tiền → Hỏi lại 'Bạn đã chi bao nhiêu?' trước khi tạo giao dịch.\n" +
```

---

### Ví dụ 3: Cho AI biết quy tắc chi tiêu cá nhân

```kotlin
"- Tôi thường ăn sáng 20-30k, ăn trưa 35-50k, ăn tối 40-60k. Dùng mức này để tư vấn.\n" +
```

---

### Ví dụ 4: Thêm tính cách cho AI

```kotlin
"- Trả lời ngắn gọn, không quá 3 câu mỗi lần nếu không cần giải thích.\n" +
"- Thỉnh thoảng dùng emoji để thân thiện hơn.\n" +
```

---

## ⚡️ Quy trình chỉnh sửa

1. Mở file `AssistantViewModel.kt`
2. Tìm hàm `buildSystemContext()` (Ctrl+F → `buildSystemContext`)
3. Thêm/sửa luật trong chuỗi `return "..."`
4. Build lại app (`▶ Run`)
5. Vào tab Trợ lý AI và nhắn tin để kiểm tra

---

## ⚠️ Lưu ý quan trọng

- **Viết tiếng Việt** trong luật vì AI đang dùng tiếng Việt.
- **Mỗi luật kết thúc bằng `\n`** để xuống dòng đúng cách, ví dụ: `"- luật...\n" +`
- **Không được xóa** phần `habitContext` và các biến `totalBal`, `debtsStr`, `goalsStr` — AI cần chúng để biết thông tin tài chính hiện tại của user.
- **Luật cụ thể hơn thì tốt hơn.** Thay vì viết "Biết cách đọc nợ", hãy viết "NẾU người dùng nói 'X trả nợ mình' → gọi addTransaction type=INCOME".
- Luật **mâu thuẫn nhau** sẽ gây AI hành xử không nhất quán — kiểm tra kỹ trước khi thêm.

---

## 🔍 Debug — AI hiểu sai?

Nếu AI làm sai, kiểm tra theo thứ tự:

1. **Thêm Logcat filter:** `BankNoti` hoặc `AssistantViewModel` để xem log
2. **Log system prompt ra:** Thêm `Log.d("AI", buildSystemContext())` để xem AI đang được dạy gì
3. **Câu luật bạn viết có đủ rõ ràng không?** → Viết lại cụ thể hơn
4. **AI có đang gọi đúng Tool không?** → Check log `functionCalls`

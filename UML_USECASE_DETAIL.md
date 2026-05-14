# 📐 Biểu Đồ UML Chi Tiết Theo Use Case — FinFit
> Mỗi Use Case có đủ: Activity · Sequence · State · Communication
> Paste vào https://mermaid.live để render

---
# UC01: Đăng Ký / Đăng Nhập

## UC01 — Activity Diagram
```mermaid
flowchart TD
    START([Mở App]) --> A{Đã đăng nhập?}
    A -->|Có| MAIN([Vào MainScreen])
    A -->|Chưa| B[Hiển thị AuthScreen]
    B --> C{Chọn hành động}
    C -->|Đăng nhập| D[Nhập Email + Mật khẩu]
    C -->|Đăng ký| E[Nhập Email + Mật khẩu mới]
    D --> F[Gọi Firebase Auth\nsignInWithEmailAndPassword]
    E --> G[Gọi Firebase Auth\ncreateUserWithEmailAndPassword]
    F --> H{Kết quả}
    G --> H
    H -->|Thất bại| I[Hiển thị lỗi\nSnackbar]
    I --> B
    H -->|Thành công - Lần đầu| J[Tạo document\nFirestore /users/uid]
    H -->|Thành công - Cũ| K[Load wallet\ntừ Firestore]
    J --> L[Chuyển sang\nOnboardingScreen]
    K --> MAIN
    L --> M[SetupCurrencyScreen]
    M --> N[SetupCategoriesScreen]
    N --> MAIN
```

## UC01 — Sequence Diagram
```mermaid
sequenceDiagram
    actor U as 👤 Người Dùng
    participant S as AuthScreen
    participant VM as AuthViewModel
    participant FA as Firebase Auth
    participant FS as Firestore

    U->>S: Nhập email + password
    U->>S: Bấm "Đăng nhập"
    S->>VM: login(email, password)
    VM->>FA: signInWithEmailAndPassword()
    alt Sai thông tin
        FA-->>VM: AuthException
        VM-->>S: showError("Sai email hoặc mật khẩu")
    else Thành công
        FA-->>VM: FirebaseUser(uid)
        VM->>FS: getDocument("users/{uid}")
        FS-->>VM: UserData
        VM-->>S: navigateTo(MainScreen)
    end

    Note over U,FS: --- Luồng Đăng Ký ---
    U->>S: Bấm "Đăng ký"
    S->>VM: register(email, password)
    VM->>FA: createUserWithEmailAndPassword()
    FA-->>VM: FirebaseUser(uid) mới
    VM->>FS: createDocument("users/{uid}", defaultWallet)
    FS-->>VM: ✅ OK
    VM-->>S: navigateTo(OnboardingScreen)
```

## UC01 — State Diagram
```mermaid
stateDiagram-v2
    [*] --> KiemTraPhien : App khởi động
    KiemTraPhien --> ChuaDangNhap : Chưa có session
    KiemTraPhien --> DaDangNhap : Có Firebase session

    ChuaDangNhap --> DangXacThuc : Nhập thông tin + bấm Đăng nhập/Đăng ký
    DangXacThuc --> ChuaDangNhap : Xác thực thất bại
    DangXacThuc --> LanDauDangNhap : Tài khoản mới
    DangXacThuc --> DaDangNhap : Tài khoản cũ

    LanDauDangNhap --> CaiDatBanDau : Onboarding + Setup
    CaiDatBanDau --> DaDangNhap : Hoàn tất cài đặt

    DaDangNhap --> ManHinhChinh : Load dữ liệu xong
    ManHinhChinh --> ChuaDangNhap : Đăng xuất
    ManHinhChinh --> [*] : Tắt app
```

## UC01 — Communication Diagram
```mermaid
flowchart LR
    U(["👤 Người Dùng"])
    AS["AuthScreen"]
    VM["AuthViewModel"]
    FA["Firebase Auth"]
    FS["Firestore"]
    NAV["NavController"]

    U -- "1: nhập email+password" --> AS
    AS -- "2: login(email,pass)" --> VM
    VM -- "3: signIn()" --> FA
    FA -- "4: FirebaseUser" --> VM
    VM -- "5: getDoc(uid)" --> FS
    FS -- "6: UserData" --> VM
    VM -- "7: navigate(Main)" --> NAV
    NAV -- "8: hiển thị MainScreen" --> U
```

---
# UC02: Ghi Giao Dịch Qua AI

## UC02 — Activity Diagram
```mermaid
flowchart TD
    START([Mở AssistantScreen]) --> A[Nhập tin nhắn tiếng Việt]
    A --> B{LocalAI\nparse được?}
    B -->|Multi-tx ≥2| C[Hiện nhiều TransactionCard]
    B -->|Q&A đơn giản| D[Trả lời text ngay]
    B -->|Không xử lý được| E[Gọi Gemini API]
    E --> F{Gemini trả về}
    F -->|Text| D
    F -->|addTransaction| G[Hiện TransactionCard]
    C & G --> H{Chọn tài khoản\nnguồn?}
    H -->|Có nhiều ví| I[Chọn ví từ dropdown]
    H -->|Chỉ 1 ví| J[Tự động chọn]
    I & J --> K{Người dùng\nxác nhận?}
    K -->|❌ Huỷ| L[Xoá card]
    K -->|✅ Xác nhận| M[Trừ số dư ví]
    M --> N[Lưu giao dịch\nvào Firestore]
    N --> O[Hiện ✅ Thành công]
    O --> END([Kết thúc])
    L --> END
    D --> END
```

## UC02 — Sequence Diagram
```mermaid
sequenceDiagram
    actor U as 👤 Người Dùng
    participant S as AssistantScreen
    participant VM as AssistantViewModel
    participant LAI as LocalAIEngine
    participant GEM as GeminiService
    participant REPO as FirestoreRepository

    U->>S: "ăn tối 45k momo"
    S->>VM: sendMessage(text)
    VM->>LAI: parseMultiTransaction(text)
    LAI-->>VM: [ParsedTx(45k, Ăn uống)]
    VM->>S: show TransactionCard(45k, Momo)
    U->>S: bấm ✅ Xác nhận
    S->>VM: confirmTransaction(tx, wallet)
    VM->>REPO: saveUserWallet(balance-45k)
    VM->>REPO: addTransaction(tx)
    REPO-->>VM: ✅ saved
    VM->>S: show "✅ Đã ghi thành công"

    Note over VM,GEM: Nếu LocalAI không parse được
    VM->>GEM: getCompletion(context, history)
    GEM-->>VM: FunctionCall: addTransaction{amount,category}
    VM->>S: show TransactionCard
```

## UC02 — State Diagram
```mermaid
stateDiagram-v2
    [*] --> Nhap : Người dùng nhập text
    Nhap --> XuLyLocal : LocalAIEngine xử lý
    XuLyLocal --> HienCard : Parse thành công
    XuLyLocal --> GoiAPI : Không xử lý được
    GoiAPI --> HienCard : Gemini trả Function Call
    GoiAPI --> TraLoiText : Gemini trả Text
    TraLoiText --> [*]

    HienCard --> ChoXacNhan : Hiện TransactionCard
    ChoXacNhan --> DaHuy : Người dùng bấm Huỷ
    ChoXacNhan --> DangLuu : Người dùng bấm Xác nhận
    DangLuu --> DaLuu : Firestore write thành công
    DangLuu --> LoiLuu : Network error
    LoiLuu --> ChoXacNhan : Retry
    DaHuy --> [*]
    DaLuu --> [*]
```

## UC02 — Communication Diagram
```mermaid
flowchart LR
    U(["👤 Người Dùng"])
    AS["AssistantScreen"]
    VM["AssistantViewModel"]
    LAI["LocalAIEngine"]
    GEM["GeminiService"]
    REPO["FirestoreRepository"]
    DB[("Firestore")]

    U -- "1: nhắn 'ăn tối 45k'" --> AS
    AS -- "2: sendMessage(text)" --> VM
    VM -- "3: parse(text)" --> LAI
    LAI -- "4: ParsedTx[]" --> VM
    VM -- "5: showCard(tx)" --> AS
    AS -- "6: xác nhận" --> U
    U -- "7: bấm Confirm" --> AS
    AS -- "8: confirmTransaction()" --> VM
    VM -- "9: saveWallet()" --> REPO
    VM -- "10: addTx()" --> REPO
    REPO -- "11: write" --> DB
    DB -- "12: ✅" --> REPO
    REPO -- "13: callback" --> VM
    VM -- "14: showSuccess" --> AS

    VM -. "fallback nếu cần" .-> GEM
```

---
# UC03: Chia Bill Nhóm (Split Bill)

## UC03 — Activity Diagram
```mermaid
flowchart TD
    START([Bắt đầu]) --> A["Nhắn: 'chia lẩu 400k\nvới Nam, Tùng, Linh'"]
    A --> B[Gemini parse\naddGroupSplitBill]
    B --> C[Tính share/người\n400k ÷ 4 = 100k]
    C --> D[Hiện SplitBillCard\nvới danh sách thành viên]
    D --> E{Đánh dấu\nai đã trả?}
    E -->|Có người trả rồi| F[Cập nhật paidAmount]
    E -->|Tiếp tục| G{Xác nhận?}
    F --> G
    G -->|❌ Huỷ| END1([Kết thúc])
    G -->|✅ Xác nhận| H[Lưu tx GROUP_PREPAYMENT]
    H --> I{Ai chưa trả?}
    I -->|Có người chưa trả| J[Tự động tạo DebtLoan\ncho từng người]
    I -->|Tất cả đã trả| K[Cộng lại số tiền\nđã nhận ngay]
    J --> L[Cập nhật số dư ví\ntrừ tổng - cộng đã thu]
    K --> L
    L --> M[Thông báo\n✅ nhóm còn nợ Xk]
    M --> END2([Kết thúc])
```

## UC03 — Sequence Diagram
```mermaid
sequenceDiagram
    actor U as 👤 Người Dùng
    participant S as AssistantScreen
    participant VM as AssistantViewModel
    participant GEM as GeminiService
    participant REPO as FirestoreRepository

    U->>S: "chia lẩu 400k với Nam Tùng Linh"
    S->>VM: sendMessage(text)
    VM->>GEM: getCompletion(context)
    GEM-->>VM: FunctionCall: addGroupSplitBill\n{total:400k, participants:[Nam,Tùng,Linh]}
    VM->>S: show SplitBillCard
    U->>S: đánh dấu "Nam trả 100k"
    U->>S: bấm ✅ Xác nhận
    S->>VM: confirmSplitBill(participants)
    VM->>REPO: addTransaction(GROUP_PREPAYMENT)
    VM->>REPO: saveDebtLoan(Tùng, 100k, LOAN)
    VM->>REPO: saveDebtLoan(Linh, 100k, LOAN)
    VM->>REPO: saveUserWallet(balance - 400k + 100k)
    REPO-->>VM: ✅ saved
    VM->>S: "✅ Nhóm còn nợ bạn 200,000đ"
```

## UC03 — State Diagram
```mermaid
stateDiagram-v2
    [*] --> DeXuat : AI đề xuất SplitBillCard
    DeXuat --> DangXemXet : Người dùng review card
    DangXemXet --> DanhDauThanhToan : Đánh dấu ai đã trả
    DanhDauThanhToan --> DangXemXet : Xem lại
    DangXemXet --> DaHuy : Bấm Huỷ
    DangXemXet --> DaXacNhan : Bấm Xác nhận

    DaXacNhan --> TaoNoChuaTra : Có người chưa trả
    DaXacNhan --> HoanTat : Tất cả đã trả

    TaoNoChuaTra --> TheoDoiNo : DebtLoan được tạo
    TheoDoiNo --> ThuNo : Người nợ trả lại
    ThuNo --> TheoDoiNo : Còn người chưa trả
    ThuNo --> HoanTat : Tất cả đã trả đủ

    DaHuy --> [*]
    HoanTat --> [*]
```

## UC03 — Communication Diagram
```mermaid
flowchart LR
    U(["👤 Người Dùng"])
    AS["AssistantScreen"]
    VM["AssistantViewModel"]
    GEM["GeminiService"]
    REPO["FirestoreRepository"]
    DB[("Firestore")]

    U -- "1: mô tả chia bill" --> AS
    AS -- "2: sendMessage()" --> VM
    VM -- "3: getCompletion()" --> GEM
    GEM -- "4: FuncCall addGroupSplitBill" --> VM
    VM -- "5: showSplitBillCard" --> AS
    U -- "6: đánh dấu thanh toán" --> AS
    AS -- "7: confirmSplitBill()" --> VM
    VM -- "8: addTransaction()" --> REPO
    VM -- "9: saveDebtLoan() ×n" --> REPO
    VM -- "10: saveWallet()" --> REPO
    REPO -- "11: write all" --> DB
    DB -- "12: ✅" --> REPO
    VM -- "13: showResult()" --> AS
```

---
# UC04: Quản Lý Mục Tiêu Tiết Kiệm

## UC04 — Activity Diagram
```mermaid
flowchart TD
    START([Bắt đầu]) --> A{Chọn hành động}
    A -->|Tạo mới| B["Nhắn 'tạo tiết kiệm mua xe 50tr'"]
    B --> C[Gemini → addSavingsGoal]
    C --> D[Hiện SavingsCard\nchờ xác nhận]
    D --> E{Xác nhận?}
    E -->|Huỷ| END1([Kết thúc])
    E -->|OK| F[Lưu SavingsGoal\nvào Firestore]
    F --> G{Có autoSaving?}
    G -->|Có| H[WorkManager lên lịch\nnạp tiền hàng tuần]
    G -->|Không| I[Quản lý thủ công]

    A -->|Nạp tiền| J["Nhắn 'nạp 500k vào quỹ mua xe'"]
    J --> K[depositSavings Card]
    K --> L{Xác nhận?}
    L -->|OK| M[currentAmount += 500k\naccountBalance -= 500k]

    A -->|Rút tiền| N["Nhắn 'rút 200k từ quỹ du lịch'"]
    N --> O[withdrawSavings Card]
    O --> P{Đích đến}
    P -->|Về ví| Q[accountBalance += 200k]
    P -->|Sang quỹ khác| R[otherGoal.amount += 200k]

    H & I & M & Q & R --> S[Cập nhật Firestore]
    S --> END2([Kết thúc])
```

## UC04 — Sequence Diagram
```mermaid
sequenceDiagram
    actor U as 👤 Người Dùng
    participant S as SavingsGoalScreen
    participant VM as AssistantViewModel
    participant GEM as GeminiService
    participant REPO as FirestoreRepository
    participant WM as WorkManager

    U->>VM: "tạo tiết kiệm mua xe 50tr\ntự động 500k/tuần"
    VM->>GEM: getCompletion()
    GEM-->>VM: addSavingsGoal{name,target:50M,autoSaving:500k}
    VM->>S: show SavingsCard
    U->>VM: confirmSavingsGoal()
    VM->>REPO: saveSavingsGoal(goal)
    VM->>WM: scheduleWeeklyAutoSave(goalId, 500k)
    WM-->>VM: scheduled ✅

    Note over WM,REPO: Mỗi tuần WorkManager chạy
    WM->>REPO: depositToGoal(goalId, 500k)
    REPO->>REPO: goal.currentAmount += 500k
    REPO->>REPO: wallet.balance -= 500k

    U->>VM: "nạp thêm 1tr vào quỹ mua xe"
    VM->>GEM: getCompletion()
    GEM-->>VM: depositSavings{goalName,amount:1M}
    VM->>S: show DepositCard
    U->>VM: confirmDeposit()
    VM->>REPO: saveSavingsGoal(updated)
    VM->>REPO: saveUserWallet(updated)
```

## UC04 — State Diagram
```mermaid
stateDiagram-v2
    [*] --> TaoMoi : Người dùng / AI tạo

    TaoMoi --> ChoXacNhan : Hiện SavingsCard
    ChoXacNhan --> [*] : Huỷ
    ChoXacNhan --> DangTietKiem : Xác nhận

    state DangTietKiem {
        [*] --> ChuaDat
        ChuaDat --> NapTien : depositSavings()
        NapTien --> ChuaDat : amount < target
        NapTien --> DatMucTieu : amount >= target
        ChuaDat --> RutTien : withdrawSavings()
        RutTien --> ChuaDat
        DatMucTieu --> NapTien : Tiếp tục nạp
    }

    DangTietKiem --> TuDongNap : WorkManager\nhàng tuần
    TuDongNap --> DangTietKiem

    DangTietKiem --> QuaHan : Quá targetDate\nchưa đạt
    QuaHan --> DangTietKiem : Gia hạn

    DangTietKiem --> HoanThanh : Đánh dấu xong
    HoanThanh --> [*]
```

## UC04 — Communication Diagram
```mermaid
flowchart LR
    U(["👤 Người Dùng"])
    AS["AssistantScreen"]
    VM["AssistantViewModel"]
    GEM["GeminiService"]
    REPO["FirestoreRepository"]
    WM["WorkManager"]
    DB[("Firestore")]

    U -- "1: yêu cầu tạo quỹ" --> AS
    AS -- "2: sendMessage()" --> VM
    VM -- "3: getCompletion()" --> GEM
    GEM -- "4: addSavingsGoal{...}" --> VM
    VM -- "5: showSavingsCard" --> AS
    U -- "6: xác nhận" --> AS
    AS -- "7: confirmGoal()" --> VM
    VM -- "8: saveSavingsGoal()" --> REPO
    VM -- "9: scheduleAutoSave()" --> WM
    REPO -- "10: write(goal)" --> DB
    WM -- "11: hàng tuần nạp tiền" --> REPO
    REPO -- "12: update goal+wallet" --> DB
```

---
# UC05: Theo Dõi Sức Khỏe (Bước Chân)

## UC05 — Activity Diagram
```mermaid
flowchart TD
    START([App Khởi Động]) --> A[Khởi động\nForeground Service]
    A --> B[Đọc bước chân\nhiện tại từ Sensor]
    B --> C{Offset đã\ntính chưa?}
    C -->|Chưa - lần đầu| D[Lưu base offset\nvào Room]
    C -->|Rồi| E[Tính steps = current - offset]
    D --> E
    E --> F{Reboot phát hiện?}
    F -->|Sensor reset về 0| G[Reset offset mới]
    G --> E
    F -->|Bình thường| H[Cập nhật Room Database]
    H --> I{30 phút trôi qua?}
    I -->|Chưa| B
    I -->|Rồi| J[WorkManager\nPeriodic Sync]
    J --> K[Push dữ liệu\nlên Firebase]
    K --> L{Bước chân mới\n> Firebase?}
    L -->|Có| M[Update Firestore\ndùng MAX]
    L -->|Không| N[Giữ nguyên]
    M & N --> I
```

## UC05 — Sequence Diagram
```mermaid
sequenceDiagram
    participant SEN as Sensor Hardware
    participant FS as ForegroundService
    participant ROOM as Room Database
    participant WM as WorkManager
    participant FB as Firebase Firestore

    Note over SEN,FB: Vòng lặp liên tục (kể cả khi app đóng)
    SEN->>FS: onSensorChanged(stepCount)
    FS->>FS: steps = stepCount - offset
    FS->>ROOM: update HealthData(steps, date)
    ROOM-->>FS: ✅ saved locally

    Note over WM,FB: Mỗi 30 phút
    WM->>ROOM: read HealthData(today)
    ROOM-->>WM: HealthData{steps, water, calories}
    WM->>FB: getDocument("users/uid/health/date")
    FB-->>WM: existing{steps: oldValue}
    WM->>WM: mergedSteps = MAX(new, old)
    WM->>FB: update{steps: mergedSteps, ...partialUpdate}
    FB-->>WM: ✅ synced

    Note over SEN,FS: Khi máy reboot
    SEN->>FS: onSensorChanged(0) ← reset
    FS->>FS: Phát hiện jump về 0
    FS->>ROOM: saveNewOffset(0)
    FS->>FS: Tiếp tục đếm từ 0
```

## UC05 — State Diagram
```mermaid
stateDiagram-v2
    [*] --> DungDich : App cài đặt lần đầu
    DungDich --> DangChay : Cấp quyền + Bật Service

    state DangChay {
        [*] --> LangNgheSensor
        LangNgheSensor --> CapNhatRoom : Nhận step event
        CapNhatRoom --> LangNgheSensor : Tiếp tục lắng nghe
        CapNhatRoom --> TinhLaiOffset : Phát hiện Reboot
        TinhLaiOffset --> LangNgheSensor
    }

    DangChay --> DongBoCloud : WorkManager trigger\nmỗi 30 phút
    DongBoCloud --> DangChay : Sync xong

    DangChay --> TatApp : User vuốt tắt app
    TatApp --> SilentSync : OneTimeWorkRequest
    SilentSync --> [*] : Sync lần cuối xong

    DangChay --> MatKetNoi : Mất mạng
    MatKetNoi --> DangChay : Có mạng lại
    MatKetNoi --> DongBoCloud : Khi có mạng → sync bù
```

## UC05 — Communication Diagram
```mermaid
flowchart LR
    SEN(["📱 Step Sensor\nHardware"])
    FS["ForegroundService"]
    ROOM[("Room DB")]
    WM["WorkManager"]
    FB[("Firebase\nFirestore")]
    UI["HealthScreen"]

    SEN -- "1: onSensorChanged(n)" --> FS
    FS -- "2: tính steps=n-offset" --> FS
    FS -- "3: update(steps, date)" --> ROOM
    ROOM -- "4: Flow emit" --> UI
    UI -- "5: hiển thị số bước" --> UI

    WM -- "6: trigger mỗi 30 phút" --> ROOM
    ROOM -- "7: read HealthData" --> WM
    WM -- "8: getDoc(date)" --> FB
    FB -- "9: oldSteps" --> WM
    WM -- "10: MAX(new,old)" --> WM
    WM -- "11: partialUpdate()" --> FB

    style ROOM fill:#1a3a1a,color:#bbffb3
    style FB fill:#1a2a3a,color:#64b5f6
```

---
## 📋 Tổng Kết

| Use Case | Activity | Sequence | State | Communication |
|----------|:---:|:---:|:---:|:---:|
| UC01: Đăng ký / Đăng nhập | ✅ | ✅ | ✅ | ✅ |
| UC02: Ghi giao dịch qua AI | ✅ | ✅ | ✅ | ✅ |
| UC03: Chia bill nhóm | ✅ | ✅ | ✅ | ✅ |
| UC04: Quản lý tiết kiệm | ✅ | ✅ | ✅ | ✅ |
| UC05: Theo dõi bước chân | ✅ | ✅ | ✅ | ✅ |

**Tổng cộng: 20 biểu đồ UML**

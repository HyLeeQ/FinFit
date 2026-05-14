# 📊 Biểu Đồ Mermaid — Báo Cáo FinFit

> **Cách dùng:** Copy từng code block → Dán vào https://mermaid.live → Export PNG/SVG

---

## Biểu Đồ 1: Use Case Tổng Quan (dùng flowchart)

> ⚠️ Mermaid không có `usecaseDiagram`. Dùng `flowchart` để mô phỏng — kết quả tương đương.

```mermaid
flowchart LR
    %% Actors
    USER(["👤 Người Dùng"])
    ADMIN(["🔧 Firebase / Hệ Thống"])

    %% Finance Use Cases
    subgraph FIN["💰 Quản Lý Tài Chính"]
        UC1["Đăng ký / Đăng nhập"]
        UC2["Quản lý Ví & Tài khoản"]
        UC3["Ghi thu chi thủ công"]
        UC4["Chụp hóa đơn (OCR)"]
        UC5["Xem báo cáo & Biểu đồ"]
        UC6["Thiết lập Ngân sách"]
        UC7["Quản lý Tiết kiệm"]
        UC8["Ghi Nợ / Cho vay"]
        UC9["Chia Bill nhóm"]
        UC10["Quản lý Quỹ giữ hộ"]
    end

    %% AI Use Cases
    subgraph AI["🤖 Trợ Lý AI"]
        UC11["Chat bằng ngôn ngữ tự nhiên"]
        UC12["AI tạo giao dịch tự động"]
        UC13["AI lập kế hoạch tuần"]
        UC14["Đọc thông báo ngân hàng"]
    end

    %% Health Use Cases
    subgraph HLT["❤️ Sức Khỏe"]
        UC15["Theo dõi bước chân"]
        UC16["Ghi nước uống & Calo"]
        UC17["Theo dõi giấc ngủ"]
    end

    %% Backend
    subgraph BE["☁️ Firebase Backend"]
        UC18["Đồng bộ dữ liệu Cloud"]
        UC19["Xác thực người dùng"]
        UC20["Lưu trữ ảnh hóa đơn"]
    end

    %% Connections
    USER --> UC1
    USER --> UC2
    USER --> UC3
    USER --> UC4
    USER --> UC5
    USER --> UC6
    USER --> UC7
    USER --> UC8
    USER --> UC9
    USER --> UC10
    USER --> UC11
    USER --> UC15
    USER --> UC16
    USER --> UC17

    UC11 --> UC12
    UC11 --> UC13
    UC14 --> UC3

    ADMIN --> UC18
    ADMIN --> UC19
    ADMIN --> UC20

    UC3 -.->|include| UC18
    UC7 -.->|include| UC18
    UC15 -.->|include| UC18
```

---

## Biểu Đồ 2: Kiến Trúc Hệ Thống (System Architecture)

```mermaid
flowchart TB
    subgraph DEVICE["📱 Android Device"]
        direction TB
        subgraph UI["UI Layer — Jetpack Compose"]
            D[DashboardScreen]
            A[AssistantScreen]
            H[HealthScreen]
            O[Other Screens ×12]
        end

        subgraph VM["ViewModel Layer — Business Logic"]
            AVM[AssistantViewModel]
            FVM[FinanceViewModel]
            HVM[HealthViewModel]
        end

        subgraph DATA["Data Layer"]
            ROOM[(Room Database\nSQLite - Offline First)]
            LAI[LocalAIEngine\nOn-device AI]
        end

        subgraph SVC["Background Services"]
            FS[Foreground Service\nStep Counter]
            WM[WorkManager\nPeriodic Sync 30min]
            BNL[BankNotificationListener\nAuto-parse SMS]
        end
    end

    subgraph CLOUD["☁️ Cloud Services"]
        FS_DB[(Firebase Firestore\nNoSQL Cloud DB)]
        AUTH[Firebase Auth]
        STORAGE[Firebase Storage\nBill Images]
        GEMINI[Google Gemini 2.0 Flash\nFunction Calling API]
    end

    subgraph ML["🧠 On-Device ML"]
        OCR[ML Kit\nText Recognition OCR]
        SENSOR[Android Sensor Manager\nStep Counter Hardware]
    end

    %% Connections
    UI --> VM
    VM --> DATA
    VM --> LAI
    VM --> GEMINI

    DATA <-->|Sync| FS_DB
    UI <-->|Auth| AUTH
    UI <-->|Upload| STORAGE

    FS --> SENSOR
    FS --> ROOM
    WM --> FS_DB
    BNL --> VM

    OCR --> VM

    style ROOM fill:#1a3a1a,color:#bbffb3
    style FS_DB fill:#1a2a3a,color:#64b5f6
    style GEMINI fill:#2a1a3a,color:#ea73fb
    style LAI fill:#2a2a1a,color:#fff
```

---

## Biểu Đồ 3: Sequence Diagram — Luồng AI Assistant

```mermaid
sequenceDiagram
    actor User as 👤 Người Dùng
    participant UI as AssistantScreen
    participant VM as AssistantViewModel
    participant LAI as LocalAIEngine
    participant GEM as Gemini 2.0 Flash
    participant DB as Firestore

    User->>UI: Nhắn "ăn tối 45k"
    UI->>VM: sendMessage(text)

    VM->>LAI: parseMultiTransaction(text)
    LAI-->>VM: [ParsedTransaction(45k, Ăn uống)]

    VM->>UI: show TransactionCard (chờ xác nhận)
    Note over UI: Hiển thị Card với số tiền,\ndanh mục để User kiểm tra

    User->>UI: Bấm "Xác nhận"
    UI->>VM: confirmTransaction()
    VM->>DB: saveUserWallet() + addTransaction()
    DB-->>VM: ✅ Success
    VM->>UI: updateMessage → Card confirmed
    UI-->>User: "✅ Đã ghi giao dịch thành công!"

    Note over User,DB: ------- Trường hợp phức tạp (cần Gemini) -------

    User->>UI: Nhắn "lập kế hoạch tuần học quân sự"
    UI->>VM: sendMessage(text)
    VM->>LAI: tryAnswerLocally() → không xử lý được
    VM->>GEM: getCompletion(systemContext, history)
    GEM-->>VM: FunctionCall: proposeWeeklyPlan(itemsJson)
    VM->>UI: show WeeklyPlanCard
    User->>UI: Bấm "Xác nhận kế hoạch"
    UI->>VM: confirmWeeklyPlan()
    VM->>DB: saveWeeklyScheduleItem() ×7
    DB-->>VM: ✅ Success
    VM->>UI: "✅ Đã cập nhật kế hoạch tuần!"
```

---

## Biểu Đồ 4: Class Diagram — Data Models Chính

```mermaid
classDiagram
    class AppUserWallet {
        +String uid
        +List~AppBankAccount~ accounts
        +Double generalSavings
        +Double groupPrepaidAmount
        +List~HeldFundItem~ heldFunds
        +Boolean autoSaveWeeklySurplus
        +totalBalance() Double
        +totalHeldFunds() Double
    }

    class AppBankAccount {
        +String id
        +String bankCode
        +String name
        +Double amount
        +Boolean isHidden
    }

    class FinanceTransaction {
        +String id
        +Double amount
        +TransactionType type
        +String category
        +String accountId
        +Boolean isGroupPrepayment
        +List~TransactionParticipant~ participants
    }

    class TransactionParticipant {
        +String name
        +Double shareAmount
        +Double paidAmount
        +Boolean isPaid
    }

    class SavingsGoal {
        +String id
        +String goalName
        +Double targetAmount
        +Double currentAmount
        +Timestamp targetDate
        +Double autoSavingAmount
    }

    class DebtLoan {
        +String id
        +String personName
        +Double amount
        +DebtLoanType type
        +Timestamp dueDate
        +Boolean isPaid
    }

    class FinanceBudget {
        +String id
        +Double amount
        +BudgetPeriod period
        +String category
    }

    class UserHabit {
        +Double minMealCost
        +Double maxMealCost
        +List~RoutineSchedule~ routineSchedules
        +String generalNotes
    }

    class TransactionType {
        <<enumeration>>
        EXPENSE
        INCOME
        TRANSFER
        GROUP_PREPAYMENT
    }

    class DebtLoanType {
        <<enumeration>>
        DEBT
        LOAN
    }

    class BudgetPeriod {
        <<enumeration>>
        WEEKLY
        MONTHLY
    }

    AppUserWallet "1" --> "*" AppBankAccount : contains
    AppUserWallet "1" --> "*" HeldFundItem : holds
    FinanceTransaction "1" --> "*" TransactionParticipant : has
    FinanceTransaction --> TransactionType : type
    DebtLoan --> DebtLoanType : type
    FinanceBudget --> BudgetPeriod : period
```

---

## Biểu Đồ 5: Flowchart — Luồng Offline-First & Cloud Sync

```mermaid
flowchart TD
    A([App Khởi Động]) --> B{Có kết nối mạng?}

    B -->|Có| C[Pull data từ Firestore]
    B -->|Không| D[Load từ Room Database]

    C --> E[Merge vào Room DB]
    E --> F[UI hiển thị từ Room]
    D --> F

    F --> G{Người dùng thao tác}

    G -->|Ghi giao dịch| H[Write vào Room ngay lập tức]
    H --> I[UI update tức thì - 0ms lag]
    I --> J{WorkManager - 30 phút}

    G -->|Bước chân| K[Foreground Service lắng nghe Sensor]
    K --> L[Tính Offset bù trừ Reboot]
    L --> M[Write vào Room]
    M --> J

    J -->|Push| N[(Firebase Firestore)]
    N --> O{Conflict?}
    O -->|Bước chân| P[SQL MAX - không giảm lùi]
    O -->|Dữ liệu khác| Q[Partial Update - không ghi đè]
    P --> R([✅ Sync hoàn tất])
    Q --> R

    G -->|Tắt app| S[OneTimeWorkRequest\nSilent Sync lần cuối]
    S --> N

    style H fill:#1a3a1a,color:#bbffb3
    style I fill:#1a3a1a,color:#bbffb3
    style N fill:#1a2a3a,color:#64b5f6
    style R fill:#2a3a1a,color:#bbffb3
```

---

## Biểu Đồ 6: Flowchart — Hybrid AI Decision Tree

```mermaid
flowchart TD
    START([Người dùng gửi tin nhắn]) --> LAI1

    LAI1{LocalAI:\nparse multi-transaction?}
    LAI1 -->|≥ 2 giao dịch tìm thấy| CARD1[Hiện nhiều TransactionCard\nKhông cần API]

    LAI1 -->|Không đủ| LAI2{LocalAI:\ntrả lời Q&A đơn giản?}
    LAI2 -->|Số dư / Chi tháng /\nGiao dịch gần đây...| TEXT1[Trả lời text ngay\nKhông cần API]

    LAI2 -->|Câu hỏi phức tạp| GEM[Gọi Gemini 2.0 Flash API\nvới System Context đầy đủ]

    GEM --> RESP{Gemini trả về?}
    RESP -->|Function Call| FC{Xác định tool}

    FC -->|addTransaction| C1[TransactionCard]
    FC -->|addDebtLoan| C2[DebtCard]
    FC -->|addSavingsGoal| C3[SavingsCard]
    FC -->|addBudget| C4[BudgetCard]
    FC -->|addGroupSplitBill| C5[SplitBillCard]
    FC -->|addAutoSchedule| C6[ScheduleCard]
    FC -->|addHeldFund| C7[HeldFundCard]
    FC -->|updateUserHabit| C8[HabitUpdateCard]
    FC -->|proposeWeeklyPlan| C9[WeeklyPlanCard]
    FC -->|depositSavings| C10[DepositSavingsCard]
    FC -->|withdrawSavings| C11[WithdrawSavingsCard]

    C1 & C2 & C3 & C4 & C5 & C6 & C7 & C8 & C9 & C10 & C11 --> CONFIRM

    RESP -->|Text thuần| TEXT2[Hiển thị câu trả lời AI]

    CONFIRM{Người dùng\nbấm Xác nhận?}
    CONFIRM -->|✅ Có| SAVE[Lưu Firestore\nCập nhật số dư]
    CONFIRM -->|❌ Huỷ| DISCARD[Bỏ qua]

    style TEXT1 fill:#1a3a1a,color:#bbffb3
    style CARD1 fill:#1a3a1a,color:#bbffb3
    style GEM fill:#2a1a3a,color:#ea73fb
    style SAVE fill:#1a2a3a,color:#64b5f6
```

---

## 📋 Hướng Dẫn Export

1. Vào **https://mermaid.live**
2. Copy từng code block ở trên (phần trong dấu ` ```mermaid ... ``` `)
3. Dán vào ô Editor bên trái
4. Bấm **"Download SVG"** hoặc **"Download PNG"** ở góc phải

> **Tip:** SVG dùng cho Word/PowerPoint sẽ sắc nét hơn PNG khi zoom.

# 🗂️ 5 Biểu Đồ UML — Dự Án FinFit
> Paste từng code block vào **https://mermaid.live** → Export PNG/SVG ngay

---

## 1. Biểu Đồ Lớp — Class Diagram

```mermaid
classDiagram
    direction TB

    class AppUserWallet {
        +String uid
        +List~AppBankAccount~ accounts
        +Double generalSavings
        +Double groupPrepaidAmount
        +List~HeldFundItem~ heldFunds
        +Boolean autoSaveWeeklySurplus
        +Boolean isTotalBalanceHidden
        +totalBalance() Double
        +totalHeldFunds() Double
    }

    class AppBankAccount {
        +String id
        +String bankCode
        +String name
        +Double amount
        +Int colorIndex
        +Boolean isHidden
        +displayName() String
    }

    class HeldFundItem {
        +String id
        +String name
        +Double amount
    }

    class FinanceTransaction {
        +String id
        +Double amount
        +TransactionType type
        +String category
        +String note
        +PaymentMethod paymentMethod
        +Timestamp timestamp
        +Boolean isFromOCR
        +String imageUrl
        +String accountId
        +String toAccountId
        +Boolean isGroupPrepayment
        +Double personalAmount
        +Double groupAmount
        +Int participantCount
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
        +String iconEmoji
        +Long colorHex
        +Double autoSavingAmount
        +Timestamp lastAutoSavingAt
    }

    class DebtLoan {
        +String id
        +String personName
        +Double amount
        +DebtLoanType type
        +String note
        +Timestamp dueDate
        +Boolean isPaid
        +Timestamp createdAt
    }

    class FinanceBudget {
        +String id
        +Double amount
        +BudgetPeriod period
        +String category
        +Timestamp startDate
    }

    class SpendingScheduleItem {
        +String id
        +Int dayOfWeek
        +Double amount
        +String category
        +String note
        +Boolean isAutoApply
    }

    class UserHabit {
        +Double minMealCost
        +Double maxMealCost
        +List~RoutineSchedule~ routineSchedules
        +List~SpendingScheduleItem~ fixedCosts
        +String lastProactiveWeek
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

    class PaymentMethod {
        <<enumeration>>
        CASH
        BANKING
    }

    AppUserWallet "1" *-- "*" AppBankAccount : chứa
    AppUserWallet "1" *-- "*" HeldFundItem : giữ hộ
    FinanceTransaction "1" *-- "*" TransactionParticipant : gồm
    FinanceTransaction --> TransactionType : loại
    FinanceTransaction --> PaymentMethod : phương thức
    DebtLoan --> DebtLoanType : loại
    FinanceBudget --> BudgetPeriod : kỳ hạn
    UserHabit "1" *-- "*" SpendingScheduleItem : lịch cố định
```

---

## 2. Biểu Đồ Hoạt Động — Activity Diagram

> **Kịch bản:** Luồng người dùng ghi giao dịch qua AI Assistant

```mermaid
flowchart TD
    START([🟢 Bắt đầu]) --> A

    A[Mở màn hình\nAI Assistant] --> B[Nhập tin nhắn\nbằng tiếng Việt]

    B --> C{LocalAIEngine\nparse được không?}

    C -->|Nhận ra nhiều\ngiao dịch cùng lúc| D[Hiện nhiều\nTransactionCard]
    C -->|Câu hỏi đơn giản\nvề số dư, chi tiêu| E[Trả lời text\ntức thì - 0ms]
    C -->|Không xử lý được| F[Gọi Gemini\n2.0 Flash API]

    F --> G{Gemini\ntrả về gì?}

    G -->|Text trả lời| H[Hiển thị\ncâu trả lời]
    G -->|Function Call| I{Loại hành động}

    I -->|addTransaction| J1[TransactionCard]
    I -->|addDebtLoan| J2[DebtCard]
    I -->|addSavingsGoal| J3[SavingsCard]
    I -->|addBudget| J4[BudgetCard]
    I -->|addGroupSplitBill| J5[SplitBillCard]
    I -->|depositSavings| J6[DepositCard]
    I -->|withdrawSavings| J7[WithdrawCard]
    I -->|proposeWeeklyPlan| J8[WeeklyPlanCard]

    D & J1 & J2 & J3 & J4 & J5 & J6 & J7 & J8 --> K

    K{Người dùng\nxem xét Card}
    K -->|✅ Xác nhận| L[Lưu vào\nFirestore]
    K -->|❌ Huỷ bỏ| M[Discard Card]

    L --> N[Cập nhật số dư\nvà danh sách giao dịch]
    N --> O[Hiển thị thông báo\n✅ Thành công]

    E --> P{Tiếp tục\nchat?}
    H --> P
    O --> P
    M --> P

    P -->|Có| B
    P -->|Không| END([🔴 Kết thúc])
```

---

## 3. Biểu Đồ Trạng Thái — State Diagram

> **Kịch bản:** Vòng đời của một Mục Tiêu Tiết Kiệm (SavingsGoal)

```mermaid
stateDiagram-v2
    [*] --> Khởi_Tạo : Người dùng / AI tạo mục tiêu

    Khởi_Tạo --> Chờ_Xác_Nhận : Hiển thị SavingsCard trên UI

    Chờ_Xác_Nhận --> Đang_Tiết_Kiệm : Người dùng bấm Xác nhận\n→ Lưu Firestore
    Chờ_Xác_Nhận --> [*] : Người dùng bấm Huỷ

    state Đang_Tiết_Kiệm {
        [*] --> Chưa_Đạt
        Chưa_Đạt --> Nạp_Tiền : depositSavings()
        Nạp_Tiền --> Chưa_Đạt : currentAmount < targetAmount
        Nạp_Tiền --> Đã_Đạt_Mục_Tiêu : currentAmount >= targetAmount
        Chưa_Đạt --> Rút_Tiền : withdrawSavings()
        Rút_Tiền --> Chưa_Đạt : Còn tiền trong quỹ
        Đã_Đạt_Mục_Tiêu --> Nạp_Tiền : Tiếp tục nạp thêm
    }

    Đang_Tiết_Kiệm --> Tự_Động_Nạp : WorkManager\nchạy hàng tuần
    Tự_Động_Nạp --> Đang_Tiết_Kiệm : autoSavingAmount > 0\n→ Trừ ví, cộng quỹ

    Đang_Tiết_Kiệm --> Quá_Hạn : targetDate đã qua\nchưa đạt mục tiêu
    Quá_Hạn --> Đang_Tiết_Kiệm : Người dùng gia hạn

    Đang_Tiết_Kiệm --> Đã_Hoàn_Thành : Người dùng đánh dấu\nhoàn thành thủ công
    Đã_Đạt_Mục_Tiêu --> Đã_Hoàn_Thành : Người dùng xác nhận\nhoàn thành

    Đã_Hoàn_Thành --> [*] : Xoá khỏi danh sách\nhoặc lưu archive
```

---

## 4. Biểu Đồ Trình Tự — Sequence Diagram

> **Kịch bản:** Chia bill nhóm (Split Bill) qua AI

```mermaid
sequenceDiagram
    actor User as 👤 Người Dùng
    participant UI as AssistantScreen
    participant VM as AssistantViewModel
    participant LAI as LocalAIEngine
    participant GEM as Gemini 2.0 Flash
    participant REPO as FirestoreRepository
    participant DB as Firebase Firestore

    User->>UI: Nhắn "chia lẩu 400k\nvới Nam, Tùng, Linh"

    UI->>VM: sendMessage(text)
    VM->>LAI: parseMultiTransaction(text)
    LAI-->>VM: [] (không parse được)

    VM->>LAI: tryAnswerLocally(text, wallet, ...)
    LAI-->>VM: LocalAnswer(handled=false)

    Note over VM,GEM: Không xử lý được locally → gọi Gemini
    VM->>GEM: getCompletion(systemContext, history)

    Note over GEM: Phân tích: totalAmount=400k,\nparticipants=[Nam,Tùng,Linh],\nparticipantCount=4

    GEM-->>VM: FunctionCall: addGroupSplitBill\n{totalAmount:400000, participants:[Nam,Tùng,Linh]}

    VM->>UI: addLocalMessage(SplitBillCard)
    UI-->>User: Hiện SplitBillCard\n(100k/người × 4)

    User->>UI: Đánh dấu "Nam đã trả 100k"
    User->>UI: Bấm ✅ Xác nhận

    UI->>VM: confirmSplitBill(totalAmount, participants, ...)

    Note over VM: Tính toán:\npersonalShare = 100k\ngroupOwes = 200k (Tùng + Linh chưa trả)

    VM->>REPO: addTransaction(tx: GROUP_PREPAYMENT)
    REPO->>DB: POST /transactions/{id}
    DB-->>REPO: ✅ 200 OK

    VM->>REPO: saveDebtLoan(Tùng: 100k, LOAN)
    VM->>REPO: saveDebtLoan(Linh: 100k, LOAN)
    REPO->>DB: POST /debtLoans/...
    DB-->>REPO: ✅ 200 OK

    VM->>REPO: saveUserWallet(balance - 400k + 100k)
    REPO->>DB: PUT /wallet/{uid}
    DB-->>REPO: ✅ 200 OK

    VM->>UI: updateMessage → SplitBillCard (confirmed=true)
    VM->>UI: addMessage "✅ Đã chia bill!\nNhóm còn nợ bạn 200,000đ"
    UI-->>User: Hiển thị kết quả xác nhận
```

---

## 5. Biểu Đồ Giao Tiếp — Communication Diagram

> ⚠️ Mermaid không có `communicationDiagram`. Dùng `flowchart` với đánh số thứ tự thông điệp — chuẩn UML.
>
> **Kịch bản:** Các đối tượng giao tiếp khi xử lý giao dịch thu chi

```mermaid
flowchart LR
    U(["👤\nNgười Dùng"])
    AS["AssistantScreen\n(UI)"]
    VM["AssistantViewModel"]
    LAI["LocalAIEngine"]
    GEM["GeminiService"]
    REPO["FirestoreRepository"]
    DB[("Firebase\nFirestore")]
    ROOM[("Room\nDatabase")]

    U -- "1: nhắn tin(text)" --> AS
    AS -- "2: sendMessage(text)" --> VM
    VM -- "3: parseMultiTransaction(text)" --> LAI
    LAI -- "4: ParsedTransaction[]" --> VM
    VM -- "5: show TransactionCard" --> AS
    AS -- "6: bấm Xác nhận" --> U

    U -- "7: confirmTransaction()" --> AS
    AS -- "8: confirmTransaction(tx, wallet)" --> VM
    VM -- "9: saveUserWallet(wallet)" --> REPO
    VM -- "10: addTransaction(tx)" --> REPO
    REPO -- "11: write(wallet)" --> DB
    REPO -- "12: write(tx)" --> DB
    DB -- "13: ✅ success" --> REPO
    REPO -- "14: success callback" --> VM
    VM -- "15: updateMessage(confirmed)" --> AS
    VM -- "16: addMessage(✅ Thành công)" --> AS

    VM -. "sync ngầm" .-> ROOM
    ROOM -. "offline backup" .-> DB

    style U fill:#2a2a2a,color:#fff
    style DB fill:#1a2a3a,color:#64b5f6
    style ROOM fill:#1a3a1a,color:#bbffb3
    style GEM fill:#2a1a3a,color:#ea73fb
```

---

## 📋 Tóm Tắt & Hướng Dẫn

| # | Biểu đồ | Cú pháp Mermaid | Mô tả kịch bản |
|---|---------|-----------------|----------------|
| 1 | **Class Diagram** | `classDiagram` | Toàn bộ data model tài chính |
| 2 | **Activity Diagram** | `flowchart TD` | Luồng ghi giao dịch qua AI |
| 3 | **State Diagram** | `stateDiagram-v2` | Vòng đời Mục Tiêu Tiết Kiệm |
| 4 | **Sequence Diagram** | `sequenceDiagram` | Chia bill nhóm qua AI |
| 5 | **Communication Diagram** | `flowchart LR` | Giao tiếp giữa các đối tượng |

### Các bước export:
1. Vào **https://mermaid.live**
2. **Xoá hết** code cũ trong ô trái
3. **Paste** code block mong muốn vào
4. Biểu đồ tự render bên phải
5. Nhấn nút **"PNG"** hoặc **"SVG"** phía trên preview để tải về

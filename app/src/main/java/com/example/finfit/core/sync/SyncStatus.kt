package com.example.finfit.core.sync

enum class SyncStatus {
    SYNCED,          // Đã đồng bộ an toàn lên Cloud
    PENDING_SYNC,    // Đang chờ đẩy lên khi có mạng
    SYNC_FAILED,     // Đồng bộ thất bại (đang thử lại)
    PENDING_DELETE   // Đang chờ xóa mềm trên Cloud
}

enum class SyncEntityType {
    TRANSACTION,
    BUDGET,
    SAVINGS_GOAL,
    DEBT_LOAN,
    MEAL,
    USER_PREFERENCE
}

enum class SyncActionType {
    CREATE,
    UPDATE,
    SOFT_DELETE
}

# 家計簿アプリ データベース設計

## データベース構造 (SQLite)

### 1. ユーザーテーブル (User)
```
- user_id: INTEGER PRIMARY KEY AUTOINCREMENT
- name: TEXT
- password_hash: TEXT (オプション - アプリロック用)
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 2. 取引テーブル (Transaction)
```
- transaction_id: INTEGER PRIMARY KEY AUTOINCREMENT
- amount: REAL NOT NULL
- type: INTEGER NOT NULL (収入=1, 支出=0)
- date: DATE NOT NULL
- category_id: INTEGER NOT NULL REFERENCES Category(category_id)
- subcategory_id: INTEGER REFERENCES Subcategory(subcategory_id)
- memo: TEXT
- is_regular: INTEGER (0=通常, 1=定期的)
- regular_transaction_id: INTEGER REFERENCES RegularTransaction(regular_transaction_id)
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 3. カテゴリテーブル (Category)
```
- category_id: INTEGER PRIMARY KEY AUTOINCREMENT
- name: TEXT NOT NULL
- icon: TEXT (アイコン識別子)
- sort_order: INTEGER
- type: INTEGER (収入=1, 支出=0, 両方=2)
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 4. サブカテゴリテーブル (Subcategory)
```
- subcategory_id: INTEGER PRIMARY KEY AUTOINCREMENT
- category_id: INTEGER NOT NULL REFERENCES Category(category_id)
- name: TEXT NOT NULL
- sort_order: INTEGER
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 5. 予算テーブル (Budget)
```
- budget_id: INTEGER PRIMARY KEY AUTOINCREMENT
- category_id: INTEGER REFERENCES Category(category_id)
- amount: REAL NOT NULL
- period_type: INTEGER NOT NULL (月=0, 週=1, 年=2)
- start_date: DATE NOT NULL
- end_date: DATE NOT NULL
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 6. 定期的な取引テーブル (RegularTransaction)
```
- regular_transaction_id: INTEGER PRIMARY KEY AUTOINCREMENT
- name: TEXT
- amount: REAL NOT NULL
- type: INTEGER NOT NULL (収入=1, 支出=0)
- category_id: INTEGER NOT NULL REFERENCES Category(category_id)
- subcategory_id: INTEGER REFERENCES Subcategory(subcategory_id)
- frequency_type: INTEGER NOT NULL (日次=0, 週次=1, 月次=2, 年次=3)
- frequency_value: INTEGER NOT NULL (頻度値: 例えば月次で1は毎月、2は2ヶ月ごと)
- start_date: DATE NOT NULL
- end_date: DATE
- memo: TEXT
- last_executed: DATE
- next_execution: DATE
- is_active: BOOLEAN NOT NULL DEFAULT 1
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 7. 目標テーブル (Goal)
```
- goal_id: INTEGER PRIMARY KEY AUTOINCREMENT
- name: TEXT NOT NULL
- target_amount: REAL NOT NULL
- current_amount: REAL NOT NULL DEFAULT 0
- start_date: DATE NOT NULL
- end_date: DATE
- category_id: INTEGER REFERENCES Category(category_id)
- type: INTEGER (貯金=0, 支出削減=1)
- is_achieved: BOOLEAN DEFAULT 0
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 8. 設定テーブル (Settings)
```
- settings_id: INTEGER PRIMARY KEY AUTOINCREMENT
- key: TEXT NOT NULL UNIQUE
- value: TEXT
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

### 9. 通知テーブル (Notification)
```
- notification_id: INTEGER PRIMARY KEY AUTOINCREMENT
- title: TEXT NOT NULL
- message: TEXT NOT NULL
- type: INTEGER (予算警告=0, 定期支払い=1, 目標達成=2, その他=3)
- reference_id: INTEGER
- is_read: BOOLEAN DEFAULT 0
- scheduled_at: TIMESTAMP
- created_at: TIMESTAMP
```

## リレーションシップ

1. Transaction → Category (Many-to-One)
2. Transaction → Subcategory (Many-to-One)
3. Transaction → RegularTransaction (Many-to-One)
4. Subcategory → Category (Many-to-One)
5. Budget → Category (Many-to-One)
6. RegularTransaction → Category (Many-to-One)
7. RegularTransaction → Subcategory (Many-to-One)
8. Goal → Category (Many-to-One)
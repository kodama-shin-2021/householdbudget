# タスクチケット: データベース実装

## チケット番号
TASK-002

## 優先度
高

## 説明
家計簿アプリのデータベース層を実装し、Room Databaseを使用してSQLiteテーブルを設計・構築する。

## 詳細タスク
1. データベースエンティティの実装
   - Transaction (取引)
   - Category (カテゴリ)
   - Subcategory (サブカテゴリ)
   - Budget (予算)
   - RegularTransaction (定期的な取引)
   - Goal (目標)
   - Settings (設定)
   - Notification (通知)

2. DAOインターフェースの実装
   - TransactionDao
   - CategoryDao
   - SubcategoryDao
   - BudgetDao
   - RegularTransactionDao
   - GoalDao
   - SettingsDao
   - NotificationDao

3. データベースマイグレーション戦略の実装

4. サンプルデータの作成（初期カテゴリなど）

## 受け入れ基準
- すべてのエンティティとDAOが正しく定義されていること
- データベースマイグレーションが問題なく機能すること
- アプリ初回起動時に初期データが正しく挿入されること
- CRUD操作が各エンティティで正しく機能すること

## 見積り工数
3日
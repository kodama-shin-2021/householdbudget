# 家計簿アプリ アーキテクチャ設計

## 基本アーキテクチャ
- **アーキテクチャパターン**: MVVM (Model-View-ViewModel)
- **プログラミング言語**: Kotlin
- **最小 SDK バージョン**: API 24 (Android 7.0 Nougat)
- **ターゲット SDK バージョン**: 最新 (API 34/35)

## 主要コンポーネント

### 1. プレゼンテーション層 (UI)
- **Activity/Fragment**: 各画面のUIコンポーネント
  - MainActivity: アプリのメインエントリポイント
  - 各機能別Fragment: ダッシュボード、収支入力、カテゴリ管理など
- **Navigation Component**: 画面遷移の管理
- **ViewBinding**: レイアウトとの効率的なバインディング
- **Material Components**: モダンなUIコンポーネント

### 2. ビジネスロジック層
- **ViewModel**: UIとデータの橋渡し役、UIロジックの管理
  - DashboardViewModel
  - TransactionViewModel
  - CategoryViewModel
  - BudgetViewModel
  - GoalViewModel
  - ReportViewModel
  - SettingsViewModel
- **LiveData/StateFlow**: リアクティブなデータ表示とライフサイクル対応
- **Coroutines**: 非同期処理の管理

### 3. データ層
- **Repository**: データソース操作の抽象化
  - TransactionRepository
  - CategoryRepository
  - BudgetRepository
  - GoalRepository
  - UserRepository
  - SettingsRepository
- **Room Database**: SQLiteデータベースへのアクセス
  - DAO (Data Access Object): データベース操作のインターフェース
  - Entity: データベーステーブルに対応するクラス
- **DataStore/SharedPreferences**: アプリ設定の保存
- **WorkManager**: 定期的なタスク実行と通知管理
  - 定期的な取引の自動登録
  - 予算超過の通知
  - データバックアップ

## データフロー
1. ユーザーがUIで操作を実行
2. ViewModelがRepositoryを通じてデータを要求・更新
3. RepositoryがRoom Database (ローカルデータ) にアクセス
4. データ変更をLiveData/StateFlowで監視し、UIを自動更新

## 依存性注入
- **Hilt/Dagger**: コンポーネント間の依存性管理

## クラス構造

```
com.example.householdbudget/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── TransactionDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── ...
│   │   ├── entity/
│   │   │   ├── Transaction.kt
│   │   │   ├── Category.kt
│   │   │   ├── ...
│   ├── repository/
│   │   ├── TransactionRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── ...
│   ├── worker/
│   │   ├── RegularTransactionWorker.kt
│   │   ├── NotificationWorker.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
├── domain/
│   ├── model/
│   ├── usecase/
├── ui/
│   ├── MainActivity.kt
│   ├── dashboard/
│   │   ├── DashboardFragment.kt
│   │   ├── DashboardViewModel.kt
│   ├── transaction/
│   │   ├── TransactionFragment.kt
│   │   ├── TransactionViewModel.kt
│   │   ├── TransactionDetailFragment.kt
│   ├── category/
│   │   ├── CategoryFragment.kt
│   │   ├── CategoryViewModel.kt
│   │   ├── ...
│   ├── budget/
│   ├── report/
│   ├── goal/
│   ├── settings/
├── util/
│   ├── CurrencyFormatter.kt
│   ├── DateUtils.kt
│   ├── NotificationHelper.kt
├── MyApplication.kt
```

## サードパーティライブラリ
- **AndroidX Libraries**: アプリの基本構成
- **Room**: データベース操作
- **LiveData/Flow**: データストリーム管理
- **Coroutines**: 非同期プログラミング
- **WorkManager**: バックグラウンドタスク
- **Navigation Component**: 画面遷移
- **Hilt/Dagger**: 依存性注入
- **MPAndroidChart**: グラフ表示
- **Material Components**: UIコンポーネント
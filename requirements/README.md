# 家計簿アプリ要件定義プロジェクト

このリポジトリは、Androidの家計簿アプリ開発のための要件定義と設計ドキュメントを含んでいます。

## 主要ドキュメント

1. [要件定義サマリー](requirements_summary.md)
   - アプリの主な機能と要件の概要

2. [詳細要件定義](detailed_requirements.md)
   - 機能ごとの詳細な要件

3. [画面フロー図](screen_flow.md)
   - アプリの画面遷移と基本的なユーザーフロー

4. [データベース設計](database_design.md)
   - SQLiteデータベースのテーブル構造とリレーションシップ

5. [UIプロトタイプ](ui_prototypes.md)
   - 主要画面のワイヤーフレーム

6. [アーキテクチャ設計](architecture_design.md)
   - MVVMアーキテクチャと技術スタックの詳細

7. [実装計画](implementation_plan.md)
   - 開発フェーズとタイムライン

## 重点機能

- **カテゴリ管理**: 多階層カテゴリとカテゴリ別予算設定
- **定期的な収支管理**: 週次/月次/年次の自動記録機能
- **目標設定**: 貯金/支出削減目標の管理
- **取引履歴**: 金額でのソート機能

## 技術スタック

- **言語**: Kotlin
- **アーキテクチャ**: MVVM
- **データベース**: Room (SQLite)
- **非同期処理**: Coroutines, LiveData/Flow
- **バックグラウンド処理**: WorkManager
- **UI**: Material Design Components, MPAndroidChart

## 開発期間

約15週間 (4ヶ月) の開発計画
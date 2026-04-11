# 作業タスク — idea-to-action

2026-04-09 改訂。CLAUDE.md の設計方針に基づく。

> **注意：** このドキュメントで言う「タスク」は開発作業の単位です。
> アプリ内のデータモデル「Step（テーマに紐づく行動単位）」とは別概念です。

各タスク完了後にビルドが通ることを確認すること。

```bash
./scripts/doctor.sh
cd android-app && ./gradlew --stop && ./gradlew assembleDebug
```

---

## 旧ドキュメントとの関係

| ファイル | 状態 |
|---------|------|
| `docs/implementation-plan.md` | 旧設計ベース（5タブ・タイムライン前提）。参照非推奨 |
| `docs/ux-improvement-tasks.md` | 一部有効だが本ドキュメントに統合済み |
| `docs/gaps-and-todos.md` | バグ詳細の参照用として引き続き有効 |

---

## P1：ナビゲーション整理

### T-01：BottomNav を 5タブ → 3タブに変更

**ゴール：** 起動直後にホーム（今日やること）が表示される。タブは ホーム / アイデア / 設定 の3つ。

**変更対象：**
- `MainNavBar.kt` — タブ項目を3つに絞る
- `MainWindow.kt` — 初期表示を Today タブに変更
- 各 NavProvider の BottomBarItem 定義 — 振り返り・テーマを削除

**方針：**
- 振り返りタブを削除する（T-03 でホームに統合）
- テーマタブを削除し、設定画面からテーマ一覧へ到達できるようにする
- 削除したタブの画面コードは残してよい。NavProvider から BottomBarItem を外すだけでよい

**依存：** なし

---

### T-02：テーマ一覧を設定画面から到達できるようにする

**ゴール：** 設定画面にテーマ一覧へのリンクを置く。テーマ一覧・詳細への遷移は引き続き動く。

**変更対象：**
- `settings/SettingsScreen.kt` — 「テーマを管理する」リンク行を追加
- `settings/SettingsNavProvider.kt` または対応するナビゲーション — テーマ一覧への遷移を接続

**依存：** T-01（設定タブが残ることが前提）

---

## P2：振り返りのホーム統合

### T-03：振り返りカードをホーム画面に統合する

**ゴール：** 夜の時間帯（19:00以降）にホーム画面に振り返りセクションが現れる。独立した振り返りタブが不要になる。

**変更対象：**
- `today/TodayScreen.kt` — 時間帯条件で振り返りセクションを末尾に表示
- `today/TodayViewModel.kt` または `today/TodayViewState.kt` — 現在時刻を参照して振り返りモード判定フラグを追加
- 振り返りのコアロジック（`ReviewViewModel.kt` の MarkResult / CarryOver）をホームから呼べるように調整

**UI方針：**
- 19:00以降：ホームのリストの下部に「今日の振り返り」セクションが追加される
- 各行に3択ボタン（できた / 着手した / できなかった）をインライン表示
- 独立した振り返り画面（ReviewScreen）はそのまま残してよい。タブから外れるだけ

**依存：** T-01

---

## P3：ホームの使い勝手強化

### T-04：MorningSuggest を起動時に自動実行する

**ゴール：** APIキー設定済みかつ今日のスケジュールが空の場合、起動時に自動でAI候補提案が走る。

**変更対象：**
- `today/TodayViewModel.kt` — init ブロックで今日の件数を確認し、ゼロなら `MorningSuggestViewModel.suggest()` を呼ぶ
- `today/TodayScreen.kt` — 自動実行中はスピナーまたはインジケーターを表示する

**方針：**
- APIキー未設定時はスキップ（現在の MorningSuggestViewModel の動作そのまま）
- 自動実行は1日1回。すでに候補を表示済みなら再実行しない
- 手動ボタン（「AIに今日の候補を提案してもらう」）は引き続き残す

**依存：** なし（MorningSuggestViewModel は実装済み）

---

### T-05：空のホームから今日にステップを追加するショートカット

**ゴール：** ホームが空の状態から、テーマのステップを選んで今日に追加するまで 2タップで完結する。

**変更対象：**
- `today/TodayScreen.kt` — 空状態の中央に「+ やること追加」ボタンを追加
- BottomSheet を追加 — アクティブテーマのステップ一覧をチェックボックスで選択し、「今日に追加」ボタンで確定
- `today/TodayViewModel.kt` — 選択されたステップを ScheduledStep として今日の日付で保存するアクションを追加

**方針：**
- ステップ一覧はテーマ名でグループ化して表示する
- 追加済みのステップはグレーアウトして選択不可にする（重複追加を防ぐ）
- このショートカットは空状態のみに表示。すでに今日のリストがある場合はFABまたはメニューから到達する（当面は空状態のみで十分）

**依存：** なし

---

### T-06：sortOrder カラムを追加して並び順管理を実装する

**ゴール：** 今日のリストの順序をユーザーが変えられる。startTime による時刻管理に依存しない。

**変更対象：**
- `today/ScheduledStepEntity.kt` — `sortOrder: Int` カラムを追加
- `today/ScheduledStepDao.kt` — `ORDER BY sortOrder ASC` に変更。`updateSortOrder(id, order)` を追加
- `today/TodayViewModel.kt` — 並び替えアクションを追加
- `today/TodayScreen.kt` — 長押しドラッグ or 上下ボタンで並び替えUIを実装
- `db/IdeaToActionDatabase.kt` — migration を追加（version インクリメント）

**方針：**
- `startTime` / `duration` カラムはDBに残してよいが、UIから露出しない
- 今日に追加した順に sortOrder を振る（末尾追加）
- ドラッグ並び替えは実装コストが高い場合、上下ボタンで代替してよい

**依存：** なし

---

## P4：体験の磨き込み

### T-07：starterAction をホームのリストに表示する

**ゴール：** 着手の入口（starterAction）を、詳細シートを開かなくてもリストから確認できる。

**変更対象：**
- `today/TodayScreen.kt` の `ScheduledStepRow` — starterAction がある場合、ステップタイトルの下に小さく表示する

**依存：** なし

---

### T-08：テーマ詳細に完了ステップ数を表示する

**ゴール：** テーマ詳細に「完了 X / 全 Y」の進捗が一行で表示される。

**変更対象：**
- `theme/ThemeDetailViewState.kt` — completedCount / totalCount フィールドを追加
- `theme/ThemeDetailViewModel.kt` — ステップ一覧から集計する
- `theme/ThemeDetailScreen.kt` — タイトル下に進捗テキストを表示

**依存：** なし

---

### T-09：REVIEW コーチングの提案ステップをテーマに追加できるようにする

**ゴール：** AI振り返り対話で提案されたステップを、ワンタップでテーマに追加できる。

**変更対象：**
- `coaching/ReviewCoachingScreen.kt` または `coaching/ReviewCoachingViewModel.kt` — 対話完了後に提案ステップを抽出し「追加する」確認UIを表示
- `step/StepDao.kt` — insert メソッドの確認（既存で十分なはず）

**方針：**
- AI応答末尾の「【提案ステップ】」フォーマットをパースしてステップ候補を取り出す
- 「追加する / スキップ / 編集してから追加」の3択で確認
- 追加先テーマはレビュー対象テーマを前提とする

**依存：** なし（ReviewCoachingViewModel は実装済み）

---

### T-10：G-4 着手済み未完了ステップの翌日自動持ち越し

**ゴール：** 前日に `started=true, done=false` だったステップが、翌朝起動時に自動で翌日リストに追加される。

**変更対象：**
- `today/TodayViewModel.kt` — init で前日の未完了・着手済みステップを確認し、今日の ScheduledStep として追加する処理を追加

**方針：**
- 重複追加しない（すでに今日にスケジュール済みなら追加しない）
- ユーザーへの通知は不要。起動時にサイレントで処理する

**依存：** なし

---

## 実施順の推奨

| タスク | 内容 | 優先度 | 規模 |
|--------|------|--------|------|
| T-01 | BottomNav 3タブ化 | P1・最高 | 小 |
| T-02 | 設定からテーマ一覧へのリンク | P1・最高 | 小 |
| T-03 | 振り返りをホームに統合 | P1・高 | 中 |
| T-04 | MorningSuggest 自動実行 | P2・高 | 小 |
| T-05 | ホームからステップ追加ショートカット | P2・高 | 中 |
| T-06 | sortOrder による並び順管理 | P2・中 | 中 |
| T-07 | starterAction をリストに表示 | P3・中 | 小 |
| T-08 | テーマ詳細の進捗表示 | P3・中 | 小 |
| T-09 | REVIEW 提案ステップの追加 | P4・低 | 中 |
| T-10 | 着手済みステップの自動持ち越し | P4・低 | 小 |

T-01 → T-02 → T-03 の順に進めると、ナビゲーション整理が先に完了してからホーム統合作業に入れる。
T-04・T-05・T-06 は T-01〜T-03 と独立しているため並行して着手可能。

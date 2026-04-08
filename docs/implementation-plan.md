# 実装計画 — idea-to-action

2026-04-08 改訂。新仕様・ギャップ分析・UX改善タスクを統合。
各ステップ完了後にビルドが通ることを確認すること。

```bash
./scripts/doctor.sh
cd android-app && ./gradlew --stop && ./gradlew assembleDebug
```

---

## 現状の整理

### 流用できるもの
- `inbox/` — 大部分再利用。アーカイブバグ修正が必要
- `coaching/ClaudeClient.kt` — API通信は再利用。システムプロンプトを差し替える
- `settings/` — APIキー管理。そのまま使う
- Hilt・Room・Navigation3 の基盤構成
- `theme/`, `step/`, `today/`, `review/` — 構造は再利用。各ステップで改修

### 既知の不具合（着手前に修正すること）
- **G-1**：Inbox エントリがテーマ化後もアーカイブされない（Step 1 で修正）

---

## Step 1：データモデル更新

**ゴール：** 新仕様に合ったエンティティがDBに存在し、ビルドが通る

### 変更内容

**ScheduledStepEntity に追加：**
```
notificationEnabled: Boolean = false
result: String?   // "done" | "started" | "not_done"（振り返り3択）
```

**CoachingMessageEntity の contextType を拡張：**
```
// 旧: launch | review
// 新: inbox_batch | morning_suggest | review | theme_focus
```

**InboxEntryEntity：**
- `themeId?: String` が未追加なら追加

**既知バグ修正（G-1）：**
- `InboxDetailViewModel.CreateTheme` 末尾で `inboxDao.archive(entryId)` を呼ぶ
- `CoachingViewModel.parseAndSaveTheme` 成功後に同様に archive する

**DB version インクリメント**（migration追加 or fallbackToDestructiveMigration）

---

## Step 2：起動・ナビゲーション修正

**ゴール：** 起動直後に「今日やること」が表示される。タブ構成が新仕様に合う

### 変更内容

- `MainWindow.kt` または `MainNavBar.kt`：初期表示タブを `今日` に変更
- ボトムナビの順序を `今日 / Inbox / テーマ / 振り返り` に変更（現状確認して必要なら修正）

**参照：** UX Task 1

---

## Step 3：振り返り画面の刷新

**ゴール：** 3択ポチポチ（できた/着手した/できなかった）で評価でき、ステップ操作も完結できる

旧仕様の振り返りから大きく変わるため、画面を再構成する。

### 変更内容

**`review/ReviewScreen.kt`：**
- 今日のScheduledStep一覧を表示
- 各行に3択ボタン：`できた` / `着手した` / `できなかった`
- 任意メモ入力（一言でよい）
- 各行から「アーカイブ（完了扱い）」操作（スワイプまたはボタン）
- 「着手済み未完了」ステップは翌日への持ち越しを強調表示またはデフォルト選択

**`review/ReviewViewModel.kt`：**
- `MarkResult(scheduledStepId, result, memo?)` アクション追加
- `MarkStarted(scheduledStepId, memo?)` アクション追加（G-3対応）
- `ArchiveStep(stepId)` アクション追加（UX Task 2対応）
- `CarryOver` は引き続き維持

**`review/ReviewViewState.kt`：**
- result フィールドを持つ ScheduledStep の状態に更新

**テーマ詳細の実績ログ（G-2）：**
- `ThemeDetailScreen.kt` 下部に実績ログセクション追加（日付・ステップ名・result・メモ）
- `ScheduledStepDao` に `getByTheme(themeId)` または `observeByStep(stepId)` を追加（既存確認）
- `ThemeDetailViewState` に実績ログリスト追加

**参照：** G-2, G-3, G-4, UX Task 2

---

## Step 4：今日やること画面の改善

**ゴール：** ステップタイトルが前面に出る。翌日分の計画ができる

### 変更内容

**`today/TodayScreen.kt`：**
- 一覧行のレイアウト変更：ステップタイトルを大きく、テーマ名をサブテキストに
- 詳細（テーマのゴール等）はタップで展開するシートに移動
- 「今日 / 明日」切り替えタブを追加

**`today/TodayViewModel.kt`：**
- 表示対象日付をパラメータ化（今日 or 翌日）

**`today/StepScheduleSheet.kt`：**
- スケジュール追加時に対象日（今日 / 明日）を選択できるようにする

**参照：** UX Task 3, UX Task 7

---

## Step 5：通知機能

**ゴール：** 設定した開始時刻に着手を促す通知が届く

### 追加・変更内容

**`today/StepScheduleSheet.kt`：**
- 通知のON/OFFトグルを追加（`notificationEnabled`）

**新規追加：**
```
today/NotificationScheduler.kt   - AlarmManager でScheduledStepの通知をセット・キャンセル
today/StepNotificationReceiver.kt - BroadcastReceiver。通知を表示する
```

**`AndroidManifest.xml`：**
- `SCHEDULE_EXACT_ALARM` 権限追加
- `BroadcastReceiver` 登録
- `RECEIVE_BOOT_COMPLETED` 権限追加（再起動後の通知再登録）

**`today/TodayViewModel.kt`：**
- ScheduledStep 保存時に NotificationScheduler を呼ぶ

---

## Step 6：Inbox バッチ処理対応

**ゴール：** 複数エントリをまとめてテーマ化、または1エントリを複数テーマに分解できる

### 変更内容

**`inbox/InboxScreen.kt`：**
- 複数エントリの選択UI（チェックボックスまたは長押し選択モード）
- 選択後に「まとめてAIと整理する」ボタン

**`inbox/InboxDetailScreen.kt`：**
- 「テーマを作成する」ボタンを複数回押せるように変更
- エントリのアーカイブタイミングを「最初のテーマ作成時」→「手動アーカイブ」に変更

**`coaching/CoachingViewModel.kt`（contextType: inbox_batch）：**
- 複数InboxエントリのテキストをまとめてAIに渡す
- 対話完了時に複数の Theme + Step を一括生成して保存できるロジック

**システムプロンプト（inbox_batch）：**
```
あなたはアクションコーチです。
ユーザーの思いつきメモから、取り組みのゴールと最初のスモールステップを一緒に考えます。

進め方：
- 複数のメモがある場合、まとめて1つのテーマにするか、分けて複数のテーマにするかを提案する
- 質問は一度に一つだけ、短く
- ゴール・テーマの重さ・最初にできる小さな行動を引き出す

日本語で話してください。
```

**参照：** UX Task 5, app-spec Inbox仕様

---

## Step 7：AI朝の候補提案

**ゴール：** 毎朝AIが今日のステップ候補を提案し、ユーザーがポチポチ承認するだけで今日の計画ができる

### 追加・変更内容

**`today/TodayScreen.kt`：**
- 今日のScheduledStepがゼロのとき「AIに今日の候補を提案してもらう」ボタンを表示
- 提案結果をカード形式で表示し、各候補を「追加 / スキップ」でポチポチ選択できるUI

**新規追加：**
```
today/MorningSuggestViewModel.kt  - アクティブなテーマ・ステップ一覧をAIに渡し、候補リストを受け取る
today/MorningSuggestState.kt
```

**`coaching/ClaudeClient.kt`：**
- contextType: morning_suggest 用のシステムプロンプト対応

**システムプロンプト（morning_suggest）：**
```
あなたはアクションコーチです。
ユーザーのアクティブなテーマとステップ一覧を見て、今日着手するのに良さそなステップを2〜3個提案してください。

出力形式（JSON）：
[{"stepId": "...", "reason": "一言理由"}]

- 重さの軽いものを優先する
- 最近着手していないものを優先する
- 理由は短く、背中を押す言葉で

日本語で話してください。
```

---

## Step 8：AI振り返りフィードバック更新

**ゴール：** 3択評価とメモをAIに渡し、一言FBと次のステップ提案を受け取れる。提案をワンタップでテーマに追加できる

### 変更内容

**`review/ReviewScreen.kt`：**
- 全ステップ評価後に「AIにフィードバックをもらう」ボタン
- AI対話完了後に「提案されたステップを追加する」確認UI（追加 / スキップ / 編集してから追加）

**`coaching/CoachingViewModel.kt`（contextType: review）：**
- 今日のScheduledStep一覧（ステップタイトル・result・メモ）をまとめてAIに渡す
- 対話完了時に提案ステップをパースし、ThemeへのStep追加ロジックを呼ぶ

**システムプロンプト（review）：**
```
あなたはアクションコーチです。
ユーザーが今日取り組んだことを振り返り、次のステップを一緒に考えます。

ユーザーから今日のステップ評価（できた/着手した/できなかった）とメモが渡されます。

進め方：
- 着手できたことを短く認める
- 次にやると良さそうな小さな行動を1〜2個提案する
- ステップ提案は必ず以下のフォーマットで末尾に含める：
  【提案ステップ】
  - （ステップタイトル）

日本語で話してください。
```

**参照：** UX Task 6

---

## Step 9：AIテーマ絞り込みコーチング

**ゴール：** 停滞テーマや多すぎるテーマをAIが気づかせ、今集中すべきテーマを絞り込める

### 追加・変更内容

**`theme/ThemesScreen.kt`：**
- 「今フォーカスするテーマを整理する」ボタン（アクティブテーマが多いとき表示）

**新規追加：**
```
theme/ThemeFocusViewModel.kt  - アクティブテーマ一覧・最終着手日をAIに渡し、絞り込み提案を受け取る
```

**判定条件（AIに渡す情報）：**
- 各テーマの最終ScheduledStep日（直近の着手日）
- アクティブなステップ数

**システムプロンプト（theme_focus）：**
```
あなたはアクションコーチです。
ユーザーのアクティブなテーマ一覧と直近の着手状況を見て、今集中すべきテーマを一緒に考えます。

進め方：
- しばらく着手していないテーマについて理由を聞く
- 「今週これに集中してみては？」と1〜2個に絞る提案をする
- アーカイブを勧めるときは押しつけず「一旦お休みにする？」くらいの言い方で

日本語で話してください。
```

---

## 実施順と優先度

| Step | 内容 | 優先度 | 備考 |
|---|---|---|---|
| 1 | データモデル更新・バグ修正 | 最高 | 他すべての前提 |
| 2 | 起動・ナビゲーション修正 | 高 | 毎回の使い勝手に直結 |
| 3 | 振り返り刷新 | 高 | コアループの完成 |
| 4 | 今日やること改善 | 高 | メイン画面の品質 |
| 5 | 通知 | 中 | 着手促進のコア機能 |
| 6 | Inboxバッチ処理 | 中 | AI実装前に整える |
| 7 | AI朝の候補提案 | 中 | AI機能の中で最も日常的 |
| 8 | AI振り返りFB更新 | 中 | Step 3完了後に実装 |
| 9 | AIテーマ絞り込み | 低 | Step 7・8が安定してから |

Step 1〜4 が完了すれば、AIなしでもデイリーサイクルが回る最小形として使える。
Step 5〜9 はAI・通知機能の充実フェーズ。

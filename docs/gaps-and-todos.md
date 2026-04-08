# 機能ギャップと対応タスク — idea-to-action

仕様書・コードレビューにより洗い出した未実装・不足箇所。  
2026-04-08 時点のコードベースを基準とする。

---

## フロー上のバグ

### G-1：Inboxエントリがテーマ化後もアーカイブされない

**仕様**：「テーマ化されたエントリはアーカイブされる（削除ではなく参照可能に残す）」

**現状**：
- `InboxDetailViewModel.CreateTheme`：ThemeEntity を作成するが `inboxDao.archive()` を呼んでいない
- `CoachingViewModel.createThemeFromConversation`：テーマ+ステップ生成後も同様

**影響**：Inbox にエントリが残り続け、「整理された状態」にならない。コアフローの入口が破綻する。

**対応**：
- `InboxDetailViewModel.CreateTheme` の処理末尾で `inboxDao.archive(key.entryId)` を呼ぶ
- `CoachingViewModel.parseAndSaveTheme` の成功後（`NavigateToTheme` trigger 前）で同様に archive する

---

## コンセプト達成に必要な未実装

### G-2：テーマ詳細に実績ログがない

**仕様**：「テーマ詳細 - ゴール・ステップ一覧・実績ログ（予定 vs 実績）」

**現状**：`ThemeDetailScreen` にステップ一覧はあるが、`ScheduledStep` の着手・完了履歴（actualStartedAt / actualEndedAt / memo）が表示されていない。

**影響**：「やってみた結果を次の行動に活かす」というフィードバックループが機能しない。蓄積された記録が参照できない。

**対応**：
- `ScheduledStepDao` に `getByTheme(themeId)` または `getByStep(stepId)` を追加（既存の `observeByStep` を活用可）
- `ThemeDetailViewState` に実績ログリストを追加
- `ThemeDetailScreen` の下部にセクションとして表示（日付・ステップ名・着手/完了・メモ）

---

### G-3：振り返り画面から「着手した」をマークできない

**仕様**：「各ステップに対して『着手したか』を確認する（TaskChuteと同じく着手を重視）」

**現状**：`ReviewViewAction` は `MarkDone`（完了+アーカイブ）と `CarryOver`（持ち越し）のみ。今日ビューで着手マークを忘れた場合、振り返り画面では記録できない。

**影響**：「着手した」という記録がつけられず、すべてが「完了した」か「持ち越す」の二択になる。着手ファーストのコンセプトに反する。

**対応**：
- `ReviewViewAction` に `MarkStarted(scheduledStepId: String, memo: String?)` を追加
- `ReviewViewModel` で `scheduledStepDao.markStarted(...)` を呼ぶ
- `ReviewScreen` の詳細シートに「着手した」ボタンを追加（完了ボタンと並べる）

---

### G-4：着手済み未完了ステップの翌日持ち越しが手動のみ

**仕様**：「着手したが完了していないステップは翌日へ持ち越される（自動削除しない）」

**現状**：`ReviewViewModel.CarryOver` で翌日スケジュールを追加できるが、手動操作が必要。振り返りで CarryOver を押し忘れると、ステップは今日分に取り残される。

**影響**：「継続・探求を楽しむ」というコンセプトのループが途切れる。

**対応（段階的）**：
- 短期：振り返り画面で「着手済み・未完了」のステップに対して「明日に持ち越す」を強調表示 or デフォルト選択にする
- 将来：アプリ起動時に前日の `started=true, done=false` なステップを自動的に翌日スケジュールへ追加する処理を入れる

---

## 軽微な不整合

### G-5：ステップ数に上限がない

**仕様**：「ステップは少数。通常3〜5個が上限目安。意味のないTODOリストは着手しなくなる」

**現状**：件数制限なし。UI上の警告もなし。

**対応（任意）**：テーマ詳細でステップが5件を超えたら「ステップが増えすぎています」などの注意テキストを表示する（ハード制限はしない）

---

## 対応優先度

| ID | 内容 | 優先度 | 規模 |
|---|---|---|---|
| G-1 | Inboxアーカイブ漏れ | 高（バグ） | 小 |
| G-3 | 振り返りで「着手した」記録 | 高 | 小〜中 |
| G-2 | テーマ詳細の実績ログ | 中 | 中 |
| G-4 | 着手済みステップの持ち越し促進 | 中 | 小（短期対応） |
| G-5 | ステップ数の上限ガイド | 低 | 小 |

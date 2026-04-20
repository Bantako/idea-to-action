# Step A: UsageLog 実装 → Dogfooding

design-review-v2.md Section 12 の指示に従う。v2 UI 変更は dogfooding データが出るまで禁止。

---

## a1: UsageLogEntity / Dao / Repository を追加

- `UsageLogEntity(id, timestamp, event, metadata?)`
- `UsageLogDao`: insert + 期間指定 query
- `UsageLogRepository`: `record(event: String, metadata: String? = null)`
- AppDatabase version を +1（既存マイグレーションと競合しないよう確認）

## a2: 各操作に record() を埋め込む

以下の箇所で呼ぶ（ViewModel or Screen）:

| イベント文字列 | 呼び出しタイミング |
|---|---|
| `tab_opened:today` | Today タブが表示されたとき |
| `tab_opened:capture` | Capture タブが表示されたとき |
| `tab_opened:projects` | Projects タブが表示されたとき |
| `memo_captured` | Memo 追加完了時 |
| `memo_linked` | Memo をプロジェクトに紐付けたとき |
| `step_added` | Step 追加完了時 |
| `step_done` | Step 完了マーク時 |
| `log_recorded` | DailyLog 記録時 |
| `project_focused` | プロジェクトをフォーカスしたとき |

## a3: 開発者向け統計画面を追加

- Projects タブのどこかに隠しエントリ（例: タイトル長押し）で開く
- 表示内容: 過去7日間の各 event ごとのカウント、日別内訳
- CSV エクスポートボタン（外部ストレージ or Share sheet）

## a4: ビルド確認 & コミット

```bash
cd android-app && ./gradlew assembleDebug
```

コミットメッセージ例: `feat: UsageLog 追加（dogfooding 計測用）`

---

## a5: dogfooding 実施（開発作業ではない）

現状のアプリを **3日間連続で使う**。変更・機能追加は禁止。

確認する問い:
- Q1: キャプチャは1日何回発生するか？
- Q2: Today を自発的に開いたか？
- Q3: ステップの「完了」を押したくなる場面があったか？
- Q4: 活動を記録したいのに記録できなかった場面があったか？
- Q5: アプリよりメモ帳/Obsidian の方が早いと感じたか？

摩擦を感じた瞬間は Capture に投入するか、別メモに日時付きで記録する。

---

## a6: dogfooding レポート作成

3日後に `docs/dogfooding-report-YYYY-MM-DD.md` を作成。

内容:
- 使用ログ集計（event 別カウント・日別）
- Q1〜Q5 への回答
- 使わなかった日とその理由
- 最も使った / 使わなかった機能
- 次のステップ判定（ケース A / B / C / D）

**レポートが出てから v2 UI 変更（Today 書き換え等）を判断する。**

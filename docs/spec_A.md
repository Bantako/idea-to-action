# spec_A: Memo / Step / Project / DailyLog への移行設計

作成: 2026-04-16  
対象ブランチ: master  
ステータス: 実装前（設計確定）

---

## 1. このspecについて

設計レビュー（`design-review.md`）を受けて、現行の Node/Edge/Theme 設計から新設計へ移行する。
このドキュメントは**設計書かつ実装指示書**として機能する。

### 移行のゴール

| 現行 | 新設計 |
|------|--------|
| `NodeEntity`（アイデア＋行動を混在） | `MemoEntity`（アイデア）＋ `StepEntity`（行動）に分離 |
| `EdgeEntity`（DAG管理） | 廃止 |
| `ThemeEntity`（事後ラベル） | `ProjectEntity`（フォーカス・ゴール・ステータスを持つ） |
| なし | `DailyLogEntity`（軽い振り返り） |
| `Capture / Graph / Today` | `Today / Capture / Projects` |

### やらないこと（このspec内で）

- DAGの代替設計（エッジ管理は完全廃止）
- 旧テーブルのデータ完全移行（Node → Memo へのベストエフォートのみ）
- AI機能の全面書き換え（最小限の調整のみ）

---

## 2. 新しいデータモデル

### 2.1 エンティティ定義

#### MemoEntity

```kotlin
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val projectId: Long? = null,   // null = 未整理
    val createdAt: Long = System.currentTimeMillis(),
)
```

#### ProjectEntity

```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goal: String? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val focusedAt: Long? = null,   // null = フォーカス中でない
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ProjectStatus { ACTIVE, PAUSED, ARCHIVED }
```

#### StepEntity

```kotlin
@Entity(tableName = "steps")
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val status: StepStatus = StepStatus.PENDING,
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class StepStatus { PENDING, DONE }
```

#### DailyLogEntity

```kotlin
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,              // "2026-04-16"
    val stepId: Long? = null,      // ステップ完了時に自動生成
    val what: String,              // やったこと一行
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
```

### 2.2 DAO 定義（インターフェースのみ）

**MemoDao**
- `fun observeUnorganized(): Flow<List<MemoEntity>>`  → `projectId IS NULL`
- `fun observeByProject(projectId: Long): Flow<List<MemoEntity>>`
- `suspend fun insert(memo: MemoEntity): Long`
- `suspend fun update(memo: MemoEntity)`
- `suspend fun delete(memo: MemoEntity)`

**ProjectDao**
- `fun observeActive(): Flow<List<ProjectEntity>>`  → `status != ARCHIVED` ORDER BY `focusedAt DESC NULLS LAST`
- `fun observeFocused(): Flow<List<ProjectEntity>>`  → `focusedAt IS NOT NULL AND status = ACTIVE`
- `suspend fun insert(project: ProjectEntity): Long`
- `suspend fun update(project: ProjectEntity)`

**StepDao**
- `fun observeByProject(projectId: Long): Flow<List<StepEntity>>`  ORDER BY `sortOrder`
- `fun observePendingByProject(projectId: Long): Flow<List<StepEntity>>`
- `suspend fun insert(step: StepEntity): Long`
- `suspend fun update(step: StepEntity)`
- `suspend fun delete(step: StepEntity)`

**DailyLogDao**
- `fun observeByDate(date: String): Flow<List<DailyLogEntity>>`
- `fun observeByProject(projectId: Long): Flow<List<DailyLogEntity>>`
- `suspend fun insert(log: DailyLogEntity): Long`
- `suspend fun delete(log: DailyLogEntity)`

---

## 3. DB マイグレーション

現行バージョン: **2**  
新バージョン: **3**

### MIGRATION_2_3 の SQL

```sql
-- 新テーブル作成
CREATE TABLE IF NOT EXISTS memos (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    text TEXT NOT NULL,
    projectId INTEGER,
    createdAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    goal TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    focusedAt INTEGER,
    createdAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS steps (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    projectId INTEGER NOT NULL,
    title TEXT NOT NULL,
    sortOrder INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PENDING',
    doneAt INTEGER,
    createdAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS daily_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    date TEXT NOT NULL,
    stepId INTEGER,
    what TEXT NOT NULL,
    note TEXT,
    createdAt INTEGER NOT NULL
);

-- 旧 nodes → memos へのベストエフォート移行
-- IDEA / DEFERRED 状態のノードのみ（DONE/ABANDONED は移行しない）
INSERT INTO memos (id, text, createdAt)
SELECT id, title, createdAt FROM nodes
WHERE status IN ('IDEA', 'DEFERRED', 'READY');

-- 旧 themes → projects へのベストエフォート移行
INSERT INTO projects (id, title, createdAt)
SELECT id, name, createdAt FROM themes;
```

> **注意**: 旧テーブル（nodes / edges / themes）はすぐには削除しない。
> Phase 5 完了後、ビルドが安定したタイミングで MIGRATION_3_4 で削除する。

---

## 4. ファイル変更計画

### 4.1 新規作成するファイル

```
data/db/
  MemoEntity.kt
  MemoDao.kt
  ProjectEntity.kt
  ProjectStatus.kt
  ProjectDao.kt
  StepEntity.kt
  StepStatus.kt
  StepDao.kt
  DailyLogEntity.kt
  DailyLogDao.kt

domain/
  MemoRepository.kt
  ProjectRepository.kt
  StepRepository.kt
  DailyLogRepository.kt

feature/projects/
  ProjectsScreen.kt
  ProjectsViewModel.kt
  ProjectDetailScreen.kt
  ProjectDetailViewModel.kt
```

### 4.2 変更するファイル

| ファイル | 変更内容 |
|----------|----------|
| `data/db/AppDatabase.kt` | 新エンティティ追加・version 3・MIGRATION_2_3 追加 |
| `data/di/DatabaseModule.kt` | 新 DAO の provide 追加 |
| `data/ai/AiService.kt` | `suggestRelatedNodeIds` の引数を `MemoEntity` 向けに修正、不要メソッド削除 |
| `feature/capture/CaptureScreen.kt` | Node → Memo 差し替え、プロジェクト紐付け UI 追加 |
| `feature/capture/CaptureViewModel.kt` | NodeRepository → MemoRepository / ProjectRepository に差し替え |
| `feature/today/TodayScreen.kt` | フォーカスプロジェクトのステップ + DailyLog 表示に全面書き換え |
| `feature/today/TodayViewModel.kt` | ProjectRepository / StepRepository / DailyLogRepository に書き換え |
| `AppContent.kt` | タブ順変更（Today 先頭）・GraphScreen → ProjectsScreen に差し替え |

### 4.3 削除するファイル

```
data/db/EdgeEntity.kt
data/db/EdgeDao.kt
data/db/EdgeType.kt
data/db/NodeStatus.kt       ← Phase 5 以降（旧テーブル削除時）
data/db/NodeEntity.kt       ← Phase 5 以降
data/db/ThemeEntity.kt      ← Phase 5 以降
data/db/ThemeDao.kt         ← Phase 5 以降
domain/ThemeRepository.kt
feature/graph/GraphScreen.kt
feature/graph/GraphViewModel.kt
feature/graph/ThemeViewModel.kt
```

> EdgeEntity / EdgeDao / EdgeType は Phase 1 完了後すぐ削除してよい。
> NodeEntity / ThemeEntity は旧テーブルの削除（Phase 5）まで残す。

---

## 5. 実装フェーズ詳細

### Phase 1: データモデル移行

**目標**: 新しい Room エンティティ・DAO・マイグレーションを実装し、ビルドが通る状態にする。

#### 作業手順

1. `data/db/` に新エンティティ・DAO ファイルを作成する（上記定義のとおり）
2. `AppDatabase.kt` を更新:
   - `version = 3`
   - `entities` に4つの新エンティティを追加（旧エンティティも残す）
   - `MIGRATION_2_3` を実装
   - 新 DAO のabstract 関数を追加
   - `Converters` に `ProjectStatus` / `StepStatus` の変換を追加
3. `DatabaseModule.kt` に新 DAO の provide を追加
4. `EdgeEntity` / `EdgeDao` / `EdgeType` を削除
   - `AppDatabase` から `EdgeEntity` と `edgeDao()` を削除
   - コンパイルエラーになる箇所（`NodeRepository` など）を一時的に仮実装でしのぐ
5. ビルド確認 → コミット

#### 完了条件

- `./gradlew assembleDebug` が通る
- 新テーブルが作成されている（エミュレータで確認不要、SQL が正しければよい）

---

### Phase 2: Capture 画面の修正

**目標**: メモ投入が `MemoEntity` に保存され、未整理メモ一覧が表示される。

#### 作業手順

1. `domain/MemoRepository.kt` を作成:
   - `MemoDao` をラップ
   - `observeUnorganized()`, `create(text)`, `update()`, `delete()`, `linkToProject(memoId, projectId)` を実装
2. `CaptureViewModel` を書き換え:
   - `NodeRepository` → `MemoRepository` に差し替え
   - `State` を `CaptureState(input, memos, aiSuggestions, editTarget)` に更新
   - `handleSubmit()` で `memoRepository.create(text)` を呼ぶ
   - AI 機能: `suggestRelatedNodeIds` → `suggestRelatedProjectIds` に変える（後述）
   - エッジ追加ロジック（`repository.addEdge`）を削除
3. `CaptureScreen.kt` を書き換え:
   - `NodeEntity` 一覧 → `MemoEntity` 一覧
   - メモタップ時: 編集ダイアログの代わりに「プロジェクトに紐付ける」シートを出す
   - プロジェクト一覧から選択 → `memoRepository.linkToProject()` を呼ぶ
4. ビルド確認 → コミット

#### AI 機能の調整

`AiService` に新しいメソッドを追加する:

```kotlin
suspend fun suggestRelatedProjectIds(
    memoText: String,
    projects: List<ProjectEntity>,
): List<Long>
```

プロンプトのテンプレートは `suggestRelatedNodeIds` を流用してよい。
`suggestRelatedNodeIds` と `rankReadyNodes` と `suggestThemeNames` はこのフェーズで削除する。

---

### Phase 3: Projects 画面の新規実装

**目標**: プロジェクト一覧・詳細（ステップ管理）が動く。

#### ProjectRepository

```kotlin
// domain/ProjectRepository.kt
observeActive(): Flow<List<ProjectEntity>>
observeFocused(): Flow<List<ProjectEntity>>
create(title: String): Long
updateGoal(id: Long, goal: String)
setFocus(id: Long)          // focusedAt = now
clearFocus(id: Long)        // focusedAt = null
pause(id: Long)             // status = PAUSED
archive(id: Long)           // status = ARCHIVED
```

#### StepRepository

```kotlin
// domain/StepRepository.kt
observeByProject(projectId: Long): Flow<List<StepEntity>>
create(projectId: Long, title: String): Long
markDone(step: StepEntity)  // status = DONE, doneAt = now
delete(step: StepEntity)
```

#### ProjectsScreen の構成

```
[ProjectsScreen]
  - フォーカス中のプロジェクト（あれば上部に強調表示）
  - アクティブなプロジェクト一覧
  - 「新しいプロジェクトを作成」ボタン
  - 各プロジェクトタップ → ProjectDetailScreen

[ProjectDetailScreen]
  - タイトル・ゴール表示と編集
  - ステップ一覧（完了マークとスワイプ削除）
  - 「ステップを追加」入力欄
  - 「フォーカスする / フォーカスを外す」ボタン
  - 「休止する」「アーカイブする」ボタン
  - 紐付いたメモ一覧（読み取り専用）
```

#### 作業手順

1. `domain/ProjectRepository.kt`, `domain/StepRepository.kt` を作成
2. `feature/projects/ProjectsViewModel.kt` を作成
3. `feature/projects/ProjectsScreen.kt` を作成（シンプルなリストで十分）
4. `feature/projects/ProjectDetailViewModel.kt` を作成
5. `feature/projects/ProjectDetailScreen.kt` を作成
6. `AppContent.kt` のタブ順を変更:
   - `Today (0) | Capture (1) | Projects (2)`
   - `GraphScreen` → `ProjectsScreen` に差し替え
   - デフォルト選択タブを 0 (Today) に
7. ビルド確認 → コミット

---

### Phase 4: Today 画面の書き換え

**目標**: フォーカス中プロジェクトのステップ一覧と今日の DailyLog が表示され、ステップ完了時に DailyLog が自動生成される。

#### DailyLogRepository

```kotlin
// domain/DailyLogRepository.kt
observeToday(): Flow<List<DailyLogEntity>>   // date = today
observeByProject(projectId: Long): Flow<List<DailyLogEntity>>
createFromStep(step: StepEntity)             // what = step.title
createManual(what: String)                   // 自由記録
delete(log: DailyLogEntity)
```

#### TodayScreen の構成

```
[TodayScreen]
  // フォーカス中プロジェクトがある場合
  [フォーカス中プロジェクト名]
  ─ ステップ1 [PENDING] → タップで完了
  ─ ステップ2 [PENDING]

  [今日の記録]
  ─ ✓ ステップ3（完了済み） 10:23
  ─ ✓ 散歩した             09:00  ← 自由記録

  [+ 一言記録を追加]

  // フォーカス中プロジェクトがない場合
  「集中するプロジェクトを選んでください」
  [Projectsへのリンク]
```

#### 作業手順

1. `domain/DailyLogRepository.kt` を作成
2. `TodayViewModel.kt` を全面書き換え:
   - `ProjectRepository.observeFocused()` と `StepRepository` と `DailyLogRepository.observeToday()` を組み合わせる
   - `markStepDone(step)`:  `stepRepository.markDone(step)` → `dailyLogRepository.createFromStep(step)` をアトミックに呼ぶ
3. `TodayScreen.kt` を全面書き換え（上記構成）
4. ビルド確認 → コミット

---

### Phase 5: AI 機能の調整（最終仕上げ）

**目標**: AiService が新設計に対応し、旧テーブルを削除できる状態にする。

#### 作業手順

1. `AiService` の整理（Phase 2 で未対応なら）:
   - `suggestRelatedProjectIds` が実装済みであることを確認
   - 不要メソッドをすべて削除
2. `CaptureViewModel` の AI ログ表示を確認・調整
3. Today の AI ステップ提案（任意・優先度中）:
   - `AiService.rankPendingSteps(steps: List<StepEntity>): List<StepEntity>` を追加
   - `TodayViewModel` で呼び出す
4. 旧テーブルのクリーンアップ:
   - `NodeEntity` / `ThemeEntity` / `NodeStatus` を削除
   - `AppDatabase` の `entities` から旧エンティティを除外
   - `version = 4` にして `MIGRATION_3_4` を追加（旧テーブル DROP）
   - `NodeRepository` / `ThemeRepository` を削除
5. ビルド確認 → コミット

#### MIGRATION_3_4 の SQL

```sql
DROP TABLE IF EXISTS nodes;
DROP TABLE IF EXISTS edges;
DROP TABLE IF EXISTS themes;
```

---

## 6. 各フェーズの完了判定

| Phase | 完了条件 |
|-------|----------|
| 1 | `assembleDebug` が通る。新テーブルの SQL が正しい。Edge関連ファイルが削除されている |
| 2 | メモ投入 → `memos` テーブルに保存される。未整理メモ一覧が表示される |
| 3 | プロジェクト作成・ステップ追加・フォーカス設定が動く。AppContent のタブが新構成になっている |
| 4 | フォーカス中プロジェクトのステップをタップ → 完了 → DailyLog に記録される |
| 5 | 旧テーブルが削除されビルドが通る。AiService が新設計のみに対応している |

---

## 7. 注意事項

- 各フェーズ完了後に必ずコミットする（CLAUDE.md のコミットルールに従う）
- フェーズ間でビルドが壊れた状態を次フェーズに持ち越さない
- Phase 1〜2 は既存 UI を壊さないよう、旧テーブルを残しつつ新テーブルを追加する
- `NodeRepository` は Phase 2 で `MemoRepository` が動くまで残してよい
- UI の見た目は最低限でよい。Compose のシンプルなリストで進める

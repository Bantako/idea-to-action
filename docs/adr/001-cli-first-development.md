# ADR-0001: CLI中心で開発する

## Context
開発者は NixOS ユーザーでターミナル志向である。
Android Studio は現時点で安定運用できておらず、CLI と Nix devShell でビルド可能な構成が先に整った。

## Decision
通常開発は Nix devShell と CLI を前提とする。
Android Studio は必須要件にしない。

## Consequences
### Positive
- 環境再現性が高い
- エージェントに検証手順を渡しやすい
- GUI 依存の手順を避けられる

### Negative
- IDE 依存機能は使いにくい
- テンプレート導入時に CLI での調整が必要になる

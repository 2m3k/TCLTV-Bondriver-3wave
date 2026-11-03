# TCLTV-Bondriver 3波対応版（32S5400）

TCL G06シリーズ（32S5400 / MT5867）の **地上波・BS・110度CS（3波）** を、WindowsのBonDriver対応ソフト（TVTestなど）から受信するためのツールです。TVで別のアプリを使いながら、独立した2チューナーで異なる局を同時に受信できます。有料CSの視聴（デスクランブルはTV側のB-CAS処理）にも対応しています。

TV用セットアップアプリのパッケージ名は `app.tcltv3wave`、アプリ名は「32S5400 3波対応版」です。

## 特長

- 地デジ・BS・110度CSの3波すべてをTVTestから選局・視聴可能
- TVがどちらの系統を表示していても相互に選局できる（ベンダーDBの「ルート」切替を選局ごとに実施）
- 衛星選局パラメータは実機で検証済み
- TVTestのチャンネルスキャンが最後まで通る（未契約チャンネルも登録可能）
- TVアプリの自動EPG収集と視聴チューナーが競合しない
  - ※トレードオフとして、TVアプリ側の衛星EPG（XMLTV）は更新されなくなります（TVTest側には影響しません）

## 動作確認環境

- TV: TCL 32S5400（MT5867、Android 11、Magiskによるroot化済み）
- B-CASカード挿入済み、BS/CSアンテナ接続済み、TV側で地上波・BS・CSのスキャン済み
- PC: Windows + TVTest（BonDriver_TCLG06 / BonDriver_TCLG06_1）

他のTCL TV・ファームウェアでの動作は未検証です。

## 導入手順

ブートローダーアンロック（BLU）とMagiskの導入が済んでいれば、あとは以下の手順だけで使えるようになります。スクリーンショット付きの詳しい手順は `docs/SETUP.md` にもあります。

### 前提

- BLU済みでMagiskが動作している対応TV（導入方法は[Magisk bootリポジトリ](https://github.com/Long0823-1/TCL-G06-Magisk-Boot)を参照）
- TVにB-CASカードを挿入し、BS/CSアンテナを接続済み
- **TV本体のチャンネル設定で地上波・BS・CSのスキャンが完了していること**（このツールはTVのスキャン結果を利用します）
- Windows PCに[Platform-Tools（ADB）](https://developer.android.com/tools/releases/platform-tools)とTVTestを用意し、TVとPCを同じLANに接続

### 手順1: リリースZIPを展開し、APKをTVへ入れる

1. リリースZIPをすべて展開する
2. `TCL-BonDriver-Setup.exe` を起動し、ADB接続先（ネットワークADBなら `TVのIPアドレス:5555`、USB接続で機器が1台だけなら空欄）と、TVTestのフォルダーを指定する
3. 「1. APKをTVへ導入」を押すと、TV用APKがTVにインストールされて開きます（初回はTV側でADB接続の許可を求められる場合があります）

### 手順2: TV側でセットアップする

1. TVの「32S5400 3波対応版」で **「1. セットアップ」** を選ぶ
2. Magiskの許可要求が出たら、対象が **32S5400 3波対応版（`app.tcltv3wave`）** であることを確認して許可する（期間は「今後も」がおすすめ）
3. 完了したらAPKの **「2. テレビを再起動」** を選び、起動後1〜2分待つ

このセットアップで、配信デーモン本体の `libtsd.so` がTVの `/data/local/tmp/` に配置され、Magiskの起動スクリプト（post-fs-data）によってTV起動時に自動で読み込まれるようになります。

### 手順3: PCへドライバと局一覧を配置する

Windowsツールで **「2. 接続設定・局一覧を更新」** を押します。TVTestのフォルダーに、1系統目用 `BonDriver_TCLG06.dll` と2系統目用 `BonDriver_TCLG06_1.dll`、および接続設定（INI）が配置されます。

※ INIには機器固有のキーが含まれます。GitHubなどへのアップロードや第三者との共有はしないでください。

### 手順4: 配信テストとTVTestの設定

1. Windowsツールの **「3. 配信をテスト」** で受信を確認する
2. TVTestを起動し、チューナーとして `BonDriver_TCLG06.dll`（2系統目は `BonDriver_TCLG06_1.dll`）を選ぶ
3. TVTestのチャンネルスキャンで `Terrestrial ISDB-T` と `Satellite BS/CS ISDB-S` をそれぞれスキャンする
4. 地デジ・BS・CSを選局して映像・音声が出れば完了です

## デーモン（libtsd.so）を手動で差し替える手順

デーモン本体はTV内の **`/data/local/tmp/libtsd.so`** という1ファイルだけです。新しい `libtsd.so`（例: `libtsd_release.so`）が配布された場合は、セットアップをやり直さなくても、このファイルを差し替えるだけで更新できます。

PCのコマンドプロンプトで、Platform-Toolsのフォルダーから実行します（`<TVのIPアドレス>` はTVのIPに読み替え。USB接続で機器が1台だけの場合は `-s <TVのIPアドレス>:5555` の部分は不要です）。

```
rem まずTVへ接続する（初回はTVの画面で接続許可を押す）
adb connect <TVのIPアドレス>:5555

rem 今のデーモンをバックアップとしてPCに保存しておく（任意・推奨）
adb -s <TVのIPアドレス>:5555 pull /data/local/tmp/libtsd.so libtsd_backup.so

rem 新しいデーモンをTVへ送る
adb -s <TVのIPアドレス>:5555 push libtsd_release.so /data/local/tmp/libtsd.apk-new.so

rem 実行権限とラベルを付けて、本番ファイルに差し替える（Magiskの許可ダイアログが出たら許可）
adb -s <TVのIPアドレス>:5555 shell su -c 'chmod 755 /data/local/tmp/libtsd.apk-new.so; chcon u:object_r:vendor_file:s0 /data/local/tmp/libtsd.apk-new.so; mv /data/local/tmp/libtsd.apk-new.so /data/local/tmp/libtsd.so'

rem TVを再起動すると新しいデーモンが読み込まれる
adb -s <TVのIPアドレス>:5555 reboot
```

再起動後1〜2分待ってから、TVTestで受信できるか確認してください。戻したい場合は、バックアップした `libtsd_backup.so` を同じ手順で送り直します。

## 困ったとき

- **Permission denied と出る**: Magiskで対象アプリ（またはADBシェル）への許可を確認し、セットアップをやり直す
- **ADBに接続できない**: TVとPCが同じLANにあるか、TV側で接続許可をしたかを確認する
- **映像が出ない**: TV本体でその放送が受信できる状態か、TVTestのMPEG-2デコーダーが設定されているかを確認する

その他のトラブルシュートは `docs/SETUP.md` の「困ったとき」を参照してください。

## 本家からの主な変更点

- チャンネルカタログとBonDriver INIに `System=`（ISDB-T / ISDB-S）を導入し、BonDriverに2つのチューニング空間（Terrestrial / Satellite BS/CS）を実装
- 衛星選局パラメータを実機で検証・確定
  - デモジュレータ設定 `{tuner, mode=8, LバンドIF周波数(kHz), 0, sat_id, 0, 0}`（sat_id: BS=59 / CS=60）
- ベンダーチャンネルDBの「ルート」（地上=4 / 衛星=8）を選局ごとに切り替え、TVの表示系統に依存しない選局を実現
- TVアプリの自動EPG収集では衛星チャンネルをスキップし、視聴チューナーとの競合を解消
- TVTestのチャンネルスキャンに対応（選局の成功条件を「ストリーム開始時点」に緩和、各段階のタイムアウトも短縮）
- TV用アプリのパッケージ名を `app.tcltv3wave`、アプリ名を「32S5400 3波対応版」に変更

## 構成

| ディレクトリ | 内容 |
|---|---|
| `android/` | TV用セットアップAPK（`assets/` にデーモン実体・導入スクリプト） |
| `native/` | 常駐デーモン `libtsd.so` と制御ツール `tsctl` のソース |
| `bondriver/` | Windows用 BonDriver DLL（x86/x64）のソース |
| `windows/` | Windows用セットアップツール（チャンネルカタログ生成）のソース |
| `docs/` | 設計・セットアップ等のドキュメント |

通信ポート: TS配信 TCP 40556（チューナー0）/ 40558（チューナー1）、制御 TCP 40557（キー認証）。

## ビルド

- ネイティブデーモン（NDK r26d）:
  ```
  clang --target=armv7a-linux-androideabi21 --sysroot=<NDK>/sysroot \
    -shared -fPIC -O2 -Wall -Wextra -Werror \
    native/tsd.c native/tsd_psi.c -ldl -llog -o libtsd.so
  ```
- Windowsツール: `windows/build.ps1`（.NET Framework 4.x コンパイラ）
- Android APK: `android/build.ps1`（Android SDK Platform 35 / Build-Tools 36.0.0、要JDK）

## 制限事項

- 検証済みは TCL 32S5400 のみです
- 未契約の有料チャンネルは視聴できません（チャンネルスキャンへの登録はされます）
- APKを自前ビルドした場合、署名が変わるため、既存のセットアップアプリがある場合は先にアンインストールが必要です

## 謝辞

このプロジェクトは、**Long0823-1 さん**が開発・公開された [TCLTV-Bondriver](https://github.com/Long0823-1/TCLTV-Bondriver) をベースにしたフォークです。

TV側の配信デーモン、セットアップアプリ、Windowsツール、BonDriverというシステム全体の設計と実装、そして実機検証に必要な [Magisk適用済みbootイメージ](https://github.com/Long0823-1/TCL-G06-Magisk-Boot) の公開まで、そのすべてがLong0823-1さんの成果です。本フォークの変更は、その土台の上に衛星放送（BS/110度CS）の視聴を可能にするための追加です。

Long0823-1さんに、心から感謝いたします。

- 本家リポジトリ: https://github.com/Long0823-1/TCLTV-Bondriver

## ライセンス

MIT License（`LICENSE` を参照）。

Copyright (c) 2026 Long0823-1（本家プロジェクト）

#!/usr/bin/env python3
"""Dropbox にリリース JAR をアップロードするスクリプト（汎用版）。

プロジェクト毎の設定は .claude/dropbox-config.json で定義する。

使い方:
  python3 .claude/scripts/dropbox-upload.py upload --version 1.0.0 [--mc-version 1.20.1]
  python3 .claude/scripts/dropbox-upload.py check
  python3 .claude/scripts/dropbox-upload.py discover [--base /Mods/MyMod]

サブコマンド:
  upload     JAR をアップロード
  check      dropbox-config.json と Dropbox 上の既存構造を照合
  discover   Dropbox 上の既存フォルダ構造から dropbox-config.json のひな型を生成

認証情報:
  環境変数 (DROPBOX_APP_KEY, DROPBOX_APP_SECRET, DROPBOX_REFRESH_TOKEN)
  または ~/.config/dropbox-upload/credentials.json:
  {
    "app_key": "...",
    "app_secret": "...",
    "refresh_token": "..."
  }

dropbox-config.json の例:
  {
    "dropbox_base": "/Mods/MyMod",
    "upload_path": "{platform}/{mc_folder}/{jar}",
    "platforms": ["Fabric", "Forge"],
    "mc_version_folders": {
      "1.20.1": "1.20-1.20.1",
      "1.16.5": "1.16.x"
    }
  }

upload_path テンプレート変数:
  {platform}    - Fabric / Forge
  {mc_folder}   - mc_version_folders で変換後のフォルダ名
  {jar}         - JAR ファイル名
"""

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

CREDENTIALS_PATH = os.path.expanduser("~/.config/dropbox-upload/credentials.json")


def find_project_root():
    """スクリプトの位置から2階層上をプロジェクトルートとして返す。"""
    script_dir = Path(__file__).resolve().parent
    return script_dir.parent.parent


def load_project_config(project_root):
    """プロジェクトの dropbox-config.json を読み込む。"""
    config_path = project_root / ".claude" / "dropbox-config.json"
    if not config_path.exists():
        print(f"エラー: {config_path} が見つかりません。", file=sys.stderr)
        print("プロジェクトに dropbox-config.json を作成してください。", file=sys.stderr)
        print("ひな型の生成: python3 .claude/scripts/dropbox-upload.py discover --base /Mods/MyMod",
              file=sys.stderr)
        sys.exit(1)
    with open(config_path) as f:
        return json.load(f)


def load_gradle_properties(project_root):
    """gradle.properties からプロパティを読み込む。"""
    props = {}
    props_path = project_root / "gradle.properties"
    if props_path.exists():
        with open(props_path) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, value = line.split("=", 1)
                    props[key.strip()] = value.strip()
    return props


def load_credentials():
    """環境変数またはファイルから認証情報を読み込む。"""
    app_key = os.environ.get("DROPBOX_APP_KEY")
    app_secret = os.environ.get("DROPBOX_APP_SECRET")
    refresh_token = os.environ.get("DROPBOX_REFRESH_TOKEN")

    if not all([app_key, app_secret, refresh_token]) and os.path.exists(CREDENTIALS_PATH):
        with open(CREDENTIALS_PATH) as f:
            config = json.load(f)
        app_key = app_key or config.get("app_key")
        app_secret = app_secret or config.get("app_secret")
        refresh_token = refresh_token or config.get("refresh_token")

    if not all([app_key, app_secret, refresh_token]):
        print("エラー: Dropbox 認証情報が見つかりません。", file=sys.stderr)
        print(f"環境変数または {CREDENTIALS_PATH} に設定してください。", file=sys.stderr)
        sys.exit(1)

    return app_key, app_secret, refresh_token


def get_access_token(app_key, app_secret, refresh_token):
    """リフレッシュトークンからアクセストークンを取得する。"""
    data = urllib.parse.urlencode({
        "grant_type": "refresh_token",
        "refresh_token": refresh_token,
    }).encode()
    req = urllib.request.Request("https://api.dropboxapi.com/oauth2/token", data=data)
    credentials = base64.b64encode(f"{app_key}:{app_secret}".encode()).decode()
    req.add_header("Authorization", f"Basic {credentials}")

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read())
        return result["access_token"]
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"エラー: アクセストークン取得失敗: {e.code} {body}", file=sys.stderr)
        sys.exit(1)


def upload_file(access_token, local_path, dropbox_path):
    """ファイルを Dropbox にアップロードする。"""
    with open(local_path, "rb") as f:
        file_data = f.read()

    api_arg = json.dumps({
        "path": dropbox_path,
        "mode": "overwrite",
        "autorename": False,
    })
    req = urllib.request.Request(
        "https://content.dropboxapi.com/2/files/upload",
        data=file_data,
    )
    req.add_header("Authorization", f"Bearer {access_token}")
    req.add_header("Content-Type", "application/octet-stream")
    req.add_header("Dropbox-API-Arg", api_arg)

    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"エラー: アップロード失敗 ({dropbox_path}): {e.code} {body}", file=sys.stderr)
        sys.exit(1)


def list_folder(access_token, path):
    """Dropbox フォルダの内容を一覧取得する。"""
    data = json.dumps({"path": path, "recursive": True}).encode()
    req = urllib.request.Request(
        "https://api.dropboxapi.com/2/files/list_folder",
        data=data,
    )
    req.add_header("Authorization", f"Bearer {access_token}")
    req.add_header("Content-Type", "application/json")

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read())

        entries = result.get("entries", [])
        while result.get("has_more"):
            data = json.dumps({"cursor": result["cursor"]}).encode()
            req = urllib.request.Request(
                "https://api.dropboxapi.com/2/files/list_folder/continue",
                data=data,
            )
            req.add_header("Authorization", f"Bearer {access_token}")
            req.add_header("Content-Type", "application/json")
            with urllib.request.urlopen(req) as resp:
                result = json.loads(resp.read())
            entries.extend(result.get("entries", []))

        return entries
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        if e.code == 409 and "not_found" in body:
            return []
        print(f"エラー: フォルダ一覧取得失敗 ({path}): {e.code} {body}", file=sys.stderr)
        sys.exit(1)


def build_jar_list(project_root, project_config, gradle_props, version, mc_version):
    """アップロード対象の JAR リストを構築する。"""
    dropbox_base = project_config["dropbox_base"]
    upload_path_tmpl = project_config["upload_path"]
    platforms = project_config.get("platforms", ["Fabric", "Forge"])
    mc_version_folders = project_config.get("mc_version_folders", {})
    jar_prefix = gradle_props.get("archives_base_name", "Mod")

    mc_folder = mc_version_folders.get(mc_version)
    if mc_folder is None:
        print(f"エラー: MC バージョン {mc_version} のフォルダマッピングがありません。",
              file=sys.stderr)
        print("dropbox-config.json の mc_version_folders に追加してください。", file=sys.stderr)
        sys.exit(1)

    jars = []
    for platform in platforms:
        jar_name = f"{jar_prefix}-{mc_version}-{version}-{platform}.jar"
        local_path = str(project_root / f"{platform.lower()}/build/libs/{jar_name}")
        upload_path = upload_path_tmpl.format(
            platform=platform,
            mc_folder=mc_folder,
            jar=jar_name,
        )
        dropbox_path = f"{dropbox_base}/{upload_path}"
        jars.append((local_path, dropbox_path))

    return jars


def cmd_upload(args):
    """upload サブコマンド: JAR をアップロードする。"""
    project_root = find_project_root()
    project_config = load_project_config(project_root)
    gradle_props = load_gradle_properties(project_root)

    mc_version = args.mc_version or gradle_props.get("minecraft_version", "1.20.1")
    jars = build_jar_list(project_root, project_config, gradle_props, args.version, mc_version)

    # ファイル存在チェック
    for local_path, _ in jars:
        if not os.path.exists(local_path):
            print(f"エラー: {local_path} が見つかりません。先にビルドしてください。",
                  file=sys.stderr)
            sys.exit(1)

    if args.dry_run:
        for local_path, dropbox_path in jars:
            size = os.path.getsize(local_path)
            print(f"[dry-run] {local_path} ({size:,} bytes) -> {dropbox_path}")
        return

    app_key, app_secret, refresh_token = load_credentials()
    access_token = get_access_token(app_key, app_secret, refresh_token)

    for local_path, dropbox_path in jars:
        size = os.path.getsize(local_path)
        print(f"アップロード中: {local_path} ({size:,} bytes) -> {dropbox_path}")
        upload_file(access_token, local_path, dropbox_path)
        print(f"  完了: {dropbox_path}")


def cmd_check(args):
    """check サブコマンド: dropbox-config.json と Dropbox 上の構造を照合する。"""
    project_root = find_project_root()
    project_config = load_project_config(project_root)
    dropbox_base = project_config["dropbox_base"]
    mc_version_folders = project_config.get("mc_version_folders", {})
    upload_path_tmpl = project_config["upload_path"]
    platforms = project_config.get("platforms", ["Fabric", "Forge"])

    app_key, app_secret, refresh_token = load_credentials()
    access_token = get_access_token(app_key, app_secret, refresh_token)

    print(f"Dropbox 構造を確認中: {dropbox_base}")
    print()

    entries = list_folder(access_token, dropbox_base)
    folders = sorted(set(
        e["path_display"][len(dropbox_base):]
        for e in entries if e[".tag"] == "folder"
    ))

    if not folders:
        print(f"  (フォルダが見つかりません)")
    else:
        print("既存フォルダ:")
        for f in folders:
            print(f"  {f}")

    # 期待されるフォルダの照合
    print()
    print("設定との照合:")
    expected_folders = set()
    for mc_ver, mc_folder in mc_version_folders.items():
        for platform in platforms:
            path = "/" + upload_path_tmpl.format(
                platform=platform, mc_folder=mc_folder, jar="*"
            )
            # JAR 名部分を除去してディレクトリパスを取得
            dir_path = "/".join(path.split("/")[:-1])
            if dir_path:
                expected_folders.add(dir_path)

    for expected in sorted(expected_folders):
        if expected in folders:
            print(f"  OK       {expected}")
        else:
            print(f"  MISSING  {expected}")

    print()


def cmd_discover(args):
    """discover サブコマンド: Dropbox の既存構造から dropbox-config.json のひな型を生成する。"""
    dropbox_base = args.base
    if not dropbox_base:
        project_root = find_project_root()
        gradle_props = load_gradle_properties(project_root)
        jar_prefix = gradle_props.get("archives_base_name", "MyMod")
        dropbox_base = f"/Mods/{jar_prefix}"
        print(f"--base 未指定のため推測: {dropbox_base}")

    app_key, app_secret, refresh_token = load_credentials()
    access_token = get_access_token(app_key, app_secret, refresh_token)

    print(f"Dropbox 構造を取得中: {dropbox_base}")
    print()

    entries = list_folder(access_token, dropbox_base)
    if not entries:
        print(f"  {dropbox_base} にフォルダが見つかりません。")
        print(f"  パスが正しいか確認してください。")
        return

    folders = sorted(set(
        e["path_display"][len(dropbox_base):]
        for e in entries if e[".tag"] == "folder"
    ))

    print("発見されたフォルダ構造:")
    for f in folders:
        depth = f.count("/") - 1
        name = f.split("/")[-1]
        print(f"  {'  ' * depth}{name}/")

    # 構造推測: platform/mc_folder or mc_folder only
    top_level = [f.split("/")[1] for f in folders if f.count("/") == 1]
    has_platform = any(t in ("Fabric", "Forge", "NeoForge") for t in top_level)

    if has_platform:
        upload_path = "{platform}/{mc_folder}/{jar}"
        # platform 配下のフォルダを mc_folder 候補とする
        mc_folders_found = sorted(set(
            f.split("/")[2]
            for f in folders
            if f.count("/") >= 2 and f.split("/")[1] in ("Fabric", "Forge", "NeoForge")
        ))
        platforms = sorted(set(
            f.split("/")[1]
            for f in folders
            if f.count("/") >= 1 and f.split("/")[1] in ("Fabric", "Forge", "NeoForge")
        ))
    else:
        upload_path = "{mc_folder}/{jar}"
        mc_folders_found = sorted(set(top_level))
        platforms = ["Fabric", "Forge"]

    mc_version_folders = {folder: folder for folder in mc_folders_found}

    config = {
        "dropbox_base": dropbox_base,
        "upload_path": upload_path,
        "platforms": platforms,
        "mc_version_folders": mc_version_folders,
    }

    print()
    print("推奨 dropbox-config.json:")
    print(json.dumps(config, indent=2, ensure_ascii=False))
    print()
    print("注意: mc_version_folders のキーは Dropbox フォルダ名をそのまま使用しています。")
    print("gradle.properties の minecraft_version に合わせて修正してください。")
    print(f"例: \"1.20.1\": \"1.20-1.20.1\"")

    # 保存先の提案
    project_root = find_project_root()
    config_path = project_root / ".claude" / "dropbox-config.json"
    print()
    print(f"保存先: {config_path}")


def main():
    parser = argparse.ArgumentParser(description="Dropbox JAR アップロード（汎用版）")
    subparsers = parser.add_subparsers(dest="command", help="サブコマンド")

    # upload
    p_upload = subparsers.add_parser("upload", help="JAR をアップロード")
    p_upload.add_argument("--version", required=True, help="mod バージョン (例: 1.0.0)")
    p_upload.add_argument("--mc-version", default=None,
                          help="Minecraft バージョン (デフォルト: gradle.properties から取得)")
    p_upload.add_argument("--dry-run", action="store_true", help="実際にはアップロードしない")

    # check
    subparsers.add_parser("check", help="dropbox-config.json と Dropbox 上の構造を照合")

    # discover
    p_discover = subparsers.add_parser("discover",
                                       help="Dropbox の既存構造から config のひな型を生成")
    p_discover.add_argument("--base", default=None, help="Dropbox ベースパス (例: /Mods/MyMod)")

    args = parser.parse_args()

    if args.command == "upload":
        cmd_upload(args)
    elif args.command == "check":
        cmd_check(args)
    elif args.command == "discover":
        cmd_discover(args)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()

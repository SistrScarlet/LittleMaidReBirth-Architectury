#!/usr/bin/env python3
"""Dropbox にリリース JAR をアップロードし、共有リンクを取得するスクリプト。

使い方:
  python3 .claude/scripts/dropbox-upload.py --version 8.10.0 [--mc-version 1.20.1]

環境変数 (または ~/.config/lmrb/dropbox.json):
  DROPBOX_APP_KEY, DROPBOX_APP_SECRET, DROPBOX_REFRESH_TOKEN

設定ファイルの例 (~/.config/lmrb/dropbox.json):
  {
    "app_key": "...",
    "app_secret": "...",
    "refresh_token": "..."
  }
"""

import argparse
import json
import os
import sys
import urllib.request
import urllib.error
import urllib.parse

CONFIG_PATH = os.path.expanduser("~/.config/lmrb/dropbox.json")
DROPBOX_BASE = "/Mods/LittleMaidReBirth"

# Minecraft バージョン → Dropbox フォルダ名のマッピング
MC_VERSION_FOLDER = {
    "1.16.5": "1.16.x",
    "1.17.1": "1.17.x",
    "1.18.2": "1.18.x",
    "1.19.2": "1.19.2",
    "1.19.3": "1.19.3",
    "1.19.4": "1.19.4",
    "1.20": "1.20-1.20.1",
    "1.20.1": "1.20-1.20.1",
    "1.20.2": "1.20.2",
}


def load_config():
    """環境変数またはファイルから認証情報を読み込む。"""
    app_key = os.environ.get("DROPBOX_APP_KEY")
    app_secret = os.environ.get("DROPBOX_APP_SECRET")
    refresh_token = os.environ.get("DROPBOX_REFRESH_TOKEN")

    if not all([app_key, app_secret, refresh_token]) and os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH) as f:
            config = json.load(f)
        app_key = app_key or config.get("app_key")
        app_secret = app_secret or config.get("app_secret")
        refresh_token = refresh_token or config.get("refresh_token")

    if not all([app_key, app_secret, refresh_token]):
        print("エラー: Dropbox 認証情報が見つかりません。", file=sys.stderr)
        print(f"環境変数または {CONFIG_PATH} に設定してください。", file=sys.stderr)
        sys.exit(1)

    return app_key, app_secret, refresh_token


def get_access_token(app_key, app_secret, refresh_token):
    """リフレッシュトークンからアクセストークンを取得する。"""
    data = urllib.parse.urlencode({
        "grant_type": "refresh_token",
        "refresh_token": refresh_token,
    }).encode()
    req = urllib.request.Request("https://api.dropboxapi.com/oauth2/token", data=data)
    # Basic 認証
    import base64
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
    """ファイルをDropboxにアップロードする。"""
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
            result = json.loads(resp.read())
        return result
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"エラー: アップロード失敗 ({dropbox_path}): {e.code} {body}", file=sys.stderr)
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Dropbox にリリース JAR をアップロード")
    parser.add_argument("--version", required=True, help="mod バージョン (例: 8.10.0)")
    parser.add_argument("--mc-version", default="1.20.1", help="Minecraft バージョン (デフォルト: 1.20.1)")
    parser.add_argument("--dry-run", action="store_true", help="実際にはアップロードしない")
    args = parser.parse_args()

    version = args.version
    mc_version = args.mc_version

    mc_folder = MC_VERSION_FOLDER.get(mc_version)
    if mc_folder is None:
        print(f"エラー: MC バージョン {mc_version} のフォルダマッピングがありません。", file=sys.stderr)
        print(f"MC_VERSION_FOLDER に追加してください。", file=sys.stderr)
        sys.exit(1)

    jars = [
        (f"fabric/build/libs/LMRB-{mc_version}-{version}-Fabric.jar",
         f"{DROPBOX_BASE}/Fabric/{mc_folder}/LMRB-{mc_version}-{version}-Fabric.jar"),
        (f"forge/build/libs/LMRB-{mc_version}-{version}-Forge.jar",
         f"{DROPBOX_BASE}/Forge/{mc_folder}/LMRB-{mc_version}-{version}-Forge.jar"),
    ]

    # ファイル存在チェック
    for local_path, _ in jars:
        if not os.path.exists(local_path):
            print(f"エラー: {local_path} が見つかりません。先にビルドしてください。", file=sys.stderr)
            sys.exit(1)

    if args.dry_run:
        for local_path, dropbox_path in jars:
            size = os.path.getsize(local_path)
            print(f"[dry-run] {local_path} ({size:,} bytes) -> {dropbox_path}")
        return

    app_key, app_secret, refresh_token = load_config()
    access_token = get_access_token(app_key, app_secret, refresh_token)

    results = []
    for local_path, dropbox_path in jars:
        size = os.path.getsize(local_path)
        print(f"アップロード中: {local_path} ({size:,} bytes) -> {dropbox_path}")
        upload_file(access_token, local_path, dropbox_path)
        print(f"  完了: {dropbox_path}")


if __name__ == "__main__":
    main()

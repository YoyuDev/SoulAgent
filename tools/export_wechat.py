"""
微信聊天记录一键导出工具
基于 PyWxDump，自动完成：检测安装 → 获取密钥 → 解密数据库 → 导出聊天记录

注意：本工具仅用于学习和技术交流，请勿用于非法用途！
使用前请确保已登录微信 PC 版（建议 3.9.5 版本）

用法：
  python export_wechat.py
  python export_wechat.py --out D:\wx_out
  python export_wechat.py --db D:\wx_dump\de_MSG0.db --out D:\wx_out
"""

import argparse
import importlib
import json
import os
import sqlite3
import subprocess
import sys
import time



# 工具函数


def print_banner():
    print()
    print("=" * 56)
    print("   微信聊天记录一键导出工具")
    print("   基于 PyWxDump · 仅限学习交流使用")
    print("=" * 56)
    print()


def print_step(num, title):
    print(f"\n{'─' * 50}")
    print(f"  步骤 {num}：{title}")
    print(f"{'─' * 50}\n")


def run_cmd(args, capture=True):
    """执行命令，返回 (returncode, stdout, stderr)"""
    result = subprocess.run(
        args,
        capture_output=capture,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return result.returncode, result.stdout, result.stderr



# 检查并安装 PyWxDump


def check_pywxdump():
    print_step(1, "检查 PyWxDump 环境")

    try:
        mod = importlib.import_module("pywxdump")
        version = getattr(mod, "__version__", "未知")
        print(f"  ✓ PyWxDump 已安装（版本 {version}）")
        return True
    except ImportError:
        print("  ✗ PyWxDump 未安装，正在安装...")
        code, out, err = run_cmd([sys.executable, "-m", "pip", "install", "pywxdump"])
        if code == 0:
            print("  ✓ 安装成功")
            return True
        else:
            print(f"  ✗ 安装失败：{err}")
            print("    请手动执行：pip install pywxdump")
            return False



# 获取微信密钥和数据路径


def get_wechat_info():
    print_step(2, "获取微信信息（密钥 & 数据路径）")

    code, out, err = run_cmd([sys.executable, "-m", "pywxdump.cli", "info"])
    combined = (out or "") + "\n" + (err or "")

    if code != 0:
        print("  ✗ 获取信息失败，请确保：")
        print("    1. 微信已登录（官网下载的 PC 版）")
        print("    2. 微信版本建议 3.9.5")
        return None, None

    # 尝试 JSON 解析（部分版本输出 JSON）
    key, wx_dir = _parse_json_output(combined)
    if key and wx_dir:
        print(f"  ✓ Key:    {key[:16]}...")
        print(f"  ✓ wx_dir: {wx_dir}")
        return key, wx_dir

    # 回退：按行解析
    key, wx_dir = _parse_line_output(combined)
    if key and wx_dir:
        print(f"  ✓ Key:    {key[:16]}...")
        print(f"  ✓ wx_dir: {wx_dir}")
        return key, wx_dir

    print("  ✗ 未能自动解析出密钥，请查看上方原始输出")
    print("  手动输入密钥和路径（或 Ctrl+C 退出）：")
    key = input("  Key: ").strip()
    wx_dir = input("  wx_dir: ").strip()
    return key, wx_dir


def _parse_json_output(text):
    """尝试解析 JSON 格式输出"""
    for line in text.splitlines():
        line = line.strip()
        if not line.startswith("{"):
            continue
        try:
            data = json.loads(line)
            key = data.get("key") or data.get("Key")
            wx_dir = data.get("wx_dir") or data.get("WxDir") or data.get("wx_dir")
            if key and wx_dir:
                return key, wx_dir
        except json.JSONDecodeError:
            continue
    return None, None


def _parse_line_output(text):
    """按行解析 key 和 wx_dir"""
    key = None
    wx_dir = None
    for line in text.splitlines():
        lower = line.lower().strip()
        # 匹配 key
        if "key" in lower and (":" in line or "=" in line):
            val = line.split(":", 1)[-1].strip() if ":" in line else line.split("=", 1)[-1].strip()
            if len(val) >= 16:
                key = val
        # 匹配 wx_dir / path
        if any(k in lower for k in ["wx_dir", "wxdir", "path", "dir", "multi"]):
            val = line.split(":", 1)[-1].strip() if ":" in line else line.split("=", 1)[-1].strip()
            if val and ("WeChat" in val or "Msg" in val or "wxid" in val):
                wx_dir = val
    return key, wx_dir



# 解密数据库


def decrypt_database(key, wx_dir, out_dir):
    print_step(3, "解密微信数据库")

    # 定位 MSG0.db
    db_in = os.path.join(wx_dir, "Msg", "Multi", "MSG0.db")
    if not os.path.exists(db_in):
        # 尝试直接用 wx_dir 作为路径（用户可能直接给了 db 路径）
        if wx_dir.endswith(".db") and os.path.exists(wx_dir):
            db_in = wx_dir
        else:
            # 搜索 wx_dir 下的 MSG0.db
            for root, dirs, files in os.walk(wx_dir):
                for f in files:
                    if f.upper() == "MSG0.DB":
                        db_in = os.path.join(root, f)
                        break
                if db_in.endswith("MSG0.db") and os.path.exists(db_in):
                    break

    if not os.path.exists(db_in):
        print(f"  ✗ 未找到 MSG0.db，尝试路径：{db_in}")
        db_in = input("  请输入 MSG0.db 的完整路径：").strip().strip('"')

    print(f"  输入：{db_in}")
    print(f"  输出：{out_dir}")
    print("  正在解密（可能需要 1-3 分钟）...")

    os.makedirs(out_dir, exist_ok=True)

    code, out, err = run_cmd([
        sys.executable, "-m", "pywxdump.cli", "decrypt",
        "-k", key,
        "-i", db_in,
        "-o", out_dir,
    ])

    combined = (out or "") + "\n" + (err or "")

    if code != 0:
        print(f"  ✗ 解密失败：{combined}")
        return None

    # 查找解密后的数据库文件
    decrypted_db = None
    for f in os.listdir(out_dir):
        if f.endswith(".db") and "MSG" in f.upper():
            candidate = os.path.join(out_dir, f)
            # 优先选 de_MSG0.db
            if "de_" in f.lower() or "decrypt" in f.lower():
                decrypted_db = candidate
                break
            if decrypted_db is None:
                decrypted_db = candidate

    if decrypted_db is None:
        # 如果目录下只有一个 .db 文件
        dbs = [os.path.join(out_dir, f) for f in os.listdir(out_dir) if f.endswith(".db")]
        if len(dbs) == 1:
            decrypted_db = dbs[0]

    if decrypted_db:
        print(f"  ✓ 解密成功：{decrypted_db}")
        return decrypted_db
    else:
        print("  ✗ 解密完成但未找到输出的 .db 文件")
        print(f"    输出目录内容：{os.listdir(out_dir)}")
        return None



# 步骤 4：导出聊天记录为 txt


def export_to_txt(db_path, txt_dir):
    print_step(4, "导出聊天记录为 txt 文件")

    os.makedirs(txt_dir, exist_ok=True)

    if not os.path.exists(db_path):
        print(f"  ✗ 数据库文件不存在：{db_path}")
        return

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # 查询文本消息
    cursor.execute("""
        SELECT CreateTime, StrContent, IsSender, StrTalker
        FROM MSG
        WHERE Type = 1
        ORDER BY CreateTime
    """)

    chats = {}
    total = 0

    for ts, content, is_send, talker in cursor.fetchall():
        if not content:
            continue
        t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(ts))
        role = "我" if is_send == 1 else "TA"
        line = f"[{t}] {role}: {content}\n"

        if talker not in chats:
            chats[talker] = []
        chats[talker].append(line)
        total += 1

    conn.close()

    # 写入文件
    count = 0
    for talker, lines in chats.items():
        file_path = os.path.join(txt_dir, f"{talker}.txt")
        with open(file_path, "w", encoding="utf-8") as f:
            f.writelines(lines)
        count += 1

    print(f"  ✓ 导出完成")
    print(f"    消息总数：{total}")
    print(f"    联系人数：{count}")
    print(f"    输出目录：{txt_dir}")



# 主流程


def main():
    parser = argparse.ArgumentParser(description="微信聊天记录一键导出工具")
    parser.add_argument("--out", default=r"D:\wx_out", help="导出目录（默认 D:\\wx_out）")
    parser.add_argument("--db", default=None, help="已解密的数据库路径（跳过解密步骤）")
    args = parser.parse_args()

    print_banner()

    # 步骤 1：检查环境
    if not check_pywxdump():
        sys.exit(1)

    out_base = args.out
    txt_dir = os.path.join(out_base, "txt")

    # 如果提供了已解密的数据库，跳过步骤 2、3
    if args.db:
        if not os.path.exists(args.db):
            print(f"  ✗ 指定的数据库不存在：{args.db}")
            sys.exit(1)
        decrypted_db = args.db
        print(f"\n  使用指定的数据库：{decrypted_db}")
    else:
        # 步骤 2：获取密钥
        key, wx_dir = get_wechat_info()
        if not key or not wx_dir:
            print("\n  ✗ 无法获取微信信息，退出")
            sys.exit(1)

        # 步骤 3：解密
        decrypt_dir = os.path.join(out_base, "decrypt")
        decrypted_db = decrypt_database(key, wx_dir, decrypt_dir)
        if not decrypted_db:
            print("\n  ✗ 解密失败，退出")
            sys.exit(1)

    # 步骤 4：导出
    export_to_txt(decrypted_db, txt_dir)

    # 完成
    print(f"\n{'═' * 50}")
    print(f"  全部完成！聊天记录已导出到：")
    print(f"  {txt_dir}")
    print(f"{'═' * 50}")
    print()


if __name__ == "__main__":
    main()

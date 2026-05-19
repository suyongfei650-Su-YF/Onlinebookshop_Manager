# -*- coding: utf-8 -*-
"""将已生成的 110 条数据裁切为 100 条，并重建 SQL。"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COVERS = ROOT / "src" / "main" / "webapp" / "book-covers"
JSON_PATH = ROOT / "data" / "books" / "books_100.json"
SQL_PATH = ROOT / "sql" / "seed_books_100.sql"
DESC_SQL = ROOT / "sql" / "seed_books_100_with_description.sql"


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def main():
    records = json.loads(JSON_PATH.read_text(encoding="utf-8"))[:100]
    JSON_PATH.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    for p in COVERS.glob("book_2*.jpg"):
        num = int(p.stem.split("_")[1])
        if num >= 200:
            p.unlink()

    cats = {}
    for r in records:
        cats.setdefault(r["categoryId"], r["category"])

    sql = [
        "-- 100 本图书种子数据（10 个类别 × 10 本）",
        "-- 执行: mysql -u root -p bookshop_admin < sql/seed_books_100.sql",
        "USE bookshop_admin;",
        "",
        "INSERT INTO category (id, parent_id, name, name_en, code, sort_weight, visible) VALUES",
        "(4, NULL, '科幻与奇幻', 'Sci-Fi & Fantasy', 'SCIFI', 3, 1),",
        "(5, NULL, '历史与传记', 'History & Biography', 'HISTORY', 4, 1),",
        "(6, NULL, '经济管理', 'Business & Economics', 'BUSINESS', 5, 1),",
        "(7, NULL, '科学技术', 'Science & Technology', 'SCIENCE', 6, 1),",
        "(8, NULL, '心理学', 'Psychology', 'PSYCH', 7, 1),",
        "(9, NULL, '计算机与互联网', 'Computer & Internet', 'COMPUTER', 8, 1),",
        "(10, NULL, '少儿与绘本', 'Children & Picture Books', 'CHILDREN', 9, 1),",
        "(11, NULL, '生活与健康', 'Life & Health', 'LIFESTYLE', 10, 1)",
        "ON DUPLICATE KEY UPDATE name = VALUES(name);",
        "",
        "INSERT INTO book (id, category_id, title, author, isbn, price, stock, status, cover_url) VALUES",
    ]
    rows = [
        f"({r['id']}, {r['categoryId']}, '{esc(r['title'])}', '{esc(r['author'])}', "
        f"'{r['isbn']}', {r['price']:.2f}, {r['stock']}, '{r['status']}', '{r['coverUrl']}')"
        for r in records
    ]
    sql.append(",\n".join(rows))
    sql.append("ON DUPLICATE KEY UPDATE title=VALUES(title), author=VALUES(author),")
    sql.append("  isbn=VALUES(isbn), price=VALUES(price), stock=VALUES(stock), cover_url=VALUES(cover_url);")
    SQL_PATH.write_text("\n".join(sql), encoding="utf-8")

    desc_lines = [
        "-- 可选：先执行 sql/patch_book_description.sql 增加简介字段",
        "USE bookshop_admin;",
        "",
    ]
    for r in records:
        desc_lines.append(f"UPDATE book SET description = '{esc(r['description'])}' WHERE id = {r['id']};")
    DESC_SQL.write_text("\n".join(desc_lines), encoding="utf-8")
    print(f"OK: {len(records)} books, covers kept book_100..book_199")


if __name__ == "__main__":
    main()

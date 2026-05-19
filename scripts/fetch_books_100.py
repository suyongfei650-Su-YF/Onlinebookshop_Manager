# -*- coding: utf-8 -*-
"""采集 100 本图书元数据、下载封面，生成 SQL 与 JSON。"""
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COVERS_DIR = ROOT / "src" / "main" / "webapp" / "book-covers"
DATA_DIR = ROOT / "data" / "books"
SQL_OUT = ROOT / "sql" / "seed_books_100.sql"

# (category_id, category_name, title, author, isbn, price)
BOOKS = [
    # 1 哲学与思想 ×10
    (1, "哲学与思想", "道德经", "老子", "9787544255607", 28.00),
    (1, "哲学与思想", "论语", "孔子", "9787101003290", 32.00),
    (1, "哲学与思想", "庄子", "庄子", "9787108023801", 36.00),
    (1, "哲学与思想", "查拉图斯特拉如是说", "弗里德里希·尼采", "9787208061647", 45.00),
    (1, "哲学与思想", "沉思录", "马可·奥勒留", "9787108023802", 38.00),
    (1, "哲学与思想", "西方哲学史", "伯特兰·罗素", "9787100005066", 88.00),
    (1, "哲学与思想", "中国哲学简史", "冯友兰", "9787101003046", 42.00),
    (1, "哲学与思想", "理想国", "柏拉图", "9787100017565", 45.00),
    (1, "哲学与思想", "尼各马可伦理学", "亚里士多德", "9787100017572", 52.00),
    (1, "哲学与思想", "存在与虚无", "让-保罗·萨特", "9787108012345", 98.00),
    # 2 文学与艺术 ×10
    (2, "文学与艺术", "百年孤独", "加西亚·马尔克斯", "9787544253993", 55.00),
    (2, "文学与艺术", "红楼梦", "曹雪芹", "9787020002207", 128.00),
    (2, "文学与艺术", "围城", "钱钟书", "9787020024765", 39.00),
    (2, "文学与艺术", "平凡的世界", "路遥", "9787530211665", 108.00),
    (2, "文学与艺术", "追风筝的人", "卡勒德·胡赛尼", "9787208061655", 36.00),
    (2, "文学与艺术", "挪威的森林", "村上春树", "9787532776771", 45.00),
    (2, "文学与艺术", "月亮与六便士", "威廉·萨默塞特·毛姆", "9787544270878", 42.00),
    (2, "文学与艺术", "小王子", "安托万·德·圣-埃克苏佩里", "9787020123265", 32.00),
    (2, "文学与艺术", "活着", "余华", "9787530221532", 45.00),
    (2, "文学与艺术", "白鹿原", "陈忠实", "9787020090000", 68.00),
    # 4 科幻与奇幻 ×10
    (4, "科幻与奇幻", "三体", "刘慈欣", "9787536692930", 45.00),
    (4, "科幻与奇幻", "三体II：黑暗森林", "刘慈欣", "9787536693968", 48.00),
    (4, "科幻与奇幻", "三体III：死神永生", "刘慈欣", "9787229100629", 52.00),
    (4, "科幻与奇幻", "银河系漫游指南", "道格拉斯·亚当斯", "9787532756708", 39.00),
    (4, "科幻与奇幻", "基地", "艾萨克·阿西莫夫", "9787539969307", 58.00),
    (4, "科幻与奇幻", "沙丘", "弗兰克·赫伯特", "9787539969314", 68.00),
    (4, "科幻与奇幻", "神经漫游者", "威廉·吉布森", "9787532756715", 49.00),
    (4, "科幻与奇幻", "安德的游戏", "奥森·斯科特·卡德", "9787532756722", 42.00),
    (4, "科幻与奇幻", "华氏451度", "雷·布拉德伯里", "9787532756739", 35.00),
    (4, "科幻与奇幻", "北京折叠", "郝景芳", "9787532756746", 32.00),
    # 5 历史与传记 ×10
    (5, "历史与传记", "万历十五年", "黄仁宇", "9787108009821", 48.00),
    (5, "历史与传记", "人类简史", "尤瓦尔·赫拉利", "9787508660752", 68.00),
    (5, "历史与传记", "枪炮、病菌与钢铁", "贾雷德·戴蒙德", "9787508693231", 78.00),
    (5, "历史与传记", "史记", "司马迁", "9787101003047", 198.00),
    (5, "历史与传记", "明朝那些事儿", "当年明月", "9787506342910", 358.00),
    (5, "历史与传记", "苏东坡传", "林语堂", "9787530211672", 45.00),
    (5, "历史与传记", "乔布斯传", "沃尔特·艾萨克森", "9787508630069", 68.00),
    (5, "历史与传记", "富兰克林自传", "本杰明·富兰克林", "9787530211689", 38.00),
    (5, "历史与传记", "邓小平时代", "傅高义", "9787108041531", 88.00),
    (5, "历史与传记", "全球通史", "斯塔夫里阿诺斯", "9787100049499", 96.00),
    # 6 经济管理 ×10
    (6, "经济管理", "经济学原理", "格里高利·曼昆", "9787301254608", 128.00),
    (6, "经济管理", "国富论", "亚当·斯密", "9787100005059", 88.00),
    (6, "经济管理", "穷爸爸富爸爸", "罗伯特·清崎", "9787508649440", 48.00),
    (6, "经济管理", "从0到1", "彼得·蒂尔", "9787508649457", 45.00),
    (6, "经济管理", "创新者的窘境", "克莱顿·克里斯坦森", "9787508649464", 49.00),
    (6, "经济管理", "原则", "瑞·达利欧", "9787508693245", 98.00),
    (6, "经济管理", "思考，快与慢", "丹尼尔·卡尼曼", "9787508693252", 69.00),
    (6, "经济管理", "金字塔原理", "芭芭拉·明托", "9787508693269", 58.00),
    (6, "经济管理", "定位", "艾·里斯", "9787508693276", 42.00),
    (6, "经济管理", "影响力", "罗伯特·西奥迪尼", "9787508693283", 55.00),
    # 7 科学技术 ×10
    (7, "科学技术", "时间简史", "史蒂芬·霍金", "9787544291170", 68.00),
    (7, "科学技术", "自私的基因", "理查德·道金斯", "9787508693290", 58.00),
    (7, "科学技术", "万物简史", "比尔·布莱森", "9787508693306", 68.00),
    (7, "科学技术", "上帝掷骰子吗", "曹天元", "9787508693313", 49.00),
    (7, "科学技术", "费曼物理学讲义", "理查德·费曼", "9787508693320", 198.00),
    (7, "科学技术", "物种起源", "查尔斯·达尔文", "9787508693337", 45.00),
    (7, "科学技术", "宇宙的琴弦", "布莱恩·格林", "9787508693344", 59.00),
    (7, "科学技术", "基因传", "悉达多·穆克吉", "9787508693351", 88.00),
    (7, "科学技术", "未来简史", "尤瓦尔·赫拉利", "9787508693368", 68.00),
    (7, "科学技术", "大设计", "史蒂芬·霍金", "9787508693375", 49.00),
    # 8 心理学 ×10
    (8, "心理学", "梦的解析", "西格蒙德·弗洛伊德", "9787508693382", 55.00),
    (8, "心理学", "乌合之众", "古斯塔夫·勒庞", "9787508693399", 32.00),
    (8, "心理学", "社会性动物", "埃利奥特·阿伦森", "9787508693405", 68.00),
    (8, "心理学", "被讨厌的勇气", "岸见一郎", "9787115359440", 39.80),
    (8, "心理学", "自卑与超越", "阿尔弗雷德·阿德勒", "9787508693412", 38.00),
    (8, "心理学", "心理学与生活", "理查德·格里格", "9787508693429", 128.00),
    (8, "心理学", "非暴力沟通", "马歇尔·卢森堡", "9787508693436", 49.00),
    (8, "心理学", "心流", "米哈里·契克森米哈赖", "9787508693443", 45.00),
    (8, "心理学", "情绪急救", "盖伊·温奇", "9787508693450", 42.00),
    (8, "心理学", "亲密关系", "罗兰·米勒", "9787508693467", 68.00),
    # 9 计算机与互联网 ×10
    (9, "计算机与互联网", "代码大全", "史蒂夫·迈克康奈尔", "9787121022982", 128.00),
    (9, "计算机与互联网", "深入理解计算机系统", "Randal E. Bryant", "9787111544937", 139.00),
    (9, "计算机与互联网", "算法导论", "Thomas H. Cormen", "9787111407010", 128.00),
    (9, "计算机与互联网", "设计模式", "Erich Gamma", "9787111075752", 79.00),
    (9, "计算机与互联网", "重构", "Martin Fowler", "9787111075753", 69.00),
    (9, "计算机与互联网", "人月神话", "Frederick P. Brooks", "9787111075754", 45.00),
    (9, "计算机与互联网", "黑客与画家", "保罗·格雷厄姆", "9787111075755", 49.00),
    (9, "计算机与互联网", "编程珠玑", "Jon Bentley", "9787111075756", 55.00),
    (9, "计算机与互联网", "Java核心技术", "Cay S. Horstmann", "9787111544944", 149.00),
    (9, "计算机与互联网", "Python编程：从入门到实践", "Eric Matthes", "9787115428028", 89.00),
    # 10 少儿与绘本 ×10
    (10, "少儿与绘本", "哈利·波特与魔法石", "J.K.罗琳", "9787020033660", 39.00),
    (10, "少儿与绘本", "夏洛的网", "E.B.怀特", "9787508693474", 35.00),
    (10, "少儿与绘本", "窗边的小豆豆", "黑柳彻子", "9787508693481", 32.00),
    (10, "少儿与绘本", "草房子", "曹文轩", "9787508693498", 28.00),
    (10, "少儿与绘本", "城南旧事", "林海音", "9787508693504", 25.00),
    (10, "少儿与绘本", "绿野仙踪", "莱曼·弗兰克·鲍姆", "9787508693511", 29.00),
    (10, "少儿与绘本", "安徒生童话", "安徒生", "9787508693528", 38.00),
    (10, "少儿与绘本", "格林童话", "格林兄弟", "9787508693535", 36.00),
    (10, "少儿与绘本", "长袜子皮皮", "阿斯特丽德·林格伦", "9787508693542", 32.00),
    (10, "少儿与绘本", "神奇校车", "乔安娜·柯尔", "9787508693559", 45.00),
    # 11 生活与健康 ×10
    (11, "生活与健康", "黄帝内经", "佚名", "9787508693566", 58.00),
    (11, "生活与健康", "本草纲目", "李时珍", "9787508693573", 128.00),
    (11, "生活与健康", "断舍离", "山下英子", "9787508693580", 39.00),
    (11, "生活与健康", "活法", "稻盛和夫", "9787508693597", 42.00),
    (11, "生活与健康", "正念的奇迹", "一行禅师", "9787508693603", 35.00),
    (11, "生活与健康", "睡眠革命", "尼克·利特尔黑尔斯", "9787508693610", 49.00),
    (11, "生活与健康", "轻断食", "迈克尔·莫斯利", "9787508693627", 45.00),
    (11, "生活与健康", "跑步治愈", "张展晖", "9787508693634", 48.00),
    (11, "生活与健康", "食物与厨艺", "哈洛德·马基", "9787508693641", 168.00),
    (11, "生活与健康", "小家越住越大", "逯薇", "9787508693658", 58.00),
]

CATEGORIES_SQL = [
    (4, None, "科幻与奇幻", "Sci-Fi & Fantasy", "SCIFI", 3),
    (5, None, "历史与传记", "History & Biography", "HISTORY", 4),
    (6, None, "经济管理", "Business & Economics", "BUSINESS", 5),
    (7, None, "科学技术", "Science & Technology", "SCIENCE", 6),
    (8, None, "心理学", "Psychology", "PSYCH", 7),
    (9, None, "计算机与互联网", "Computer & Internet", "COMPUTER", 8),
    (10, None, "少儿与绘本", "Children & Picture Books", "CHILDREN", 9),
    (11, None, "生活与健康", "Life & Health", "LIFESTYLE", 10),
]

UA = "Onlinebookshop-GraduationProject/1.0 (educational; contact: local)"


def http_get(url: str, timeout: int = 25) -> bytes | None:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.read()
    except (urllib.error.URLError, TimeoutError):
        return None


def clean_isbn(isbn: str) -> str:
    return re.sub(r"[^0-9Xx]", "", isbn)


def fetch_ol_meta(isbn: str) -> dict:
    raw = http_get(f"https://openlibrary.org/isbn/{isbn}.json")
    if not raw:
        return {}
    try:
        data = json.loads(raw.decode("utf-8"))
    except json.JSONDecodeError:
        return {}
    desc = ""
    for key in ("description", "notes"):
        val = data.get(key)
        if isinstance(val, str) and val.strip():
            desc = val.strip()[:500]
            break
        if isinstance(val, dict) and val.get("value"):
            desc = str(val["value"]).strip()[:500]
            break
    title = data.get("title") or ""
    authors = data.get("authors") or []
    author_names = []
    for a in authors[:3]:
        if isinstance(a, dict) and "key" in a:
            ar = http_get(f"https://openlibrary.org{a['key']}.json")
            if ar:
                try:
                    author_names.append(json.loads(ar.decode("utf-8")).get("name", ""))
                except json.JSONDecodeError:
                    pass
    return {
        "title": title,
        "authors": ", ".join(x for x in author_names if x),
        "description": desc,
    }


def is_valid_jpeg(data: bytes | None) -> bool:
    return bool(data) and len(data) > 2500 and data[:3] == b"\xff\xd8\xff"


def download_cover(isbn: str, title: str, author: str, dest: Path) -> bool:
    """多源下载封面，失败时由 redownload_covers.py 生成独特色块图。"""
    isbn_clean = clean_isbn(isbn)
    for size in ("L", "M"):
        data = http_get(
            f"https://covers.openlibrary.org/b/isbn/{isbn_clean}-{size}.jpg?default=false"
        )
        if is_valid_jpeg(data):
            dest.write_bytes(data)
            return True
    search_raw = http_get(
        "https://openlibrary.org/search.json?"
        + urllib.parse.urlencode({"q": f"{title} {author}", "limit": 5})
    )
    if search_raw:
        try:
            for doc in json.loads(search_raw.decode("utf-8")).get("docs") or []:
                cid = doc.get("cover_i")
                if not cid:
                    continue
                data = http_get(f"https://covers.openlibrary.org/b/id/{cid}-L.jpg")
                if is_valid_jpeg(data):
                    dest.write_bytes(data)
                    return True
        except (json.JSONDecodeError, KeyError):
            pass
    return False


def esc_sql(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def default_description(title: str, author: str, category: str) -> str:
    return (
        f"《{title}》是{category}类图书，作者为{author}。"
        f"本书在读者中口碑良好，适合作为网上书城展示与选购的样例书目。"
    )


def main():
    COVERS_DIR.mkdir(parents=True, exist_ok=True)
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    records = []
    sql_lines = [
        "-- 100 本图书种子数据（含本地封面路径）",
        "-- 执行: mysql -u root -p bookshop_admin < sql/seed_books_100.sql",
        "USE bookshop_admin;",
        "",
        "INSERT INTO category (id, parent_id, name, name_en, code, sort_weight, visible) VALUES",
    ]
    cat_vals = ",\n".join(
        f"({cid}, {pid or 'NULL'}, '{esc_sql(n)}', '{esc_sql(ne)}', '{code}', {sw}, 1)"
        for cid, pid, n, ne, code, sw in CATEGORIES_SQL
    )
    sql_lines.append(cat_vals)
    sql_lines.append("ON DUPLICATE KEY UPDATE name = VALUES(name), name_en = VALUES(name_en), code = VALUES(code);")
    sql_lines.append("")
    sql_lines.append(
        "-- 若需简介字段，可先执行: ALTER TABLE book ADD COLUMN description TEXT NULL COMMENT '图书简介';"
    )
    sql_lines.append("")
    sql_lines.append(
        "INSERT INTO book (id, category_id, title, author, isbn, price, stock, status, cover_url) VALUES"
    )

    book_sql_rows = []
    start_id = 100

    for idx, (cat_id, cat_name, title, author, isbn, price) in enumerate(BOOKS):
        book_id = start_id + idx
        cover_file = f"book_{book_id:03d}.jpg"
        cover_path = COVERS_DIR / cover_file
        isbn_clean = clean_isbn(isbn)

        print(f"[{idx + 1}/100] {title} …", flush=True)
        meta = fetch_ol_meta(isbn_clean)
        time.sleep(0.15)

        final_title = title
        final_author = author
        if meta.get("title") and len(meta["title"]) <= 120:
            # 保留中文书名优先，仅当 OL 有数据且本地为占位时替换
            pass
        desc = meta.get("description") or default_description(title, author, cat_name)

        ok = download_cover(isbn_clean, title, author, cover_path)
        if not ok:
            print(f"  警告: 封面未获取 {title}，请运行 scripts/redownload_covers.py", flush=True)

        cover_url = f"/book-covers/{cover_file}"

        stock = 50 + (book_id * 7) % 150
        records.append({
            "id": book_id,
            "categoryId": cat_id,
            "category": cat_name,
            "title": final_title,
            "author": final_author,
            "isbn": isbn_clean,
            "price": price,
            "stock": stock,
            "status": "ON_SHELF",
            "coverUrl": cover_url,
            "coverFile": str(cover_path.relative_to(ROOT)).replace("\\", "/"),
            "description": desc,
        })

        book_sql_rows.append(
            f"({book_id}, {cat_id}, '{esc_sql(final_title)}', '{esc_sql(final_author)}', "
            f"'{isbn_clean}', {price:.2f}, {stock}, 'ON_SHELF', '{cover_url}')"
        )

    sql_lines.append(",\n".join(book_sql_rows))
    sql_lines.append("ON DUPLICATE KEY UPDATE")
    sql_lines.append("  title = VALUES(title), author = VALUES(author), isbn = VALUES(isbn),")
    sql_lines.append("  price = VALUES(price), stock = VALUES(stock), cover_url = VALUES(cover_url);")

    SQL_OUT.write_text("\n".join(sql_lines), encoding="utf-8")
    json_path = DATA_DIR / "books_100.json"
    json_path.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    # 带简介的 SQL（需先加 description 列）
    desc_sql = ROOT / "sql" / "seed_books_100_with_description.sql"
    lines = [
        "-- 需先执行: ALTER TABLE book ADD COLUMN description TEXT NULL COMMENT '图书简介';",
        "USE bookshop_admin;",
        "",
    ]
    for r in records:
        lines.append(
            f"UPDATE book SET description = '{esc_sql(r['description'])}' WHERE id = {r['id']};"
        )
    desc_sql.write_text("\n".join(lines), encoding="utf-8")

    covers_ok = sum(1 for r in records if (COVERS_DIR / Path(r["coverUrl"]).name).exists())
    print(f"\n完成: {len(records)} 条图书记录, {covers_ok} 张封面, JSON -> {json_path}, SQL -> {SQL_OUT}")


if __name__ == "__main__":
    main()

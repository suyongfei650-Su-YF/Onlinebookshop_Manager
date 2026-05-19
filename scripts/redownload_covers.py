# -*- coding: utf-8 -*-
"""重新下载 100 本不重复封面：Google Books -> Open Library -> 按书名生成独特色块封面。"""
import hashlib
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COVERS_DIR = ROOT / "src" / "main" / "webapp" / "book-covers"
JSON_PATH = ROOT / "data" / "books" / "books_100.json"

UA = "Onlinebookshop-GraduationProject/1.0"

# 检索用英文名（提高 Google Books / OL 命中率）
EN_TITLES = {
    "道德经": "Tao Te Ching",
    "论语": "Analects Confucius",
    "庄子": "Zhuangzi",
    "查拉图斯特拉如是说": "Thus Spoke Zarathustra",
    "沉思录": "Meditations Marcus Aurelius",
    "西方哲学史": "History of Western Philosophy Russell",
    "中国哲学简史": "Short History of Chinese Philosophy",
    "理想国": "The Republic Plato",
    "尼各马可伦理学": "Nicomachean Ethics",
    "存在与虚无": "Being and Nothingness Sartre",
    "百年孤独": "One Hundred Years of Solitude",
    "红楼梦": "Dream of the Red Chamber",
    "围城": "Fortress Besieged",
    "平凡的世界": "Ordinary World",
    "追风筝的人": "The Kite Runner",
    "挪威的森林": "Norwegian Wood Murakami",
    "月亮与六便士": "The Moon and Sixpence",
    "小王子": "The Little Prince",
    "活着": "To Live Yu Hua",
    "白鹿原": "White Deer Plain",
    "三体": "The Three-Body Problem",
    "三体II：黑暗森林": "The Dark Forest",
    "三体III：死神永生": "Death's End",
    "银河系漫游指南": "Hitchhiker's Guide to the Galaxy",
    "基地": "Foundation Asimov",
    "沙丘": "Dune Herbert",
    "神经漫游者": "Neuromancer",
    "安德的游戏": "Ender's Game",
    "华氏451度": "Fahrenheit 451",
    "万历十五年": "1587 A Year of No Significance",
    "人类简史": "Sapiens",
    "枪炮、病菌与钢铁": "Guns Germs and Steel",
    "史记": "Records of the Grand Historian",
    "明朝那些事儿": "Those Things of Ming Dynasty",
    "苏东坡传": "The Gay Genius",
    "乔布斯传": "Steve Jobs Walter Isaacson",
    "富兰克林自传": "Autobiography of Benjamin Franklin",
    "邓小平时代": "Deng Xiaoping and the Transformation of China",
    "全球通史": "A Global History",
    "经济学原理": "Principles of Economics Mankiw",
    "国富论": "Wealth of Nations",
    "穷爸爸富爸爸": "Rich Dad Poor Dad",
    "从0到1": "Zero to One Peter Thiel",
    "创新者的窘境": "The Innovator's Dilemma",
    "原则": "Principles Ray Dalio",
    "思考，快与慢": "Thinking Fast and Slow",
    "金字塔原理": "The Pyramid Principle",
    "定位": "Positioning Ries Trout",
    "影响力": "Influence Cialdini",
    "时间简史": "A Brief History of Time",
    "自私的基因": "The Selfish Gene",
    "万物简史": "A Short History of Nearly Everything",
    "上帝掷骰子吗": "Quantum Physics History",
    "费曼物理学讲义": "The Feynman Lectures on Physics",
    "物种起源": "On the Origin of Species",
    "宇宙的琴弦": "The Elegant Universe",
    "基因传": "The Gene Siddhartha Mukherjee",
    "未来简史": "Homo Deus",
    "大设计": "The Grand Design Hawking",
    "梦的解析": "The Interpretation of Dreams",
    "乌合之众": "The Crowd Le Bon",
    "社会性动物": "The Social Animal Aronson",
    "被讨厌的勇气": "The Courage to Be Disliked",
    "自卑与超越": "What Life Could Mean to You",
    "心理学与生活": "Psychology and Life",
    "非暴力沟通": "Nonviolent Communication",
    "心流": "Flow Csikszentmihalyi",
    "情绪急救": "Emotional First Aid",
    "亲密关系": "Intimate Relationships Miller",
    "代码大全": "Code Complete",
    "深入理解计算机系统": "Computer Systems A Programmer's Perspective",
    "算法导论": "Introduction to Algorithms",
    "设计模式": "Design Patterns Gang of Four",
    "重构": "Refactoring Fowler",
    "人月神话": "The Mythical Man-Month",
    "黑客与画家": "Hackers and Painters",
    "编程珠玑": "Programming Pearls",
    "Java核心技术": "Core Java Horstmann",
    "Python编程：从入门到实践": "Python Crash Course",
    "哈利·波特与魔法石": "Harry Potter and the Philosopher's Stone",
    "夏洛的网": "Charlotte's Web",
    "窗边的小豆豆": "Totto-chan",
    "草房子": "Straw House Cao Wenxuan",
    "城南旧事": "Memories of Peking South",
    "绿野仙踪": "The Wonderful Wizard of Oz",
    "安徒生童话": "Andersen's Fairy Tales",
    "格林童话": "Grimm's Fairy Tales",
    "长袜子皮皮": "Pippi Longstocking",
    "神奇校车": "Magic School Bus",
    "黄帝内经": "Huangdi Neijing",
    "本草纲目": "Compendium of Materia Medica",
    "断舍离": "Decluttering Yamashita",
    "活法": "Living Philosophy Inamori",
    "正念的奇迹": "The Miracle of Mindfulness",
    "睡眠革命": "Sleep Nick Littlehales",
    "轻断食": "The Fast Diet",
    "跑步治愈": "Running",
    "食物与厨艺": "On Food and Cooking McGee",
    "小家越住越大": "Home Design",
    "北京折叠": "Folding Beijing",
}


def http_get(url: str, timeout: int = 20) -> bytes | None:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.read()
    except (urllib.error.URLError, TimeoutError, OSError):
        return None


def is_valid_jpeg(data: bytes) -> bool:
    return bool(data) and len(data) > 2500 and data[:3] == b"\xff\xd8\xff"


def md5(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


def google_cover(title: str, author: str, isbn: str) -> bytes | None:
    queries = [
        f"isbn:{isbn}",
        f"isbn:{isbn[:10]}" if len(isbn) >= 10 else None,
    ]
    en = EN_TITLES.get(title)
    if en:
        queries.append(en)
    queries.append(f"{title} {author.split('·')[0][:12]}")
    for q in queries:
        if not q:
            continue
        url = "https://www.googleapis.com/books/v1/volumes?" + urllib.parse.urlencode(
            {"q": q, "maxResults": 5}
        )
        raw = http_get(url)
        if not raw:
            continue
        try:
            items = json.loads(raw.decode("utf-8")).get("items") or []
        except json.JSONDecodeError:
            continue
        for item in items:
            links = (item.get("volumeInfo") or {}).get("imageLinks") or {}
            for key in ("extraLarge", "large", "medium", "thumbnail", "smallThumbnail"):
                img_url = links.get(key)
                if not img_url:
                    continue
                img_url = img_url.replace("http://", "https://")
                if "zoom=" in img_url:
                    img_url = re.sub(r"zoom=\d+", "zoom=1", img_url)
                data = http_get(img_url)
                if is_valid_jpeg(data):
                    return data
        time.sleep(0.2)
    return None


def ol_cover_by_isbn(isbn: str) -> bytes | None:
    clean = re.sub(r"[^0-9Xx]", "", isbn)
    for size in ("L", "M"):
        data = http_get(f"https://covers.openlibrary.org/b/isbn/{clean}-{size}.jpg?default=false")
        if is_valid_jpeg(data):
            return data
    return None


def ol_cover_by_search(title: str, author: str) -> bytes | None:
    author_short = re.split(r"[·,，/]", author)[0].strip()
    queries = []
    if title in EN_TITLES:
        queries.append(EN_TITLES[title])
    queries.extend([f"{title} {author_short}", title, author_short])
    seen_ids = set()
    for q in queries:
        if not q:
            continue
        url = "https://openlibrary.org/search.json?" + urllib.parse.urlencode(
            {"q": q, "limit": 8, "fields": "cover_i,isbn,title"}
        )
        raw = http_get(url)
        if not raw:
            continue
        try:
            docs = json.loads(raw.decode("utf-8")).get("docs") or []
        except json.JSONDecodeError:
            continue
        for doc in docs:
            cid = doc.get("cover_i")
            if not cid or cid in seen_ids:
                continue
            seen_ids.add(cid)
            data = http_get(f"https://covers.openlibrary.org/b/id/{cid}-L.jpg")
            if is_valid_jpeg(data):
                return data
        time.sleep(0.15)
    return None


def generate_unique_cover(book_id: int, title: str, author: str) -> bytes:
    """生成带书名/作者文字的独特色块封面（保证每张不同）。"""
    from PIL import Image, ImageDraw, ImageFont

    w, h = 300, 450
    hue = (book_id * 37 + len(title) * 13) % 360
    # HSL -> RGB 简化
    import colorsys

    r, g, b = [int(x * 255) for x in colorsys.hls_to_rgb(hue / 360, 0.35, 0.55)]
    r2, g2, b2 = [int(x * 255) for x in colorsys.hls_to_rgb((hue + 40) / 360, 0.25, 0.45)]
    img = Image.new("RGB", (w, h), (r, g, b))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, h - 8, w, h], fill=(r2, g2, b2))
    draw.rectangle([0, 0, w, 60], fill=(r2, g2, b2))

    try:
        font_title = ImageFont.truetype("msyh.ttc", 26)
        font_sub = ImageFont.truetype("msyh.ttc", 16)
    except OSError:
        font_title = ImageFont.load_default()
        font_sub = font_title

    def wrap(text: str, max_chars: int = 8) -> list[str]:
        lines, cur = [], ""
        for ch in text:
            cur += ch
            if len(cur) >= max_chars:
                lines.append(cur)
                cur = ""
        if cur:
            lines.append(cur)
        return lines[:5]

    y = 100
    for line in wrap(title, 7):
        draw.text((24, y), line, fill=(255, 255, 255), font=font_title)
        y += 36
    y += 20
    auth = author[:14] + ("…" if len(author) > 14 else "")
    draw.text((24, y), auth, fill=(230, 230, 230), font=font_sub)
    draw.text((24, h - 40), f"No.{book_id}", fill=(200, 200, 200), font=font_sub)

    import io

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=88)
    return buf.getvalue()


def main():
    records = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    used_hashes: set[str] = set()
    stats = {"google": 0, "ol_isbn": 0, "ol_search": 0, "generated": 0, "dup_retry": 0}

    for i, book in enumerate(records):
        book_id = book["id"]
        title = book["title"]
        author = book["author"]
        isbn = book["isbn"]
        dest = COVERS_DIR / f"book_{book_id:03d}.jpg"
        print(f"[{i+1}/{len(records)}] {title}", flush=True)

        candidates: list[tuple[str, bytes]] = []

        g = google_cover(title, author, isbn)
        if g:
            candidates.append(("google", g))

        o1 = ol_cover_by_isbn(isbn)
        if o1:
            candidates.append(("ol_isbn", o1))

        o2 = ol_cover_by_search(title, author)
        if o2:
            candidates.append(("ol_search", o2))

        chosen = None
        for source, data in candidates:
            h = md5(data)
            if h not in used_hashes:
                chosen = (source, data, h)
                break
            stats["dup_retry"] += 1

        if not chosen:
            # 若 API 返回重复图，用生成封面
            data = generate_unique_cover(book_id, title, author)
            chosen = ("generated", data, md5(data))
            stats["generated"] += 1
        else:
            stats[chosen[0]] += 1

        used_hashes.add(chosen[2])
        dest.write_bytes(chosen[1])
        time.sleep(0.25)

    JSON_PATH.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n来源统计: {stats}")
    print(f"唯一封面数: {len(used_hashes)}/{len(records)}")


if __name__ == "__main__":
    main()

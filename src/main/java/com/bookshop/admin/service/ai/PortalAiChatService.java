package com.bookshop.admin.service.ai;

import com.bookshop.admin.config.AiProperties;
import com.bookshop.admin.dto.CartBookRow;
import com.bookshop.admin.dto.DashboardTopBookRow;
import com.bookshop.admin.dto.FavoriteBookRow;
import com.bookshop.admin.dto.PortalAiActionResult;
import com.bookshop.admin.dto.PortalAiBookCard;
import com.bookshop.admin.dto.PortalAiChatResult;
import com.bookshop.admin.dto.PortalCategoryRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.Customer;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CartItemMapper;
import com.bookshop.admin.mapper.CustomerMapper;
import com.bookshop.admin.mapper.OrderItemMapper;
import com.bookshop.admin.service.UserCartService;
import com.bookshop.admin.service.UserFavoriteService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PortalAiChatService {

    private static final int SEARCH_LIMIT = 8;
    private static final int HISTORY_LIMIT = 8;

    private static final Pattern BOOK_ID_PATTERN =
            Pattern.compile("(?:id|编号|#)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOK_TITLE_PATTERN = Pattern.compile("《([^》]{1,40})》");

    private final AiProperties aiProperties;
    private final OpenAiCompatibleClient aiClient;
    private final BookMapper bookMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final CartItemMapper cartItemMapper;
    private final UserCartService userCartService;
    private final UserFavoriteService userFavoriteService;

    public PortalAiChatService(
            AiProperties aiProperties,
            OpenAiCompatibleClient aiClient,
            BookMapper bookMapper,
            OrderItemMapper orderItemMapper,
            CustomerMapper customerMapper,
            CartItemMapper cartItemMapper,
            UserCartService userCartService,
            UserFavoriteService userFavoriteService) {
        this.aiProperties = aiProperties;
        this.aiClient = aiClient;
        this.bookMapper = bookMapper;
        this.orderItemMapper = orderItemMapper;
        this.customerMapper = customerMapper;
        this.cartItemMapper = cartItemMapper;
        this.userCartService = userCartService;
        this.userFavoriteService = userFavoriteService;
    }

    public Map<String, Object> buildStatusExtras(HttpSession session) {
        PortalUserContext ctx = resolveUserContext(session);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loggedIn", ctx.loggedIn);
        data.put("guest", !ctx.loggedIn);
        data.put("userNickname", ctx.displayName);
        if (ctx.loggedIn) {
            data.put("cartCount", userCartService.countItems(ctx.userId));
            data.put("favoriteCount", ctx.favoriteCount);
            data.put("favoriteBookIds", new ArrayList<>(ctx.favoriteBookIds));
        }
        return data;
    }

    public PortalAiChatResult chat(
            String userMessage, List<OpenAiCompatibleClient.ChatMessage> history, HttpSession session) {
        PortalAiChatResult result = new PortalAiChatResult();
        PortalUserContext ctx = resolveUserContext(session);
        applyUserMeta(result, ctx);

        String msg = userMessage == null ? "" : userMessage.trim();
        if (msg.isEmpty()) {
            result.setReply("请输入您想查找的书名、作者或阅读主题，我会帮您在馆藏中检索并推荐。"
                    + (ctx.loggedIn
                            ? " 登录后还可说「把第一本加入购物车」「查看我的收藏」或「根据收藏推荐」。"
                            : " 游客可先找书，登录后可使用购物车与个人收藏。"));
            return result;
        }

        boolean guide = isGuideIntent(msg);
        boolean addCart = isAddToCartIntent(msg);
        boolean viewCart = isViewCartIntent(msg);
        boolean viewFav = isViewFavoritesIntent(msg);
        boolean favRec = isFavoriteBasedRecommendIntent(msg);
        boolean addFav = isAddFavoriteIntent(msg);
        boolean removeFav = isRemoveFavoriteIntent(msg);

        Long categoryId = (guide && !addCart && !favRec) ? null : resolveCategoryId(msg);
        String keyword = (guide && !addCart && !favRec) ? null : buildSearchKeyword(msg, categoryId);

        if (viewFav && ctx.loggedIn) {
            result.setBooks(cardsFromFavorites(ctx));
        } else if (favRec && ctx.loggedIn) {
            List<PortalAiBookCard> rec = recommendFromFavorites(ctx);
            if (!rec.isEmpty()) {
                result.setBooks(rec);
            }
        } else if (!guide || addCart || isRecommendIntent(msg) || addFav || removeFav) {
            List<Book> found = bookMapper.selectPortalPage(keyword, categoryId, null, null, "default", 0, SEARCH_LIMIT);
            List<PortalAiBookCard> cards = toCards(found);
            if (!cards.isEmpty()) {
                result.setBooks(cards);
            } else if (isRecommendIntent(msg) || addCart || addFav) {
                result.setBooks(loadTopSellingCards(6));
            }
        } else if (isRecommendIntent(msg)) {
            result.setBooks(loadTopSellingCards(6));
        }

        List<PortalAiActionResult> actions = new ArrayList<>();
        String cartNote = processCartCommands(msg, ctx, result.getBooks(), actions);
        String favNote = processFavoriteCommands(msg, ctx, result.getBooks(), actions);
        String commandNote = mergeReply(cartNote, favNote);
        if (!actions.isEmpty()) {
            result.setActions(actions);
            refreshCartCount(result, ctx);
            refreshFavoriteMeta(result, ctx);
        }

        String context = buildCatalogContext(msg, keyword, categoryId, result.getBooks(), guide, ctx);

        String baseReply;
        if (aiProperties.isEnabled()) {
            try {
                baseReply = callModel(msg, history, context);
                result.setUsedAi(true);
            } catch (Exception e) {
                result.setHint(formatAiErrorHint(e));
                baseReply = guide ? buildGuideReply(msg) : buildFallbackReply(msg, result.getBooks(), ctx, favRec);
            }
        } else {
            result.setHint("未启用大模型（app.ai.enabled=false），以下为本地帮助说明。可在 application-local.yml 配置 DeepSeek 等接口。");
            baseReply = guide ? buildGuideReply(msg) : buildFallbackReply(msg, result.getBooks(), ctx, favRec);
        }

        if (viewCart && !ctx.loggedIn && (commandNote == null || commandNote.isEmpty())) {
            commandNote = guestCartLoginHint();
        }
        if ((viewFav || favRec || addFav) && !ctx.loggedIn && (commandNote == null || commandNote.isEmpty())) {
            commandNote = guestFavoriteLoginHint();
        }

        result.setReply(mergeReply(commandNote, baseReply));
        refreshFavoriteMeta(result, ctx);
        return result;
    }

    private static void applyUserMeta(PortalAiChatResult result, PortalUserContext ctx) {
        result.setLoggedIn(ctx.loggedIn);
        result.setGuest(!ctx.loggedIn);
        result.setUserNickname(ctx.displayName);
        if (ctx.loggedIn) {
            result.setCartCount(ctx.cartCount);
            result.setFavoriteCount(ctx.favoriteCount);
            result.setFavoriteBookIds(new ArrayList<>(ctx.favoriteBookIds));
        }
    }

    private void refreshFavoriteMeta(PortalAiChatResult result, PortalUserContext ctx) {
        if (!ctx.loggedIn) {
            return;
        }
        reloadFavoriteIds(ctx);
        result.setFavoriteCount(ctx.favoriteCount);
        result.setFavoriteBookIds(new ArrayList<>(ctx.favoriteBookIds));
    }

    private void reloadFavoriteIds(PortalUserContext ctx) {
        List<FavoriteBookRow> rows = userFavoriteService.listByCustomer(ctx.userId);
        ctx.favoriteBookIds = new ArrayList<>();
        for (FavoriteBookRow r : rows) {
            if (r != null && r.getId() != null) {
                ctx.favoriteBookIds.add(r.getId());
            }
        }
        ctx.favoriteCount = ctx.favoriteBookIds.size();
    }

    private void refreshCartCount(PortalAiChatResult result, PortalUserContext ctx) {
        if (ctx.loggedIn) {
            long count = userCartService.countItems(ctx.userId);
            result.setCartCount(count);
            ctx.cartCount = count;
        }
    }

    private static String mergeReply(String prefix, String body) {
        String p = prefix == null ? "" : prefix.trim();
        String b = body == null ? "" : body.trim();
        if (p.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return p;
        }
        if (b.contains(p) || p.contains(b)) {
            return b.length() >= p.length() ? b : p;
        }
        return p + "\n\n" + b;
    }

    private String processCartCommands(
            String msg,
            PortalUserContext ctx,
            List<PortalAiBookCard> candidates,
            List<PortalAiActionResult> actions) {
        StringBuilder note = new StringBuilder();

        if (isViewCartIntent(msg)) {
            if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("查看购物车"));
                note.append(guestCartLoginHint());
            } else {
                List<CartBookRow> rows = cartItemMapper.selectByCustomerId(ctx.userId);
                note.append(formatCartSummary(rows));
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("VIEW_CART");
                a.setSuccess(true);
                a.setMessage(rows == null || rows.isEmpty() ? "购物车为空" : "已列出购物车");
                a.setCartCount(userCartService.countItems(ctx.userId));
                actions.add(a);
            }
        }

        if (isAddToCartIntent(msg)) {
            Long bookId = resolveBookIdForCart(msg, candidates);
            if (bookId == null) {
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("ADD_TO_CART");
                a.setSuccess(false);
                a.setMessage("请说明书名（如《三体》），或说「把第一本加入购物车」");
                actions.add(a);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append("未能识别要加入购物车的图书。请先让我推荐或搜索，再说「把第一本加入购物车」。");
            } else if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("加入购物车"));
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append(guestCartLoginHint());
            } else {
                UserCartService.AddResult add = userCartService.addItem(ctx.userId, bookId, 1);
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("ADD_TO_CART");
                a.setSuccess(add.isSuccess());
                a.setMessage(add.getMessage());
                a.setBookId(add.getBookId());
                a.setBookTitle(add.getBookTitle());
                if (add.isSuccess()) {
                    a.setCartCount(add.getCartCount());
                }
                actions.add(a);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                if (add.isSuccess()) {
                    note.append("✓ 已将《").append(nullToEmpty(add.getBookTitle()))
                            .append("》加入您的购物车（共 ").append(add.getCartCount()).append(" 种商品）。");
                } else {
                    note.append(add.getMessage());
                }
            }
        }

        return note.toString();
    }

    private String processFavoriteCommands(
            String msg,
            PortalUserContext ctx,
            List<PortalAiBookCard> candidates,
            List<PortalAiActionResult> actions) {
        StringBuilder note = new StringBuilder();

        if (isViewFavoritesIntent(msg)) {
            if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("查看个人收藏"));
                note.append(guestFavoriteLoginHint());
            } else {
                List<FavoriteBookRow> rows = userFavoriteService.listByCustomer(ctx.userId);
                note.append(formatFavoritesSummary(rows));
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("VIEW_FAVORITES");
                a.setSuccess(true);
                a.setMessage(rows.isEmpty() ? "收藏夹为空" : "已列出收藏");
                a.setCartCount(ctx.favoriteCount);
                actions.add(a);
            }
        }

        if (isFavoriteBasedRecommendIntent(msg)) {
            if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("根据收藏推荐"));
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append(guestFavoriteLoginHint());
            } else if (ctx.favoriteCount <= 0) {
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append("您还没有收藏图书。可在图书详情页点击「收藏」，或先让我帮您找书后再收藏。");
            } else if (candidates != null && !candidates.isEmpty()) {
                String topCat = dominantCategoryName(ctx);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append("根据您的收藏");
                if (topCat != null && !topCat.isEmpty()) {
                    note.append("（偏爱「").append(topCat).append("」等分类）");
                }
                note.append("，为您在馆藏中挑选了 ").append(candidates.size()).append(" 本您可能感兴趣的书：");
            }
        }

        if (isAddFavoriteIntent(msg)) {
            Long bookId = resolveBookIdForCart(msg, candidates);
            if (bookId == null) {
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("ADD_TO_FAVORITE");
                a.setSuccess(false);
                a.setMessage("请说明书名或说「收藏第一本」");
                actions.add(a);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append("未能识别要收藏的图书。请先搜索或让我推荐，再说「收藏第一本」。");
            } else if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("加入收藏"));
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append(guestFavoriteLoginHint());
            } else {
                UserFavoriteService.FavoriteMutationResult add =
                        userFavoriteService.add(ctx.userId, bookId);
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("ADD_TO_FAVORITE");
                a.setSuccess(add.isSuccess());
                a.setMessage(add.getMessage());
                a.setBookId(add.getBookId());
                a.setBookTitle(add.getBookTitle());
                if (add.isSuccess()) {
                    a.setCartCount(add.getFavoriteCount());
                }
                actions.add(a);
                reloadFavoriteIds(ctx);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                if (add.isSuccess()) {
                    note.append("✓ 已将《").append(nullToEmpty(add.getBookTitle()))
                            .append("》加入您的收藏（共 ").append(add.getFavoriteCount()).append(" 册）。");
                } else {
                    note.append(add.getMessage());
                }
            }
        }

        if (isRemoveFavoriteIntent(msg)) {
            Long bookId = resolveBookIdForCart(msg, candidates);
            if (!ctx.loggedIn) {
                actions.add(actionLoginRequired("取消收藏"));
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append(guestFavoriteLoginHint());
            } else if (bookId == null) {
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append("请说明要取消收藏的书名，或说「取消收藏第一本」。");
            } else {
                UserFavoriteService.FavoriteMutationResult rm =
                        userFavoriteService.remove(ctx.userId, bookId);
                PortalAiActionResult a = new PortalAiActionResult();
                a.setType("REMOVE_FAVORITE");
                a.setSuccess(rm.isSuccess());
                a.setMessage(rm.getMessage());
                a.setBookId(rm.getBookId());
                a.setBookTitle(rm.getBookTitle());
                a.setCartCount(rm.getFavoriteCount());
                actions.add(a);
                reloadFavoriteIds(ctx);
                if (note.length() > 0) {
                    note.append("\n\n");
                }
                note.append(rm.isSuccess()
                        ? "已取消收藏《" + nullToEmpty(rm.getBookTitle()) + "》。"
                        : rm.getMessage());
            }
        }

        return note.toString();
    }

    private List<PortalAiBookCard> cardsFromFavorites(PortalUserContext ctx) {
        List<PortalAiBookCard> cards = new ArrayList<>();
        for (FavoriteBookRow r : userFavoriteService.listByCustomer(ctx.userId)) {
            PortalAiBookCard c = fromFavoriteRow(r);
            if (c != null) {
                cards.add(c);
            }
            if (cards.size() >= SEARCH_LIMIT) {
                break;
            }
        }
        return cards;
    }

    private static PortalAiBookCard fromFavoriteRow(FavoriteBookRow r) {
        if (r == null || r.getId() == null) {
            return null;
        }
        PortalAiBookCard c = new PortalAiBookCard();
        c.setId(r.getId());
        c.setTitle(r.getTitle());
        c.setAuthor(r.getAuthor());
        c.setPrice(r.getPrice());
        c.setCoverUrl(r.getCoverUrl());
        c.setCategoryName(r.getCategoryName());
        return c;
    }

    private List<PortalAiBookCard> recommendFromFavorites(PortalUserContext ctx) {
        List<FavoriteBookRow> favs = userFavoriteService.listByCustomer(ctx.userId);
        if (favs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> favIds = new HashSet<>();
        Map<Long, Integer> catVotes = new LinkedHashMap<>();
        String topAuthor = null;
        for (FavoriteBookRow f : favs) {
            if (f.getId() != null) {
                favIds.add(f.getId());
            }
            if (f.getCategoryId() != null) {
                catVotes.merge(f.getCategoryId(), 1, Integer::sum);
            }
            if (topAuthor == null && f.getAuthor() != null && !f.getAuthor().trim().isEmpty()) {
                topAuthor = f.getAuthor().trim();
            }
        }

        Long topCatId = null;
        int bestVotes = 0;
        for (Map.Entry<Long, Integer> e : catVotes.entrySet()) {
            if (e.getValue() > bestVotes) {
                bestVotes = e.getValue();
                topCatId = e.getKey();
            }
        }

        List<PortalAiBookCard> out = new ArrayList<>();
        if (topCatId != null) {
            appendRecommendCandidates(out, bookMapper.selectPortalPage(null, topCatId, null, null, "default", 0, 24), favIds);
        }
        if (out.size() < 4 && topAuthor != null) {
            appendRecommendCandidates(
                    out, bookMapper.selectPortalPage(topAuthor, null, null, null, "default", 0, 16), favIds);
        }
        if (out.isEmpty()) {
            appendRecommendCandidates(out, toBookList(loadTopSellingCards(8)), favIds);
        }
        return out.size() > SEARCH_LIMIT ? out.subList(0, SEARCH_LIMIT) : out;
    }

    private static void appendRecommendCandidates(List<PortalAiBookCard> out, List<Book> books, Set<Long> excludeIds) {
        if (books == null) {
            return;
        }
        for (Book b : books) {
            if (b == null || b.getId() == null || excludeIds.contains(b.getId())) {
                continue;
            }
            boolean dup = false;
            for (PortalAiBookCard existing : out) {
                if (b.getId().equals(existing.getId())) {
                    dup = true;
                    break;
                }
            }
            if (dup) {
                continue;
            }
            PortalAiBookCard c = new PortalAiBookCard();
            c.setId(b.getId());
            c.setTitle(b.getTitle());
            c.setAuthor(b.getAuthor());
            c.setPrice(b.getPrice());
            c.setCoverUrl(b.getCoverUrl());
            c.setCategoryName(b.getCategoryName());
            out.add(c);
            if (out.size() >= SEARCH_LIMIT) {
                return;
            }
        }
    }

    private static List<Book> toBookList(List<PortalAiBookCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Book> books = new ArrayList<>();
        for (PortalAiBookCard c : cards) {
            if (c == null || c.getId() == null) {
                continue;
            }
            Book b = new Book();
            b.setId(c.getId());
            b.setTitle(c.getTitle());
            b.setAuthor(c.getAuthor());
            b.setPrice(c.getPrice());
            b.setCoverUrl(c.getCoverUrl());
            b.setCategoryName(c.getCategoryName());
            books.add(b);
        }
        return books;
    }

    private String dominantCategoryName(PortalUserContext ctx) {
        List<FavoriteBookRow> favs = userFavoriteService.listByCustomer(ctx.userId);
        Map<String, Integer> votes = new LinkedHashMap<>();
        for (FavoriteBookRow f : favs) {
            if (f.getCategoryName() != null && !f.getCategoryName().trim().isEmpty()) {
                votes.merge(f.getCategoryName().trim(), 1, Integer::sum);
            }
        }
        String best = null;
        int max = 0;
        for (Map.Entry<String, Integer> e : votes.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private String formatFavoritesSummary(List<FavoriteBookRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "您的收藏夹目前是空的。在图书详情页点击「收藏」，或对我说「收藏第一本」。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("您的个人收藏（共 ").append(rows.size()).append(" 册）：\n\n");
        int i = 1;
        for (FavoriteBookRow r : rows) {
            if (r == null) {
                continue;
            }
            sb.append(i++).append(". 《").append(nullToEmpty(r.getTitle())).append("》");
            if (r.getAuthor() != null && !r.getAuthor().isEmpty()) {
                sb.append(" — ").append(r.getAuthor());
            }
            if (r.getCategoryName() != null && !r.getCategoryName().isEmpty()) {
                sb.append(" · ").append(r.getCategoryName());
            }
            sb.append("\n");
            if (i > 12) {
                sb.append("… 更多请前往「我的收藏」页面查看。\n");
                break;
            }
        }
        sb.append("\n可说「根据收藏推荐」获取相似书目；页头「收藏」可管理全部收藏。");
        return sb.toString();
    }

    private static String guestFavoriteLoginHint() {
        return "个人收藏需登录读者账号。请先登录后查看收藏、根据收藏推荐，或在对话中说「收藏第一本」。";
    }

    private static boolean isViewFavoritesIntent(String msg) {
        if (isGuideIntent(msg)) {
            return false;
        }
        return msg.contains("我的收藏") || msg.contains("查看收藏") || msg.contains("收藏夹")
                || msg.contains("收藏列表") || msg.contains("有哪些收藏") || msg.contains("我收藏了");
    }

    private static boolean isFavoriteBasedRecommendIntent(String msg) {
        if (isGuideIntent(msg)) {
            return false;
        }
        return msg.contains("根据收藏") || msg.contains("按收藏") || msg.contains("收藏推荐")
                || msg.contains("根据我的收藏") || msg.contains("像我的收藏") || msg.contains("收藏的书推荐")
                || (msg.contains("推荐") && msg.contains("收藏"));
    }

    private static boolean isAddFavoriteIntent(String msg) {
        if (isGuideIntent(msg) || isViewFavoritesIntent(msg)) {
            return false;
        }
        if (msg.contains("加入收藏") || msg.contains("添加收藏") || msg.contains("收藏这本")
                || msg.contains("收藏第一本") || msg.contains("加入我的收藏")) {
            return true;
        }
        return msg.contains("收藏") && (msg.contains("第一本") || msg.contains("第二本") || msg.contains("这本"))
                && !msg.contains("推荐") && !msg.contains("查看") && !msg.contains("取消");
    }

    private static boolean isRemoveFavoriteIntent(String msg) {
        return msg.contains("取消收藏") || msg.contains("移除收藏") || msg.contains("删掉收藏");
    }

    private PortalUserContext resolveUserContext(HttpSession session) {
        PortalUserContext ctx = new PortalUserContext();
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return ctx;
        }
        ctx.userId = ((Number) idObj).longValue();
        ctx.loggedIn = ctx.userId > 0;
        try {
            Customer c = customerMapper.selectById(ctx.userId);
            if (c != null) {
                String nick = c.getNickname();
                if (nick != null && !nick.trim().isEmpty()) {
                    ctx.displayName = nick.trim();
                } else if (c.getUsername() != null && !c.getUsername().trim().isEmpty()) {
                    ctx.displayName = c.getUsername().trim();
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        if (ctx.displayName == null || ctx.displayName.isEmpty()) {
            ctx.displayName = "读者";
        }
        if (ctx.loggedIn) {
            ctx.cartCount = userCartService.countItems(ctx.userId);
            reloadFavoriteIds(ctx);
        }
        return ctx;
    }

    private static PortalAiActionResult actionLoginRequired(String actionLabel) {
        PortalAiActionResult a = new PortalAiActionResult();
        a.setType("LOGIN_REQUIRED");
        a.setSuccess(false);
        a.setMessage("游客无法" + actionLabel + "，请先登录读者账号");
        return a;
    }

    private static String guestCartLoginHint() {
        return "您当前为游客模式，可先浏览与咨询找书。若要加入购物车或查看购物车，请先点击页头「登录」注册/登录读者账号。";
    }

    private String formatCartSummary(List<CartBookRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "您的购物车目前是空的。可以说「推荐几本热销图书」找书后再加入购物车。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("您的购物车（共 ").append(rows.size()).append(" 种）：\n\n");
        int i = 1;
        for (CartBookRow r : rows) {
            if (r == null) {
                continue;
            }
            sb.append(i++).append(". 《").append(nullToEmpty(r.getTitle())).append("》");
            if (r.getQuantity() != null && r.getQuantity() > 1) {
                sb.append(" ×").append(r.getQuantity());
            }
            if (r.getPrice() != null) {
                sb.append("，¥").append(r.getPrice().toPlainString());
            }
            sb.append("\n");
        }
        sb.append("\n可点击页头购物车图标进入结算。");
        return sb.toString();
    }

    private Long resolveBookIdForCart(String msg, List<PortalAiBookCard> candidates) {
        Matcher idM = BOOK_ID_PATTERN.matcher(msg);
        if (idM.find()) {
            try {
                return Long.parseLong(idM.group(1));
            } catch (NumberFormatException ignored) {
                /* fall through */
            }
        }

        Matcher titleM = BOOK_TITLE_PATTERN.matcher(msg);
        if (titleM.find()) {
            Long id = findBookIdByTitle(titleM.group(1));
            if (id != null) {
                return id;
            }
        }

        int ordinal = parseOrdinalIndex(msg);
        if (ordinal >= 0 && candidates != null && ordinal < candidates.size()) {
            return candidates.get(ordinal).getId();
        }

        if (candidates != null) {
            for (PortalAiBookCard c : candidates) {
                if (c == null || c.getTitle() == null || c.getId() == null) {
                    continue;
                }
                if (msg.contains(c.getTitle())) {
                    return c.getId();
                }
            }
            if (candidates.size() == 1 && candidates.get(0).getId() != null) {
                return candidates.get(0).getId();
            }
        }

        String stripped = msg.replaceAll("(加入|添加|放进|加购|购物车|购买|买|把|这本|那本|推荐|的|了|吧|请|帮|我)", "").trim();
        if (stripped.length() >= 2) {
            return findBookIdByTitle(stripped.length() > 20 ? stripped.substring(0, 20) : stripped);
        }
        return null;
    }

    private Long findBookIdByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        String kw = title.trim();
        List<Book> found = bookMapper.selectPortalPage(kw, null, null, null, "default", 0, 3);
        if (found == null || found.isEmpty()) {
            return null;
        }
        for (Book b : found) {
            if (b != null && b.getId() != null && b.getTitle() != null) {
                if (b.getTitle().contains(kw) || kw.contains(b.getTitle())) {
                    return b.getId();
                }
            }
        }
        return found.get(0).getId();
    }

    private static int parseOrdinalIndex(String msg) {
        if (msg.contains("第一本") || msg.contains("第一本书") || msg.contains("上面那本") || msg.contains("这本")) {
            return 0;
        }
        if (msg.contains("第二本") || msg.contains("第二本书")) {
            return 1;
        }
        if (msg.contains("第三本")) {
            return 2;
        }
        return -1;
    }

    private static boolean isAddToCartIntent(String msg) {
        if (isGuideIntent(msg)) {
            return false;
        }
        if (msg.contains("加入购物车") || msg.contains("添加购物车") || msg.contains("放进购物车")
                || msg.contains("加购") || msg.contains("加入购物车里")) {
            return true;
        }
        return (msg.contains("加入") || msg.contains("添加") || msg.contains("放进"))
                && msg.contains("购物车");
    }

    private static boolean isViewCartIntent(String msg) {
        if (isGuideIntent(msg)) {
            return false;
        }
        return msg.contains("我的购物车") || msg.contains("购物车里") || msg.contains("查看购物车")
                || msg.contains("购物车有什么") || "购物车".equals(msg.trim())
                || (msg.contains("购物车") && (msg.contains("看看") || msg.contains("列出") || msg.contains("有哪些")));
    }

    private static final class PortalUserContext {
        private boolean loggedIn;
        private Long userId;
        private String displayName;
        private long cartCount;
        private long favoriteCount;
        private List<Long> favoriteBookIds = new ArrayList<>();
    }

    private String callModel(
            String userMessage,
            List<OpenAiCompatibleClient.ChatMessage> history,
            String catalogContext) throws Exception {
        List<OpenAiCompatibleClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAiCompatibleClient.ChatMessage("system", systemPrompt() + "\n\n" + catalogContext));

        if (history != null) {
            int from = Math.max(0, history.size() - HISTORY_LIMIT);
            for (int i = from; i < history.size(); i++) {
                OpenAiCompatibleClient.ChatMessage h = history.get(i);
                if (h == null || h.content == null || h.content.trim().isEmpty()) {
                    continue;
                }
                String role = h.role == null ? "user" : h.role.trim().toLowerCase(Locale.ROOT);
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                messages.add(new OpenAiCompatibleClient.ChatMessage(role, h.content.trim()));
            }
        }

        messages.add(new OpenAiCompatibleClient.ChatMessage("user", userMessage));
        return aiClient.chat(messages);
    }

    private static String systemPrompt() {
        return "你是「馆藏书屋」门户端的智能阅读助手，负责帮助读者在馆藏中找书、做推荐并解答购书与阅读相关问题。\n"
                + "规则：\n"
                + "1. 优先依据系统提供的「馆藏检索上下文」回答，不要编造不存在的书名、价格或库存。\n"
                + "2. 推荐图书时说明书名、作者、价格（如有），并简要说明推荐理由。\n"
                + "3. 若上下文无匹配图书，诚实说明并建议用户换关键词或浏览「藏书分类」页面。\n"
                + "4. 可解答：如何搜索、分类浏览、购物车与下单流程、会员登录等（基于一般网上书城逻辑）。\n"
                + "5. 若用户已登录，可提示其说「把第一本加入购物车」「查看我的收藏」「根据收藏推荐」；游客需先登录才能加购与收藏。\n"
                + "6. 依据上下文中的「个人收藏」做个性化推荐时，说明与收藏分类/口味的关联。\n"
                + "7. 回答使用简体中文，语气亲切专业，条理清晰，适当使用列表。";
    }

    private String buildCatalogContext(
            String userMessage,
            String keyword,
            Long categoryId,
            List<PortalAiBookCard> books,
            boolean guideIntent,
            PortalUserContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("【馆藏检索上下文】\n");
        sb.append("用户问题：").append(userMessage).append("\n");
        if (ctx.loggedIn) {
            sb.append("读者状态：已登录（").append(ctx.displayName).append("），购物车约 ")
                    .append(ctx.cartCount).append(" 种商品，个人收藏 ")
                    .append(ctx.favoriteCount).append(" 册；可代为加购与收藏。\n");
            appendFavoritesContext(sb, ctx);
        } else {
            sb.append("读者状态：游客（未登录），仅可检索与说明；加购与收藏需先登录。\n");
        }
        if (guideIntent) {
            sb.append("问题类型：站点使用说明（非找书关键词，请勿强行按书名检索）。\n");
            sb.append("站点操作要点：顶部搜索框搜书名/作者；「藏书分类」浏览；登录后加购物车；「购物车」结算下单。\n");
        }
        if (keyword != null) {
            sb.append("检索关键词：").append(keyword).append("\n");
        }
        if (categoryId != null) {
            sb.append("限定分类 ID：").append(categoryId).append("\n");
        }

        try {
            List<PortalCategoryRow> cats = bookMapper.selectPortalCategoryStats();
            if (cats != null && !cats.isEmpty()) {
                sb.append("\n可选分类（名称 / 在架册数）：\n");
                int n = 0;
                for (PortalCategoryRow c : cats) {
                    if (c == null || c.getName() == null) {
                        continue;
                    }
                    sb.append("- ").append(c.getName());
                    if (c.getBookCount() != null) {
                        sb.append("（").append(c.getBookCount()).append(" 册）");
                    }
                    sb.append("\n");
                    if (++n >= 12) {
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
            /* 分类统计失败不影响主流程 */
        }

        if (books == null || books.isEmpty()) {
            sb.append("\n本次未检索到上架图书。\n");
        } else {
            sb.append("\n检索到的图书（最多 ").append(books.size()).append(" 本）：\n");
            for (PortalAiBookCard b : books) {
                sb.append("- ID=").append(b.getId())
                        .append(" 《").append(nullToEmpty(b.getTitle())).append("》")
                        .append(" 作者：").append(nullToEmpty(b.getAuthor()));
                if (b.getCategoryName() != null) {
                    sb.append(" 分类：").append(b.getCategoryName());
                }
                if (b.getPrice() != null) {
                    sb.append(" 价格：¥").append(b.getPrice().toPlainString());
                }
                if (b.getSoldQty() != null && b.getSoldQty() > 0) {
                    sb.append(" 销量：").append(b.getSoldQty());
                }
                sb.append("\n");
            }
        }

        List<PortalAiBookCard> hot = loadTopSellingCards(5);
        if (!hot.isEmpty()) {
            sb.append("\n近期热销（供推荐参考）：\n");
            for (PortalAiBookCard b : hot) {
                sb.append("- 《").append(nullToEmpty(b.getTitle())).append("》 作者：")
                        .append(nullToEmpty(b.getAuthor()));
                if (b.getSoldQty() != null) {
                    sb.append(" 销量：").append(b.getSoldQty());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private void appendFavoritesContext(StringBuilder sb, PortalUserContext ctx) {
        List<FavoriteBookRow> favs = userFavoriteService.listByCustomer(ctx.userId);
        if (favs.isEmpty()) {
            sb.append("\n个人收藏：暂无。可引导用户在详情页收藏或对话「收藏第一本」。\n");
            return;
        }
        sb.append("\n个人收藏（最近 ").append(Math.min(favs.size(), 8)).append(" 册，供推荐参考）：\n");
        int n = 0;
        for (FavoriteBookRow f : favs) {
            if (f == null || f.getTitle() == null) {
                continue;
            }
            sb.append("- ID=").append(f.getId()).append(" 《").append(f.getTitle()).append("》");
            if (f.getAuthor() != null) {
                sb.append(" 作者：").append(f.getAuthor());
            }
            if (f.getCategoryName() != null) {
                sb.append(" 分类：").append(f.getCategoryName());
            }
            sb.append("\n");
            if (++n >= 8) {
                break;
            }
        }
        String topCat = dominantCategoryName(ctx);
        if (topCat != null) {
            sb.append("收藏偏好分类：").append(topCat).append("\n");
        }
    }

    private static String formatAiErrorHint(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("402") || m.contains("Insufficient Balance") || m.contains("余额")) {
            return "DeepSeek 账户余额不足（402），请登录 https://platform.deepseek.com 充值后再试。以下为本地操作说明：";
        }
        if (m.contains("401") || m.contains("Unauthorized") || m.contains("authentication")) {
            return "API Key 无效或未授权（401），请检查 application-local.yml 中的 app.ai.api-key。以下为本地操作说明：";
        }
        if (m.contains("429")) {
            return "请求过于频繁（429），请稍后再试。以下为本地操作说明：";
        }
        return "大模型暂时不可用，以下为本地操作说明：";
    }

    /** 购书流程、搜索方式等说明（不依赖大模型） */
    private static String buildGuideReply(String msg) {
        boolean search = msg.contains("搜索") || msg.contains("查找") || msg.contains("找书");
        boolean buy = msg.contains("购买") || msg.contains("买") || msg.contains("下单") || msg.contains("结账")
                || msg.contains("购物车") || msg.contains("支付");
        boolean login = msg.contains("登录") || msg.contains("注册") || msg.contains("账号");

        if (search && buy) {
            return "在馆藏书屋搜索与购书可以按以下步骤操作：\n\n"
                    + "【搜索图书】\n"
                    + "1. 点击顶部导航「藏书分类」，或首页分类卡片进入书目列表。\n"
                    + "2. 使用页头搜索框输入书名、作者或 ISBN，回车即可筛选。\n"
                    + "3. 在列表页可按分类、价格排序进一步缩小范围。\n\n"
                    + "【购买下单】\n"
                    + "1. 点击图书进入详情页，选择数量后加入购物车。\n"
                    + "2. 右上角进入「购物车」，确认商品与金额。\n"
                    + "3. 点击结算；若未登录，系统会引导至用户登录/注册页。\n"
                    + "4. 填写收货信息并提交订单，可在「我的订单」查看进度。\n\n"
                    + "如需找具体书目，请直接告诉我书名或主题，例如「推荐文学类图书」。";
        }
        if (search) {
            return "搜索馆藏图书的方式：\n"
                    + "• 顶部搜索框：输入书名、作者等关键词后回车。\n"
                    + "• 藏书分类：从导航进入分类列表，支持筛选与排序。\n"
                    + "• 首页「新品上架」「热销版本」可快速浏览热门书目。\n"
                    + "告诉我您想找的主题，我也可以帮您检索并推荐。";
        }
        if (buy) {
            return "购书与下单流程：\n"
                    + "1. 在图书详情页将商品加入购物车。\n"
                    + "2. 打开「购物车」核对品种与数量。\n"
                    + "3. 登录读者账号后进入结算页提交订单。\n"
                    + "4. 在「个人中心 / 我的订单」查看订单状态。\n"
                    + "购物车前需先注册或登录用户账号（非管理员账号）。";
        }
        if (login) {
            return "读者账号说明：\n"
                    + "• 登录/注册：点击页头「登录」或购书时按提示跳转至用户登录页。\n"
                    + "• 管理员后台：页头「管理员入口」进入 /admin/login（与普通读者账号分开）。\n"
                    + "• 登录后可使用购物车、下单、收藏与个人中心等功能。";
        }
        return "馆藏书屋常用功能：\n"
                + "• 找书：顶部搜索、藏书分类、首页分类与榜单。\n"
                + "• 购书：详情页加购 → 购物车 → 登录后结算。\n"
                + "• 咨询：可直接输入书名、作者或「推荐××类图书」，我会帮您检索馆藏。\n"
                + "如需某一步的详细说明，请说明是「搜索」还是「购买」。";
    }

    private static boolean isGuideIntent(String msg) {
        if (msg.contains("购书流程") || msg.contains("使用说明") || msg.contains("怎么用") || msg.contains("如何使用")) {
            return true;
        }
        boolean how = msg.contains("如何") || msg.contains("怎么") || msg.contains("怎样");
        if (!how) {
            return false;
        }
        return msg.contains("搜索") || msg.contains("查找") || msg.contains("找书")
                || msg.contains("购买") || msg.contains("买") || msg.contains("下单")
                || msg.contains("购物车") || msg.contains("结账") || msg.contains("结算")
                || msg.contains("登录") || msg.contains("注册") || msg.contains("支付")
                || msg.contains("收藏") || msg.contains("订单");
    }

    private String buildFallbackReply(
            String userMessage, List<PortalAiBookCard> books, PortalUserContext ctx, boolean favoriteRecommend) {
        if (isGuideIntent(userMessage)) {
            return buildGuideReply(userMessage);
        }
        if (books != null && !books.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (favoriteRecommend && ctx.loggedIn) {
                String topCat = dominantCategoryName(ctx);
                sb.append("根据您的个人收藏");
                if (topCat != null) {
                    sb.append("（偏爱「").append(topCat).append("」等）");
                }
                sb.append("，为您推荐 ").append(books.size()).append(" 本馆藏图书：\n\n");
            } else {
                sb.append("根据「").append(userMessage).append("」，为您在馆藏中找到 ")
                        .append(books.size()).append(" 本相关图书：\n\n");
            }
            int i = 1;
            for (PortalAiBookCard b : books) {
                sb.append(i++).append(". 《").append(nullToEmpty(b.getTitle())).append("》");
                if (b.getAuthor() != null && !b.getAuthor().isEmpty()) {
                    sb.append(" — ").append(b.getAuthor());
                }
                if (b.getPrice() != null) {
                    sb.append("，¥").append(b.getPrice().toPlainString());
                }
                sb.append("\n");
            }
            sb.append("\n点击下方卡片可查看详情；也可前往「藏书分类」继续筛选。");
            if (ctx.loggedIn) {
                sb.append("\n可说「把第一本加入购物车」「收藏第一本」，或点卡片按钮操作。");
            } else {
                sb.append("\n游客可先浏览；登录后可使用购物车、收藏与下单。");
            }
            return sb.toString();
        }
        if (favoriteRecommend && ctx.loggedIn && ctx.favoriteCount <= 0) {
            return "您还没有收藏图书，暂时无法按收藏推荐。可先让我找书，在详情页收藏后再说「根据收藏推荐」。";
        }
        if (isRecommendIntent(userMessage)) {
            return "暂未检索到精确匹配。您可以浏览首页「热销版本」或在顶部搜索框输入书名/作者。如需智能解读，请在配置中启用 app.ai 并连接 Ollama 等开源模型。";
        }
        return "未在馆藏中找到与「" + userMessage + "」高度匹配的上架图书。建议换用书名、作者或更短的关键词重试，或浏览「藏书分类」。";
    }

    private static boolean isRecommendIntent(String msg) {
        String m = msg.toLowerCase(Locale.ROOT);
        return m.contains("推荐") || m.contains("热销") || m.contains("畅销") || m.contains("好书")
                || m.contains("有什么书") || m.contains("买什么");
    }

    private Long resolveCategoryId(String msg) {
        try {
            List<PortalCategoryRow> cats = bookMapper.selectPortalCategoryStats();
            if (cats == null) {
                return null;
            }
            for (PortalCategoryRow c : cats) {
                if (c == null || c.getName() == null) {
                    continue;
                }
                if (msg.contains(c.getName())) {
                    return c.getId();
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return null;
    }

    private static String buildSearchKeyword(String msg, Long categoryId) {
        if (categoryId != null) {
            String cleaned = msg.replaceAll("(推荐|有什么|哪些|图书|书籍|相关|关于|的|吗|呢|？|\\?)", "").trim();
            return cleaned.length() >= 2 ? cleaned : null;
        }
        return msg.length() > 40 ? msg.substring(0, 40) : msg;
    }

    private List<PortalAiBookCard> loadTopSellingCards(int limit) {
        try {
            List<DashboardTopBookRow> rows = orderItemMapper.selectTopSellingBooks(limit);
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<PortalAiBookCard> cards = new ArrayList<>();
            for (DashboardTopBookRow r : rows) {
                if (r == null || r.getBookId() == null) {
                    continue;
                }
                PortalAiBookCard c = new PortalAiBookCard();
                c.setId(r.getBookId());
                c.setTitle(r.getTitle());
                c.setAuthor(r.getAuthor());
                c.setPrice(r.getPrice());
                c.setCoverUrl(r.getCoverUrl());
                c.setCategoryName(r.getCategoryName());
                c.setSoldQty(r.getSoldQty());
                cards.add(c);
            }
            return cards;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<PortalAiBookCard> toCards(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }
        List<PortalAiBookCard> cards = new ArrayList<>();
        for (Book b : books) {
            if (b == null || b.getId() == null) {
                continue;
            }
            PortalAiBookCard c = new PortalAiBookCard();
            c.setId(b.getId());
            c.setTitle(b.getTitle());
            c.setAuthor(b.getAuthor());
            c.setPrice(b.getPrice());
            c.setCoverUrl(b.getCoverUrl());
            c.setCategoryName(b.getCategoryName());
            cards.add(c);
        }
        return cards;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

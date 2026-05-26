package com.bookshop.admin.service;

import com.bookshop.admin.config.AiProperties;
import com.bookshop.admin.dto.BookAlertCategoryStat;
import com.bookshop.admin.dto.DashboardTopBookRow;
import com.bookshop.admin.service.ai.OpenAiCompatibleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminBookAlertAiService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookAlertAiService.class);

    private final AiProperties aiProperties;
    private final OpenAiCompatibleClient aiClient;

    public AdminBookAlertAiService(AiProperties aiProperties, OpenAiCompatibleClient aiClient) {
        this.aiProperties = aiProperties;
        this.aiClient = aiClient;
    }

    public Map<String, Object> analyze(Map<String, Object> report) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiEnabled", aiProperties.isEnabled());
        result.put("model", aiProperties.getModel());

        String prompt = buildPrompt(report);
        if (!aiProperties.isEnabled()) {
            result.put("analysis", buildRuleBasedAnalysis(report));
            result.put("hint", "未启用大模型（app.ai.enabled=false），以下为规则引擎生成的经营建议。可在 application-local.yml 配置 DeepSeek 等接口。");
            return result;
        }

        try {
            List<OpenAiCompatibleClient.ChatMessage> messages = new ArrayList<>();
            messages.add(new OpenAiCompatibleClient.ChatMessage(
                    "system",
                    "你是网上书城「馆藏书屋」的库存与品类经营顾问。根据提供的统计数据，用中文给出简洁、可执行的建议。"
                            + "分四段：①售空/低库存补货 ②畅销书加印/推广 ③滞销减量 ④建议新增品类。"
                            + "每段 2-4 条要点，不要编造未提供的数据。"));
            messages.add(new OpenAiCompatibleClient.ChatMessage("user", prompt));
            String text = aiClient.chat(messages);
            result.put("analysis", text);
            result.put("hint", null);
        } catch (Exception e) {
            log.warn("admin book alert ai failed: {}", e.getMessage());
            result.put("analysis", buildRuleBasedAnalysis(report));
            result.put("hint", friendlyAiError(e) + " 已回退为规则分析。");
        }
        return result;
    }

    public String analyzeRuleOnly(Map<String, Object> report) {
        return buildRuleBasedAnalysis(report);
    }

    @SuppressWarnings("unchecked")
    private static String buildPrompt(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        sb.append("日期：").append(report.get("dateLabel")).append("\n\n");

        Map<String, Long> counts = (Map<String, Long>) report.get("counts");
        if (counts != null) {
            sb.append("【概览】售空 ").append(counts.get("outOfStock"))
                    .append(" 种，低库存 ").append(counts.get("lowStock"))
                    .append(" 种，畅销 ").append(counts.get("hotSelling"))
                    .append(" 种，建议减量 ").append(counts.get("reduceCandidates"))
                    .append(" 种\n\n");
        }

        appendBookList(sb, "售空图书", (List<DashboardTopBookRow>) report.get("outOfStock"));
        appendBookList(sb, "低库存图书", (List<DashboardTopBookRow>) report.get("lowStock"));
        appendBookList(sb, "近30天畅销", (List<DashboardTopBookRow>) report.get("hotSelling"));
        appendBookList(sb, "建议减量（库存高、销量低）", (List<DashboardTopBookRow>) report.get("reduceCandidates"));

        List<BookAlertCategoryStat> suggest = (List<BookAlertCategoryStat>) report.get("suggestAddCategories");
        if (suggest != null && !suggest.isEmpty()) {
            sb.append("【建议扩充品类】\n");
            for (BookAlertCategoryStat c : suggest) {
                sb.append("- ").append(c.getCategoryName())
                        .append("：在售 ").append(c.getBookCount())
                        .append(" 种，已售 ").append(c.getSoldQty()).append(" 册\n");
            }
        }

        return sb.toString();
    }

    private static void appendBookList(StringBuilder sb, String title, List<DashboardTopBookRow> list) {
        sb.append("【").append(title).append("】\n");
        if (list == null || list.isEmpty()) {
            sb.append("（无）\n\n");
            return;
        }
        int n = 0;
        for (DashboardTopBookRow b : list) {
            sb.append("- ").append(b.getTitle())
                    .append(" | 分类:").append(b.getCategoryName() != null ? b.getCategoryName() : "—")
                    .append(" | 库存:").append(b.getStock())
                    .append(" | 销量:").append(b.getSoldQty()).append("\n");
            if (++n >= 10) {
                break;
            }
        }
        sb.append("\n");
    }

    @SuppressWarnings("unchecked")
    private static String buildRuleBasedAnalysis(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        Map<String, Long> counts = (Map<String, Long>) report.get("counts");
        long out = counts != null && counts.get("outOfStock") != null ? counts.get("outOfStock") : 0L;
        long low = counts != null && counts.get("lowStock") != null ? counts.get("lowStock") : 0L;

        sb.append("📦 售空与补货\n");
        if (out > 0) {
            sb.append("• 当前有 ").append(out).append(" 种在售图书已售空，请优先补货或暂时下架，避免读者下单失败。\n");
        } else {
            sb.append("• 暂无售空图书，请继续保持库存巡检。\n");
        }
        if (low > 0) {
            sb.append("• ").append(low).append(" 种图书库存低于 10 册，建议安排补货。\n");
        }

        sb.append("\n🔥 畅销与大卖\n");
        List<DashboardTopBookRow> hot = (List<DashboardTopBookRow>) report.get("hotSelling");
        if (hot != null && !hot.isEmpty()) {
            sb.append("• 近阶段畅销：");
            for (int i = 0; i < Math.min(3, hot.size()); i++) {
                if (i > 0) {
                    sb.append("、");
                }
                sb.append("《").append(hot.get(i).getTitle()).append("》");
            }
            sb.append("，可考虑首页推荐或适度加印。\n");
        } else {
            sb.append("• 暂无明显畅销品，可结合分类活动做促销引流。\n");
        }

        sb.append("\n📉 建议减量\n");
        List<DashboardTopBookRow> reduce = (List<DashboardTopBookRow>) report.get("reduceCandidates");
        if (reduce != null && !reduce.isEmpty()) {
            sb.append("• ").append(reduce.size()).append(" 种图书库存较高但销量偏低，可减少进货或参与清仓，释放仓储与资金。\n");
        } else {
            sb.append("• 未发现明显滞销积压，库存结构较健康。\n");
        }

        sb.append("\n➕ 品类扩充\n");
        List<BookAlertCategoryStat> suggest = (List<BookAlertCategoryStat>) report.get("suggestAddCategories");
        if (suggest != null && !suggest.isEmpty()) {
            for (BookAlertCategoryStat c : suggest) {
                sb.append("• 「").append(c.getCategoryName())
                        .append("」销量 ").append(c.getSoldQty())
                        .append(" 册但仅有 ").append(c.getBookCount())
                        .append(" 种在售，建议丰富该分类书目。\n");
            }
        } else {
            sb.append("• 各分类书目较为均衡，可继续关注读者留言与搜索热词。\n");
        }

        return sb.toString().trim();
    }

    private static String friendlyAiError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (msg.contains("402")) {
            return "DeepSeek 账户余额不足（402）。";
        }
        if (msg.contains("401")) {
            return "API Key 无效（401）。";
        }
        return "AI 调用失败：" + msg;
    }
}

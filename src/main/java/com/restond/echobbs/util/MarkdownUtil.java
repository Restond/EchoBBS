package com.restond.echobbs.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Markdown 渲染与 HTML 消毒工具类。
 * <p>
 * 使用 commonmark 将 Markdown 解析为 HTML，再通过 jsoup 的 {@link Safelist} 进行白名单消毒，
 * 移除危险标签（如 script、iframe）和属性（如 onclick），防御存储型 XSS 攻击。
 * <p>
 * 允许的标签：基础排版 + 标题 + 代码块 + 表格 + 图片 + 删除线。
 */
public class MarkdownUtil {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    public static String renderAndSanitize(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Node document = PARSER.parse(markdown);
        String html = RENDERER.render(document);

        Safelist safelist = Safelist.basicWithImages()
                .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                .addTags("pre", "code")
                .addAttributes("pre", "class")
                .addAttributes("code", "class")
                .addTags("table", "thead", "tbody", "tr", "th", "td")
                .addTags("del");

        return Jsoup.clean(html, safelist);
    }
}

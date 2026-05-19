package com.bookshop.admin.util;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/** 纯 Java 生成 SVG 验证码，避免 AWT 在 headless 环境渲染失败 */
public final class CaptchaUtil {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LEN = 4;
    private static final int W = 112;
    private static final int H = 40;

    private CaptchaUtil() {
    }

    public static String randomCode() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder(LEN);
        for (int i = 0; i < LEN; i++) {
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static byte[] svgBytes(String code) {
        Random rnd = new Random();
        StringBuilder svg = new StringBuilder(1024);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(W).append("\" height=\"").append(H).append("\" viewBox=\"0 0 ").append(W).append(" ").append(H).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#f5f3f0\"/>");
        for (int i = 0; i < 8; i++) {
            int x1 = rnd.nextInt(W);
            int y1 = rnd.nextInt(H);
            int x2 = rnd.nextInt(W);
            int y2 = rnd.nextInt(H);
            svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                    .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                    .append("\" stroke=\"#c9bfb4\" stroke-width=\"1\"/>");
        }
        for (int i = 0; i < code.length(); i++) {
            int x = 14 + i * 24 + rnd.nextInt(4);
            int y = 28 + rnd.nextInt(6);
            int rotate = -20 + rnd.nextInt(41);
            svg.append("<text x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" fill=\"#4a3f36\" font-family=\"Arial, sans-serif\" font-size=\"24\" font-weight=\"700\"")
                    .append(" transform=\"rotate(").append(rotate).append(" ").append(x).append(" ").append(y).append(")\">")
                    .append(code.charAt(i)).append("</text>");
        }
        for (int i = 0; i < 30; i++) {
            int x = rnd.nextInt(W);
            int y = rnd.nextInt(H);
            svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y).append("\" r=\"1\" fill=\"#b8aea5\"/>");
        }
        svg.append("</svg>");
        return svg.toString().getBytes(StandardCharsets.UTF_8);
    }
}

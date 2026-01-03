package com.acme.herald.comment;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class CommentsTipTapJiraWikiRenderer {
    private CommentsTipTapJiraWikiRenderer() {}

    public static String render(Object tiptapDoc, String plainFallback) {
        if (!(tiptapDoc instanceof Map<?, ?> root)) {
            return plainFallback != null ? plainFallback : "";
        }
        StringBuilder sb = new StringBuilder(256);
        renderNode((Map<String, Object>) root, sb);
        String out = sb.toString().trim();
        return out.isBlank() ? (plainFallback != null ? plainFallback : "") : out;
    }

    private static void renderNode(Map<String, Object> node, StringBuilder sb) {
        String type = asString(node.get("type"));

        switch (type) {
            case "doc" -> renderChildren(node, sb);
            case "paragraph" -> {
                renderChildren(node, sb);
                sb.append("\n\n");
            }
            case "text" -> {
                String text = asString(node.get("text"));
                if (text == null) return;
                sb.append(applyMarks(text, node));
            }
            case "hardBreak" -> sb.append("\n");
            case "mention" -> {
                Map<String, Object> attrs = (Map<String, Object>) node.get("attrs");
                String key = attrs != null ? firstNonBlank(
                        asString(attrs.get("jiraUserKey")),
                        asString(attrs.get("id")),
                        asString(attrs.get("username"))
                ) : null;

                if (key == null || key.isBlank()) {
                    String label = attrs != null ? asString(attrs.get("label")) : null;
                    sb.append("@").append(escapePlain(label != null ? label : "user"));
                } else {
                    sb.append("[~").append(escapeMentionKey(key)).append("]");
                }
            }
            default -> renderChildren(node, sb); // kontenery/nieobsługiwane
        }
    }

    private static void renderChildren(Map<String, Object> node, StringBuilder sb) {
        Object content = node.get("content");
        if (!(content instanceof List<?> list)) return;
        for (Object child : list) {
            if (child instanceof Map<?, ?> ch) {
                renderNode((Map<String, Object>) ch, sb);
            }
        }
    }

    private static String applyMarks(String raw, Map<String, Object> node) {
        String text = escapePlain(raw);

        Object marksObj = node.get("marks");
        if (!(marksObj instanceof List<?> marks)) return text;

        boolean bold = false, italic = false, code = false;
        String link = null;

        for (Object m : marks) {
            if (!(m instanceof Map<?, ?> mm)) continue;
            String t = asString(mm.get("type"));
            if ("bold".equals(t)) bold = true;
            if ("italic".equals(t)) italic = true;
            if ("code".equals(t)) code = true;
            if ("link".equals(t)) {
                Object attrsObj = mm.get("attrs");
                if (attrsObj instanceof Map<?, ?> a) link = asString(a.get("href"));
            }
        }

        if (code) text = "{{" + text + "}}";
        if (bold) text = "*" + text + "*";
        if (italic) text = "_" + text + "_";
        if (link != null && !link.isBlank()) {
            text = "[" + text + "|" + escapeLink(link) + "]";
        }
        return text;
    }

    private static String escapePlain(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("|", "\\|");
    }

    private static String escapeLink(String s) {
        return s.trim()
                .replace("[", "%5B")
                .replace("]", "%5D");
    }

    private static String escapeMentionKey(String s) {
        // minimalnie – żeby nie popsuć wiki
        return s.trim().replace("]", "").replace("[", "");
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... xs) {
        if (xs == null) return null;
        for (String x : xs) {
            if (x != null && !x.isBlank()) return x;
        }
        return null;
    }
}

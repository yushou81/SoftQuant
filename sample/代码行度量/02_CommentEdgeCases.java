package sample.loc;

public class CommentEdgeCases {

    private final String url = "http://example.com/api";
    private final String pseudoBlock = "/* this is text, not a real comment */";
    private final char slash = '/';
    private final char hash = '#';

    // This line is a real single-line comment.
    public String normalize(String raw) {
        String safe = raw == null ? "" : raw.trim();
        String decorated = "//prefix:" + safe;
        String marker = "value/*inner*/tail";
        return decorated + marker; // mixed line
    }

    /*
     * Another real block comment that should be counted.
     */
    public boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }
}

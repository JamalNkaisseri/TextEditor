package com.texteditor.ui;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides Python syntax highlighting functionality for a CodeArea.
 */
public class PythonSyntaxHighlighter {

    // Define token patterns for Python syntax elements
    private static final String[] KEYWORDS = {
            "and", "as", "assert", "break", "class", "continue", "def", "del", "elif",
            "else", "except", "False", "finally", "for", "from", "global", "if", "import",
            "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise",
            "return", "True", "try", "while", "with", "yield"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String FUNCTION_PATTERN = "\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()";
    private static final String CLASS_PATTERN = "\\bclass\\s+([a-zA-Z_][a-zA-Z0-9_]*)";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "#[^\n]*";
    private static final String DECORATOR_PATTERN = "@[a-zA-Z_][a-zA-Z0-9_]*";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b";

    // Combine all patterns
    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<FUNCTION>" + FUNCTION_PATTERN + ")"
                    + "|(?<CLASS>" + CLASS_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<DECORATOR>" + DECORATOR_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
    );

    /**
     * Apply this syntax highlighter to a CodeArea.
     *
     * @param codeArea The CodeArea to apply highlighting to
     */
    public void applyHighlighting(CodeArea codeArea) {
        // Listen for text changes and compute highlighting
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(100))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                });
    }

    /**
     * Compute the style spans for syntax highlighting.
     *
     * @param text The text to analyze
     * @return StyleSpans for the text with appropriate styling
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword"
                            : matcher.group("FUNCTION") != null ? "function"
                            : matcher.group("CLASS") != null ? "class"
                            : matcher.group("STRING") != null ? "string"
                            : matcher.group("COMMENT") != null ? "comment"
                            : matcher.group("DECORATOR") != null ? "decorator"
                            : matcher.group("NUMBER") != null ? "number"
                            : null;

            assert styleClass != null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);

        return spansBuilder.create();
    }
}
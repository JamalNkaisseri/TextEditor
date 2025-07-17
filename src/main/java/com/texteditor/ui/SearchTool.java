package com.texteditor.ui;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;

public class SearchTool {

    private final CodeArea codeArea;
    private final TextField searchField;
    private final List<Integer> matchIndices = new ArrayList<>();
    private String lastQuery = "";
    private int currentIndex = -1;

    public SearchTool(CodeArea codeArea, TextField searchField) {
        this.codeArea = codeArea;
        this.searchField = searchField;

        searchField.textProperty().addListener((obs, old, val) -> {
            highlightAll(val);
            currentIndex = -1;
        });
    }

    public void highlightAll(String query) {
        String text = codeArea.getText();
        codeArea.clearStyle(0, text.length());
        matchIndices.clear();

        if (query == null || query.isEmpty()) return;

        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            matchIndices.add(matcher.start());
            codeArea.setStyle(matcher.start(), matcher.end(), Collections.singleton("search-highlight"));
        }

        lastQuery = query;
    }

    public void findNext() {
        if (matchIndices.isEmpty()) return;

        currentIndex = (currentIndex + 1) % matchIndices.size();
        scrollToMatch(matchIndices.get(currentIndex));
    }

    public void findPrevious() {
        if (matchIndices.isEmpty()) return;

        currentIndex = (currentIndex - 1 + matchIndices.size()) % matchIndices.size();
        scrollToMatch(matchIndices.get(currentIndex));
    }

    private void scrollToMatch(int start) {
        int end = start + lastQuery.length();

        int paragraph = codeArea.offsetToPosition(start, Forward).getMajor();
        int column = codeArea.offsetToPosition(start, Forward).getMinor();

        codeArea.moveTo(paragraph, column);
        codeArea.selectRange(start, end);
        Platform.runLater(() -> codeArea.requestFollowCaret());
    }
}

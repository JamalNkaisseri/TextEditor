import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;

import java.util.*;
import java.util.regex.Pattern;

public class AutoStyler {

    private static final Pattern MAIN_HEADER = Pattern.compile("^[A-Z].+:\\s*$");
    private static final Pattern TITLE_LINE = Pattern.compile("^[A-Z][^:]+$");
    // Fixed: More flexible list item pattern
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*[a-z]\\)\\s*.*");

    private static final long DEBOUNCE_DELAY_MS = 300;
    private Timer debounceTimer;

    public void bindTo(CodeArea codeArea) {
        // Initial style application
        applyStyling(codeArea);

        // Debounced styling on text change
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (debounceTimer != null) debounceTimer.cancel();

            debounceTimer = new Timer();
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> applyStyling(codeArea));
                }
            }, DEBOUNCE_DELAY_MS);
        });
    }

    public void applyStyling(CodeArea codeArea) {
        String text = codeArea.getText();
        String[] lines = text.split("\n", -1);
        codeArea.clearStyle(0, text.length());

        int index = 0;

        for (String line : lines) {
            Collection<String> styles = new ArrayList<>();
            String trimmed = line.trim();

            if (MAIN_HEADER.matcher(trimmed).matches()) {
                styles.add("main-header");
            } else if (TITLE_LINE.matcher(trimmed).matches()) {
                styles.add("note-title");
            } else if (LIST_ITEM.matcher(trimmed).matches()) {
                styles.add("list-item");
            }

            if (!styles.isEmpty()) {
                codeArea.setStyle(index, index + line.length(), styles);
            }

            index += line.length() + 1;
        }
    }
}
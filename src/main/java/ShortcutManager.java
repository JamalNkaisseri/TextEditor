import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

public class ShortcutManager {

    private final Map<KeyCombination, Runnable> shortcuts = new HashMap<>();

    public void register(KeyCombination combo, Runnable action) {
        shortcuts.put(combo, action);
    }

    public void attachTo(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
                if (entry.getKey().match(event)) {
                    entry.getValue().run();
                    event.consume();
                    break;
                }
            }
        });
    }

    // ---------- Shortcut Constants ----------

    public static final KeyCombination NEW_TAB = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination OPEN_FILE = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SAVE_FILE = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SAVE_AS_FILE = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

    public static final KeyCombination CUT = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination PASTE = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SHOW_CLIPBOARD_HISTORY = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination CLOSE_TAB = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination EXIT_APP = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);

    // Font size
    public static final KeyCombination INCREASE_FONT_PLUS = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination INCREASE_FONT_EQUALS = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination DECREASE_FONT = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination RESET_FONT = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
}

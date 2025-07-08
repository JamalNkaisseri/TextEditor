import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TabManager {

    private final TabPane tabPane = new TabPane();
    private final Map<Tab, CodeArea> codeAreas = new HashMap<>();
    private final Set<Integer> usedNumbers = new HashSet<>();

    public TabManager(Scene scene) {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        createNewTab();

        // Shortcuts
        ShortcutManager shortcutManager = new ShortcutManager();
        shortcutManager.register(ShortcutManager.NEW_TAB, this::createNewTab);
        shortcutManager.register(ShortcutManager.CLOSE_TAB, this::closeCurrentTab);
        shortcutManager.attachTo(scene);
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public CodeArea getCurrentCodeArea() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        return selected != null ? codeAreas.get(selected) : null;
    }

    public void createNewTab() {
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        // Start with dirty mark since it's unsaved
        String tabTitle = generateTabTitle();
        Tab tab = new Tab("*" + tabTitle); // <-- add the asterisk

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(scrollPane);

        codeAreas.put(tab, codeArea);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // On close, remove from tracking
        tab.setOnClosed(e -> {
            int num = extractUntitledNumber(tab.getText().replace("*", ""));
            usedNumbers.remove(num);
            codeAreas.remove(tab);
        });
    }


    private String generateTabTitle() {
        // First tab is "Untitled"
        if (!usedNumbers.contains(0)) {
            usedNumbers.add(0);
            return "Untitled";
        }

        // Find next available number
        int n = 1;
        while (usedNumbers.contains(n)) n++;
        usedNumbers.add(n);
        return "Untitled " + n;
    }

    private int extractUntitledNumber(String title) {
        if (title.equals("Untitled")) return 0;
        if (title.startsWith("Untitled ")) {
            try {
                return Integer.parseInt(title.substring(9).trim());
            } catch (NumberFormatException ignored) {}
        }
        return -1; // not a tracked tab
    }

    public void closeCurrentTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.getTabPane().getTabs().remove(selected); // this will trigger setOnClosed
        }
    }

    public Tab createNewTabWithContent(String content, String title) {
        CodeArea codeArea = new CodeArea();
        codeArea.replaceText(content);

        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        Tab tab = new Tab(title);
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(scrollPane);

        codeAreas.put(tab, codeArea);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        return tab;
    }

}

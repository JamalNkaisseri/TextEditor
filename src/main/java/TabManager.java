import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.HashMap;
import java.util.Map;

public class TabManager {

    private final TabPane tabPane = new TabPane();
    private final Map<Tab, CodeArea> codeAreas = new HashMap<>();

    public TabManager() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        createNewTab(); // Create initial tab
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

        // Line numbers
        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        Tab tab = new Tab("Untitled");
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(scrollPane);

        codeAreas.put(tab, codeArea);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }
}

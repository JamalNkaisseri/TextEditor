import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class SearchBar extends HBox {

    private final TextField searchField = new TextField();
    private final Button nextBtn = new Button("Next");
    private final Button prevBtn = new Button("Prev");
    private final Region spacer = new Region();
    private Runnable onClose;

    public SearchBar() {
        setSpacing(10);
        setPadding(new Insets(5));
        setStyle("-fx-background-color: #1E1E2E; -fx-border-color: #444; -fx-border-width: 1;");

        searchField.setPromptText("Search...");
        searchField.setStyle("-fx-background-color: #222; -fx-text-fill: #DDD; -fx-prompt-text-fill: #888;");

        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(searchField, nextBtn, prevBtn, spacer);

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && onClose != null) onClose.run();
        });
    }

    public TextField getSearchField() {
        return searchField;
    }

    public Button getNextButton() {
        return nextBtn;
    }

    public Button getPrevButton() {
        return prevBtn;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void focusField() {
        searchField.requestFocus();
        searchField.selectAll();
    }
}

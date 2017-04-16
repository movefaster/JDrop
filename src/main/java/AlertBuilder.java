import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Morton on 4/16/17.
 */
public class AlertBuilder {
    private Alert alert;
    private Consumer<Void> positiveCallback;
    private Consumer<Void> negativeCallback;
    private ButtonType negativeButton;
    public AlertBuilder(Alert.AlertType type) {
        alert = new Alert(type);
    }
    public AlertBuilder setTitle(String title) {
        alert.setTitle(title);
        return this;
    }
    public AlertBuilder setMessage(String msg) {
        alert.setContentText(msg);
        return this;
    }
    public AlertBuilder setPositive(Consumer<Void> callback) {
        this.positiveCallback = callback;
        return this;
    }
    public AlertBuilder setPositive(String text, Consumer<Void> callback) {
        this.positiveCallback = callback;
        ButtonType positiveButton = new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().removeIf(type -> type == ButtonType.OK || type == ButtonType.YES);
        alert.getButtonTypes().add(positiveButton);
        return this;
    }
    public AlertBuilder setNegative(String text, Consumer<Void> callback) {
        this.negativeCallback = callback;
        ButtonType negativeButton = new ButtonType(text, ButtonBar.ButtonData.NO);
        alert.getButtonTypes().add(negativeButton);
        return this;
    }
    public AlertBuilder setNegative(Consumer<Void> callback) {
        this.negativeCallback = callback;
        return this;
    }
    public AlertBuilder setOwner(Window ownerWindow) {
        alert.initOwner(ownerWindow);
        return this;
    }
    public AlertBuilder setModality(Modality modality) {
        alert.initModality(modality);
        return this;
    }
    public AlertBuilder addExpandableContent(Node node) {
        alert.getDialogPane().setExpandableContent(node);
        alert.getDialogPane().setExpanded(true);
        return this;
    }
    public AlertBuilder addTextArea(String text) {
        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane pane = new GridPane();
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.add(textArea, 0, 0);
        alert.getDialogPane().setExpandableContent(pane);
        alert.getDialogPane().setExpanded(true);
        return this;
    }
    public AlertBuilder addCustomPane(Parent root) {
        alert.getDialogPane().setContent(root);
        return this;
    }
    public AlertBuilder setHeaderText(String text) {
        alert.setHeaderText(text);
        return this;
    }
    public Alert get() {
        return alert;
    }
    public void showAndWait() {
        Optional<ButtonType> response = alert.showAndWait();
        if (response != null && response.isPresent()) {
            if (response.get().getButtonData() == ButtonBar.ButtonData.OK_DONE
                    || response.get() == ButtonType.OK || response.get() == ButtonType.YES) {
                if (positiveCallback != null) positiveCallback.accept(null);
            } else {
                if (negativeCallback != null) negativeCallback.accept(null);
            }
        }
    }
    public void show() {
        alert.show();
    }
}

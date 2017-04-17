/*
 * Copyright [2017] [Morton Mo]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.fxmisc.easybind.EasyBind;

import java.io.*;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller implements Initializable {

    @FXML
    Button sendButton;
    @FXML
    Button exitButton;
    @FXML
    Button browseButton;
    @FXML
    TextField recipientAddressText;
    @FXML
    TextField currentAddressText;
    @FXML
    TextArea msgTextArea;
    @FXML
    TextField fileText;
    @FXML
    Label codeLabel;
    @FXML
    TextField codeText;

    private static final Logger LOG = Logger.getGlobal();
    private static final int INVALID = -1;
    private static final int TEXT = 0;
    private static final int FILE = 1;

    private Stage primaryStage;
    private ObjectProperty<File> file;
    private Server server;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        server = new Server(this::onServerError);
        codeLabel.textProperty().bind(server.codeProperty());
        file = new SimpleObjectProperty<>();
        file.addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) msgTextArea.setText("");
        }));

        currentAddressText.setEditable(false);
        currentAddressText.setText(getCurrentIp());

        // when lose focus validate IP
        recipientAddressText.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue) {
                InetAddressValidator validator = InetAddressValidator.getInstance();
                String addr = recipientAddressText.getText();
                if (addr.length() > 0 && !validator.isValidInet4Address(addr)) {
                    LOG.log(Level.WARNING, "Invalid IPv4 address: " + addr);
                    new AlertBuilder(Alert.AlertType.WARNING)
                            .setTitle("Error")
                            .setMessage(addr + " is not a valid IPv4 address. Please" +
                                    " enter a valid address.")
                            .setPositive(r -> Platform.runLater(() -> recipientAddressText.requestFocus()))
                            .showAndWait();
                }
            }
        }));

        fileText.textProperty().bind(EasyBind.map(file, f -> f != null ? f.getAbsolutePath() : ""));
        fileText.setEditable(false);
        browseButton.textProperty().bind(EasyBind.map(file, f -> f == null ? "Browse..." : "  Clear  "));

        msgTextArea.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.length() > 0) file.setValue(null);
        }));

        codeText.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                codeText.setText(newValue.replaceAll("[^\\d]", ""));
            }
        }));
    }

    void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void onServerError(Throwable e) {
        LOG.log(Level.SEVERE, "Server error occurred. See stack trace");
        StringWriter stackTrace = new StringWriter();
        PrintWriter writer = new PrintWriter(stackTrace);
        e.printStackTrace(writer);
        LOG.log(Level.INFO, stackTrace.toString());

        if (e instanceof FileNotFoundException) {
            new AlertBuilder(Alert.AlertType.ERROR)
                    .setTitle("Send Error")
                    .setMessage("The file you selected cannot be found. Please select a new file.")
                    .setPositive(r -> Platform.runLater(() -> {
                        this.file.setValue(null);
                        this.browseButton.requestFocus();
                    })).showAndWait();
        }
    }

    private String getCurrentIp() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOG.log(Level.SEVERE, "Unable to acquire local IP address. Exiting.");
            new AlertBuilder(Alert.AlertType.ERROR).setTitle("Error")
                    .setMessage("Unable to acquire local IP address. Please check if you are connected to the internet.")
                    .setPositive(r -> System.exit(1))
                    .setNegative(r -> System.exit(1)).showAndWait();
        }
        return "";
    }

    @FXML
    public void onSendButtonPressed(Event event) {
        switch (validate()) {
            case INVALID:
                break;
            case TEXT:
                LOG.log(Level.INFO, "Sending text: " + msgTextArea.getText());
                sendText();
                break;
            case FILE:
                LOG.log(Level.INFO, "Sending file: " + file.get().getAbsolutePath());
                sendFile();
                break;
            default:
        }
    }

    private int validate() {
        AlertBuilder sendError = new AlertBuilder(Alert.AlertType.ERROR)
                .setOwner(primaryStage)
                .setTitle("Send Error");
        if (recipientAddressText.getText().length() == 0) {
            sendError.setMessage("You must specify the recipient's address.")
                    .setPositive(r -> Platform.runLater(() -> recipientAddressText.requestFocus()))
                    .showAndWait();
            return INVALID;
        }
        int codeLength = codeText.getText().length();
        if (codeLength == 0) {
            sendError.setMessage("You must supply the code displayed on the recipient's computer screen.")
                    .setPositive(r -> Platform.runLater(() -> codeText.requestFocus())).showAndWait();
            return INVALID;
        } else if (codeLength != 6) {
            sendError.setMessage("The verification code is 6 digits long.")
                    .setPositive(r -> Platform.runLater(() -> codeText.requestFocus())).showAndWait();
            return INVALID;
        }
        if (msgTextArea.getText().length() > 0) {
            return TEXT;
        } else if (file.get() != null) {
            return FILE;
        } else {
            sendError.setMessage("You must either select a file or write a message.").showAndWait();
            return INVALID;
        }
    }

    private void sendText() {
        String text = msgTextArea.getText();
        String host = recipientAddressText.getText();
        String code = codeText.getText();
        server.sendText(host, code, text);
    }

    private void sendFile() {
        File file = this.file.get();
        String host = recipientAddressText.getText();
        String code = codeText.getText();
        server.sendFile(host, code, file);
    }

    @FXML
    public void onExitButtonPressed(Event event) {
        new AlertBuilder(Alert.AlertType.CONFIRMATION)
                .setTitle("Exit")
                .setOwner(primaryStage)
                .setMessage("Are you sure you want to exit JDrop?")
                .setPositive(r -> {
                    server.interrupt();
                    Platform.exit();
                    System.exit(0);
                })
                .setNegative(r -> {}).showAndWait();
    }

    @FXML
    public void onBrowseButtonPressed(Event event) {
        if (file.get() == null) {
            file.setValue(new FileChooserBuilder()
                    .setTitle("Open")
                    .setPath(new File(System.getProperty("user.home")))
                    .showOpenDialog(primaryStage));
        } else {
            file.setValue(null);
        }
    }
}

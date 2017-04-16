import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class Main extends Application {

    public static final Logger LOG;

    static {
        LOG = Logger.getGlobal();
        SimpleFormatter fmt = new SimpleFormatter();
        StreamHandler sh = new StreamHandler(System.out, fmt);
        LOG.addHandler(sh);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        primaryStage.setTitle("JDrop");
        primaryStage.setScene(new Scene(root, 500, 500));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

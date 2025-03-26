package app_quan_ly_tai_chinh;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    private Controller controller;

    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app_quan_ly_tai_chinh/Main.fxml"));
            Parent root = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/app_quan_ly_tai_chinh/styles.css").toExternalForm());

            primaryStage.setMinWidth(1280);
            primaryStage.setMinHeight(720);
            primaryStage.setMaxWidth(1920);
            primaryStage.setMaxHeight(1080);

            scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustLayout(newVal.doubleValue(), scene.getHeight()));
            scene.heightProperty().addListener((obs, oldVal, newVal) -> adjustLayout(scene.getWidth(), newVal.doubleValue()));

            primaryStage.getIcons().add(new Image(getClass().getResource("/images.png").toString()));
            primaryStage.setScene(scene);
            primaryStage.setTitle("Fund Flex");
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adjustLayout(double width, double height) {
        if (controller != null) {
            controller.adjustLayout(width, height);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

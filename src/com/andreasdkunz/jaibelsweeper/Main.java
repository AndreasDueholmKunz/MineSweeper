package com.andreasdkunz.jaibelsweeper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Main extends Application {
    static Main instance;
    private static Stage primaryStage;

    static int width = 16;
    static int height = 11;
    static int bombs = 15;

    private Stage splashScreen;
    private void loadSplashScreen() {
        splashScreen = new Stage(StageStyle.TRANSPARENT);
        splashScreen.setScene(new Scene(new Label("Hej!"), 400, 200));
        splashScreen.getScene().setFill(Color.TRANSPARENT);
        System.out.println(splashScreen.getScene().getStylesheets());
        splashScreen.show();
    }

    private void closeSplashScreen() {
        splashScreen.hide();
        splashScreen = null;
    }

    private void loadMainScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("mainmenu.fxml"));
            Thread.sleep(3000);
            Platform.runLater(() -> {
                primaryStage.setScene(new Scene(root, 1280, 720));
                primaryStage.getScene().setCamera(new PerspectiveCamera());
                primaryStage.setOnCloseRequest(event -> System.exit(0));
                primaryStage.setTitle("JaibelSweeper");
                primaryStage.show();
                closeSplashScreen();
            });
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        instance = this;
        Main.primaryStage = primaryStage;

        loadSplashScreen();
        new Thread(this::loadMainScreen).start();

    }

    void switchToGame() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("game.fxml"));
            Scene scene = primaryStage.getScene();
            primaryStage.setScene(new Scene(root, scene.getWidth(), scene.getHeight()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void switchToMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("mainmenu.fxml"));
            Scene scene = primaryStage.getScene();
            primaryStage.setScene(new Scene(root, scene.getWidth(), scene.getHeight()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}

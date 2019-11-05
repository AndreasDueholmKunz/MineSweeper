package com.cybercat3.jaibelsweeper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    static Main instance;
    private static Stage primaryStage;

    static int width = 16;
    static int height = 11;
    static int bombs = 15;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        Main.primaryStage = primaryStage;
        primaryStage.setScene(new Scene(new Group(), 1280, 720));
        switchToMain();
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> System.exit(0));
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
            scene.setRoot(root);

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

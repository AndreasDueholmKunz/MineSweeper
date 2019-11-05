package com.andreasdkunz.jaibelsweeper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class gameController {
    @FXML private Pane gamePane;
    @FXML private Button backButton;
    @FXML private Button restart;
    @FXML private VBox topVBox;
    @FXML private VBox centerVBox;
    @FXML private VBox bottomVBox;
    @FXML private BorderPane superPane;
    private Label flagAmount;
    private Label timeLabel;
    private int time = 0;
    private int gameState = 1;
    private boolean paused = false;
    private Stage window = Main.getPrimaryStage();

    private Runnable flagLabelUpdater;

    private int cheatState = 0;

    private MineSweeperGame theGame = null;



    public void initialize() {
        System.out.println(Controller.instance.sliderHeight.getValue());
        System.out.println(Controller.instance.sliderWidth.getValue());
        System.out.println(Controller.instance.sliderBombs.getValue());

        // This is for activating cheats
        Platform.runLater(() -> superPane.setOnKeyTyped(event -> {
            String lower = event.getCharacter().toLowerCase();
            if (lower.equals("j") && cheatState == 0) cheatState++;
            else if (lower.equals("a") && cheatState == 1) cheatState++;
            else if (lower.equals("i") && cheatState == 2) cheatState++;
            else if (lower.equals("b") && cheatState == 3) cheatState++;
            else if (lower.equals("e") && cheatState == 4) cheatState++;
            else if (lower.equals("l") && cheatState ==5 ) {
                cheatState = 0;
                System.out.println("Changed cheat state");
                if (theGame.isCheating()) theGame.stopCheat();
                else                      theGame.startCheat();
                System.out.println("cheating? " + theGame.isCheating());
            } else cheatState = 0;
            System.out.println("cheatState = " + cheatState);
        }));

        newGame();

        ImageView flagIV = new ImageView(new Image(getClass().getResource("flagGrey.png").toExternalForm()));
        flagAmount = new Label("20 / 21");
        flagAmount.setTextFill(Color.GRAY);
        flagAmount.setFont(new Font("Verdana Bold",35));

        topVBox.getChildren().add(flagIV);
        topVBox.getChildren().add(flagAmount);
        topVBox.setPadding(new Insets(0,25,0,0));

        ImageView clockIV = new ImageView(new Image(getClass().getResource("clock.png").toExternalForm()));
        timeLabel = new Label("00:00");
        timeLabel.setTextFill(Color.GRAY);
        timeLabel.setFont(new Font("Verdana Bold", 35));

        centerVBox.getChildren().addAll(clockIV, timeLabel);
        centerVBox.setPadding(new Insets(0,25,0,0));


        bottomVBox.setPadding(new Insets(0,25,25,0));
//        bottomVBox.setStyle("-fx-border-color: red; -fx-border-width: 3; -fx-border-style: dashed");

        flagIV.setPreserveRatio(true);
        flagIV.fitWidthProperty().bind(superPane.widthProperty().multiply(0.1));
        clockIV.setPreserveRatio(true);
        clockIV.fitWidthProperty().bind(superPane.widthProperty().multiply(0.1));


        restart.setOnAction(event -> newGame());
        backButton.setOnAction(event -> {
            Main.instance.switchToMain();
            theGame.stopCheat();
            gameState++;
        });
        {
            Button[] buttons = {restart, backButton};
            for (Button b : buttons) {
                b.prefWidthProperty().bind(superPane.widthProperty().multiply(0.12).add(25));
                b.prefHeightProperty().bind(superPane.heightProperty().multiply(0.08).add(25));

            }
        }

        flagLabelUpdater = () -> flagAmount.setText(theGame.getCellsFlagged()+ " / " + theGame.getBombAmount());
    }

    private void newGame() {
        Platform.runLater(() -> {
            gameState++;
            time = 0;
            if (theGame != null) {
                theGame.stopCheat();
                theGame.stopExplosion();
            }

            Thread timerThread = new Thread(() -> {
                int myGameState = gameState;
                while (myGameState == gameState) {
                    int minutes = time / 60;
                    int seconds = time % 60;
                    String s = (seconds <= 9 ? "0" : "") + seconds;
                    String m = (minutes <= 9 ? "0" : "") + minutes;
                    Platform.runLater(() -> timeLabel.setText(m + ":" + s));

                    if (!window.isIconified()) {
                        ++time;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            timerThread.setName("TimeCounterThread");
            timerThread.setPriority(6);
            timerThread.setDaemon(true);
            timerThread.start();

            theGame = new MineSweeperGame(gamePane,

                    (int) Controller.instance.sliderWidth.getValue(),
                    (int) Controller.instance.sliderHeight.getValue(),
                    (int) Controller.instance.sliderBombs.getValue()/100d
            );
            theGame.setOnFlagChanged(flagLabelUpdater);
            theGame.setOnWon(() -> {
                gameState++;
                System.out.println("The Score: " + Math.round((double)Main.width*Main.height/(Main.bombs*2)/time*2000));
                System.out.println("The score2: " + Math.round((theGame.getCellsRevealed()*Main.bombs) / (double) time));
            });
            theGame.setOnLost(() -> gameState++);
            Platform.runLater(() -> flagLabelUpdater.run());
        });
    }

}

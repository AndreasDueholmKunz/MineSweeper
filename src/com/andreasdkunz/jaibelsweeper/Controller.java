package com.andreasdkunz.jaibelsweeper;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;


public class Controller {

    @FXML private StackPane superPane;
    @FXML private Label title;
    @FXML public Slider sliderWidth;
    @FXML public Slider sliderHeight;
    @FXML public Slider sliderBombs;
    @FXML private Label widthLabel;
    @FXML private Label heightLabel;
    @FXML private Label bombsLabel;
    @FXML private Button startButton;
    @FXML private Label maxBombs;
    @FXML private Label minWidth;
    @FXML private Label maxWidth;
    @FXML private Label minHeight;
    @FXML private Label maxHeight;
    @FXML private Label minBombs;
    @FXML private Label widthLabelValue;
    @FXML private Label heightLabelValue;
    @FXML private Label bombsLabelValue;
    @FXML private Button negativeValueWidth;
    @FXML private Button positiveValueWidth;
    @FXML private Button negativeValueHeight;
    @FXML private Button positiveValueHeight;
    @FXML private Button negativeValueBombs;
    @FXML private Button positiveValueBombs;

    public static Controller instance;

    private boolean onMenu = true;

    public void initialize() {
        instance = this;

        // Styles and sets up the buttons that change the sliders
        {
            EventHandler<ActionEvent> buttonHandler = event -> {
                String id = event.toString().split("[\\[, =]")[4];
                switch (id) {
                    case "positiveValueWidth":
                        sliderWidth.setValue(sliderWidth.getValue()+1);
                        break;
                    case "negativeValueWidth":
                        sliderWidth.setValue(sliderWidth.getValue()-1);
                        break;
                    case "positiveValueHeight":
                        sliderHeight.setValue(sliderHeight.getValue()+1);
                        break;
                    case "negativeValueHeight":
                        sliderHeight.setValue(sliderHeight.getValue()-1);
                        break;
                    case "positiveValueBombs":
                        sliderBombs.setValue(sliderBombs.getValue()+1);
                        break;
                    case "negativeValueBombs":
                        sliderBombs.setValue(sliderBombs.getValue()-1);
                        break;
                }
            };


            Button[] buttons = {negativeValueWidth, positiveValueWidth, negativeValueHeight, positiveValueHeight, negativeValueBombs, positiveValueBombs};
            for (Button button : buttons) {
                button.setStyle("-fx-background-color: rgb(0,0,0,0.2); -fx-font: 15 Verdana");
                button.setOnAction(buttonHandler);
            }

        }



        // Scales the sliders
        Slider[] sliders = {sliderWidth,sliderHeight,sliderBombs};
        for (Slider s : sliders) {
            s.prefWidthProperty().bind(superPane.widthProperty().multiply(0.3));
            s.setSnapToTicks(true);
            s.setMajorTickUnit(1);
//            s.setStyle("-fx-border-width: 3; -fx-border-color: red; -fx-border-style: dashed");
        }

        // Initialises the slider values
            sliderWidth.setValue(Main.width);
            sliderHeight.setValue(Main.height);
            sliderBombs.setValue(Main.bombs);




        // The sliderValueListener
        ChangeListener<Number> sliderValueListener = (observable, oldValue, newValue) -> {

            Main.width = (int) Math.round(sliderWidth.getValue());
            Main.height = (int) Math.round(sliderHeight.getValue());
            Main.bombs = (int) Math.round(sliderBombs.getValue());
            System.out.println("Main.width: " + Main.width);
            System.out.println("Main.height: " + Main.height);
            System.out.println("Main.bombs: " + Main.bombs);


            widthLabelValue.setText(Math.round(sliderWidth.getValue())+"");
            heightLabelValue.setText(Math.round(sliderHeight.getValue())+"");
            bombsLabelValue.setText(Math.round(sliderBombs.getValue())+"%");
        };

        // Applies the listener to the sliders
        sliderWidth.valueProperty().addListener(sliderValueListener);
        sliderHeight.valueProperty().addListener(sliderValueListener);
        sliderBombs.valueProperty().addListener(sliderValueListener);

        // Runs the listener
        Platform.runLater(() -> sliderValueListener.changed(null,null,null));


        // Sets the bomb min and max labels up.
        {
            Label[] labels = {minWidth, maxWidth, minHeight, maxHeight, minBombs, maxBombs};
            for (Label l : labels) {
                l.setFont(new Font("Verdana", 35));
            }
        }

        // Sets the currentValueLabels up
        {
            Label[] labels = {widthLabelValue, heightLabelValue, bombsLabelValue};
            for (Label l : labels) {
                l.setFont(new Font("Verdana",20));
            }
        }


        // Sets the labels up
        Label[] labels = {widthLabel, heightLabel, bombsLabel};
        for (Label l : labels) {
            l.setTextFill(new Color(0,0.57,0.57,1));
            l.setPadding(new Insets(0,0,0,25));
            l.setFont(new Font("Verdana", 30));
//            l.setStyle("-fx-border-width: 3; -fx-border-color: red; -fx-border-style: dashed");
        }


        // Sets up the button
        startButton.setOnAction(event -> Main.instance.switchToGame());
        startButton.setPrefHeight(134);

//        startButton.setStyle("-fx-border-width: 3; -fx-border-color: red; -fx-border-style: dashed");

//        labelVBox.setStyle("-fx-border-width: 3; -fx-border-color: red; -fx-border-style: dashed");


        // Getting background image
        Image backgroundImage = new Image(getClass().getResource("background.png").toExternalForm());
        ImageView[] backgrounds = {new ImageView(backgroundImage), new ImageView(backgroundImage)};

        // Blurs the backgrounds
        for (ImageView i : backgrounds) i.setEffect(new GaussianBlur(20));

        // Scales the menu
        ChangeListener<Number> windowSizeListener = (observable, oldValue, newValue) -> {

            title.setFont(new Font(title.getFont().getName(),
                    superPane.getWidth() / 8
                    ));


            // This if for scaling the moving background
            double biggestSide = (superPane.getWidth() > superPane.getHeight() ? superPane.getWidth() : superPane.getHeight());

            double ratio = backgroundImage.getWidth() / backgroundImage.getHeight();

            for (ImageView i : backgrounds) {
                i.setFitHeight(biggestSide*1);
                i.setFitWidth(i.getFitHeight()*ratio);
            }
        };

        // Adds the scaler to dimension properties
        superPane.widthProperty().addListener(windowSizeListener);
        superPane.heightProperty().addListener(windowSizeListener);

        // Animates the background
        new Thread(() -> {
            while (onMenu) {
                Platform.runLater(() -> {
                    backgrounds[0].setTranslateX(backgrounds[0].getTranslateX()+1);
                    backgrounds[1].setTranslateX(backgrounds[0].getTranslateX()-backgrounds[1].getFitWidth());

                    if (backgrounds[0].getTranslateX() > superPane.getWidth()*1.5) {
                        ImageView temp = backgrounds[0];
                        backgrounds[0] = backgrounds[1];
                        backgrounds[1] = temp;
                    }
                });
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Adds the background to the stackPane
        superPane.getChildren().addAll(backgrounds);

        // Puts the background to the back, so the menu can be seen
        for (ImageView i : backgrounds) i.toBack();



    }
}




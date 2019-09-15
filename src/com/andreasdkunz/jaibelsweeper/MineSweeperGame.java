package com.andreasdkunz.jaibelsweeper;


import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.*;

class MineSweeperGame {

    private Pane gamePane;
    private int cellsHorizontal;
    private int cellsVertical;
    private int bombAmount;
    private int cellsRevealed;
    private int cellsFlagged;
    private boolean hasLost = false;
    private boolean hasWon = false;
    private boolean currentlyMassRevealing = false;
    private boolean isCheating = false;

    // If you're reading this, I just want to apoligize.

    private int lowestProbabilityX;
    private int lowestProbabilityY;

    private int highestProbabilityX;
    private int highestProbabilityY;

    private int cheatState = 0;

    private Cell markedJaibelCell = null;

    private double size;
    private double chanceOfUnknownBomb = 0;

    private Runnable onWon = null;
    private Runnable onLost = null;
    private Runnable onFlagChanged = null;

    private Thread jaibelCheater = null;

    private Cell[][] gameGrid;

    private Color[] colorMap = {
            Color.BLUE,
            Color.GREEN,
            Color.RED,
            Color.PURPLE,
            Color.MAROON,
            Color.TURQUOISE,
            Color.BLACK,
            Color.GRAY
    };

    private Image flagImage = new Image(getClass().getResource("flag.png").toExternalForm());
    private Image jaibelImage = new Image(getClass().getResource("Jaibel.png").toExternalForm());

    public MineSweeperGame(Pane gamePane, int cellsHorizontal, int cellsVertical, int bombAmount) {
        this.gamePane = gamePane;
        this.cellsHorizontal = cellsHorizontal;
        this.cellsVertical = cellsVertical;
        this.bombAmount = bombAmount;

        if (bombAmount > cellsHorizontal * cellsVertical) {
            System.out.println("(MineSweeperGame -> constructor) More bombs than cells.");
            return;
        }

        gamePane.getChildren().clear();

        this.cellsFlagged = 0;
        this.cellsRevealed = 0;
        this.size = 0;

        gameGrid = new Cell[cellsHorizontal][cellsVertical];

        // This creates new cells for every spot in the gameGrid
        Platform.runLater(() -> {
            for (int i = 0; i < cellsHorizontal; i++) {
                for (int j = 0; j < cellsVertical; j++) {
                    gameGrid[i][j] = new Cell(i, j);
                }
            }
        });

        // This creates a windowSizeListener and assigns it to the gamePane properties.
        Platform.runLater(() -> {
            ChangeListener<Number> windowSizeListener = (observableValue, oldValue, newValue) -> {
                this.scale();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            gamePane.widthProperty().addListener(windowSizeListener);
            gamePane.heightProperty().addListener(windowSizeListener);
        });

        Platform.runLater(this::setBombs);
        Platform.runLater(this::countBombs);
        Platform.runLater(this::scale);


//        Platform.runLater(() -> {
//            for (int i = 0; i < cellsHorizontal; i++) {
//                for (int j = 0; j < cellsVertical; j++) {
//                    gameGrid[i][j].reveal();
//                }
//            }
//        });


    }

    public MineSweeperGame(Pane gamePane, int cellsHorizontal, int cellsVertical, double bombRatio) {
        this(gamePane, cellsHorizontal, cellsVertical, (int) Math.round(cellsHorizontal * cellsVertical * bombRatio));
    }

    private void lost(Cell explodedBomb) {
        PerspectiveCamera camera = new PerspectiveCamera();
        Main.getPrimaryStage().getScene().setCamera(camera);

        StackPane explodedBombGraphic = explodedBomb.graphic;


        Ellipse explosion = new Ellipse();
        explosion.centerXProperty().bind(explodedBombGraphic.layoutXProperty().add(explodedBombGraphic.widthProperty().multiply(0.5)));
        explosion.centerYProperty().bind(explodedBombGraphic.layoutYProperty().add(explodedBombGraphic.heightProperty().multiply(0.5)));

        explosion.setRadiusX(0);
        explosion.setRadiusY(0);

        explosion.setFill(new RadialGradient(
                0,
                .1,
                0.5,
                0.5,
                0.7,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0)),
                new Stop(1, Color.rgb(255, 128, 0))
        ));

        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                if (Math.pow(i,1.3) < 1000) {
                    camera.setLayoutX(Math.random() * (1000-Math.pow(i,1.3))/ size * 7);
                    camera.setLayoutY(Math.random() * (1000-Math.pow(i,1.3)) / size * 7);
                }
//                if (i == 120) {
//                    camera.setLayoutX(0);
//                    camera.setLayoutY(0);
//                }
                int finalI = i;
                Platform.runLater(() -> {
                    explosion.setRadiusX(finalI * size / 10);
                    explosion.setRadiusY(finalI * size / 10);
                    explosion.setOpacity(explosion.getOpacity() - 0.005);

                    if (explosion.getOpacity() <= 0) gamePane.getChildren().remove(explosion);
                });
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();

        gamePane.getChildren().add(explosion);

        hasLost = true;
        for (int i = 0; i < cellsHorizontal; i++) {
            for (int j = 0; j < cellsVertical; j++) {
                if (!gameGrid[i][j].isRevealed) {
                    gameGrid[i][j].reveal();
                }
            }
        }

        if (isCheating) calcCheatPercentages();

        if (onLost != null) onLost.run();
    }

    private void won() {
        System.out.println("wonCalled");
        hasWon = true;

        for (Cell[] i : gameGrid) {
            for (Cell j : i) {
                if (j.isBomb && !j.isFlagged) {
                    j.flag();
                }
            }
        }

        Platform.runLater(() -> {
            Label winLabel = new Label("Du Vandt!");
            winLabel.setFont(new Font("Trebuchet MS", 150));
            winLabel.setTextFill(Color.GOLDENROD);
            winLabel.layoutXProperty().bind(gamePane.widthProperty().subtract(winLabel.widthProperty()).multiply(0.5));
            winLabel.layoutYProperty().bind(gamePane.heightProperty().subtract(winLabel.heightProperty()).multiply(0.4));
            winLabel.setEffect(new Bloom(0.1));


            gamePane.getChildren().add(winLabel);
        });

        if (onWon != null) onWon.run();
    }

    private void setBombs() {
        int bombsSet = 0;
        while (bombsSet < bombAmount) {
            int randomX = (int) Math.floor(Math.random() * cellsHorizontal);
            int randomY = (int) Math.floor(Math.random() * cellsVertical);
            if (!gameGrid[randomX][randomY].isBomb) {
                gameGrid[randomX][randomY].isBomb = true;
                bombsSet++;
            }
        }
    }

    private void countBombs() {
        for (int i = 0; i < cellsHorizontal; i++) {
            for (int j = 0; j < cellsVertical; j++) {
                gameGrid[i][j].countBombs();
            }
        }
    }

    private void scale() {
        double widthProportional = gamePane.getWidth() * cellsVertical;
        double heightProportional = gamePane.getHeight() * cellsHorizontal;

        double smallestSide = (widthProportional < heightProportional) ? widthProportional : heightProportional;
        int largestAmount;
        int smallestAmount;

        if (cellsHorizontal < cellsVertical) {
            largestAmount = cellsVertical;
            smallestAmount = cellsHorizontal;
        } else {
            largestAmount = cellsHorizontal;
            smallestAmount = cellsVertical;
        }


        size = smallestSide / largestAmount;
        size /= largestAmount / (largestAmount / (double) smallestAmount);
        System.out.println("size: " + size);

        for (Cell[] i : gameGrid) {
            for (Cell j : i) {
                j.scale();
            }
        }

    }

    private void massReveal(Cell revealOrigin) {
        System.out.println("MassRevealCalled");
//        currentlyMassRevealing = true;
        System.out.println(revealOrigin);


        new Thread(() -> {
            class QueueHolder {
                private List<Cell> currentQueue = new LinkedList<>();
                private List<Cell> nextQueue = new LinkedList<>();
            }

            QueueHolder q = new QueueHolder();




            q.currentQueue.add(revealOrigin);

            while (q.currentQueue.size() != 0) {
                for (Cell c : q.currentQueue) {
                    Platform.runLater(() -> {
                        if (c.bombCount == 0) {
                            int neighborsRevealed = 0;
                            for (int i = c.x - 1; i <= c.x + 1; i++) {
                                for (int j = c.y - 1; j <= c.y + 1; j++) {
                                    if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                                        if (!(i == c.x && j == c.y)) {
                                            Cell curr = gameGrid[i][j];
                                            if (!curr.isRevealed && !curr.isFlagged) {
//                                                Platform.runLater(curr::reveal);
                                                curr.reveal();
                                                curr.isRevealed = true;
                                                q.nextQueue.add(curr);
                                                neighborsRevealed++;
                                                c.logs.put("neighborsRevealed", neighborsRevealed + "");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });


                    try {
                        Thread.sleep(0, 5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                q.currentQueue.clear();
                List<Cell> temp = q.currentQueue;
                q.currentQueue = q.nextQueue;
                q.nextQueue = temp;
            }
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                currentlyMassRevealing = false;
                calcCheatPercentages();
            });
        }).start();


    }

    public void setOnWon(Runnable onWon) {
        this.onWon = onWon;
    }

    public void setOnLost(Runnable onLost) {
        this.onLost = onLost;
    }

    public void startCheat() {
        calcChanceOfUnknownBomb();
        Platform.runLater(this::calcCheatPercentages);
//        calcCheatPercentages();
        isCheating = true;

        jaibelCheater = new Thread(() -> {
            ImageView Jaibel = new ImageView(new Image(getClass().getResource("Jaibel.png").toExternalForm()));
            Platform.runLater(() -> {
                Jaibel.setPreserveRatio(true);
                Jaibel.fitWidthProperty().bind(gameGrid[0][0].graphic.widthProperty());
                gamePane.getChildren().add(Jaibel);
            });
            while (isCheating) {
                if (!currentlyMassRevealing) {
                    int targetX = highestProbabilityX;
                    int targetY = highestProbabilityY;
                    boolean shouldFlag = true;
                    if (lowestProbabilityX == -5) {
                        for (int x = 0; x < cellsHorizontal; x++) {
                            for (int y = 0; y < cellsVertical; y++) {
                                Cell curr = gameGrid[x][y];
                                if (!curr.isRevealed && !curr.isFlagged) {
                                    if (lowestProbabilityX == -5) {
                                        lowestProbabilityX = x;
                                        lowestProbabilityY = y;
                                    } else if (gameGrid[lowestProbabilityX][lowestProbabilityY].myPercentageChance >
                                            gameGrid[x][y].myPercentageChance) {
                                        lowestProbabilityX = x;
                                        lowestProbabilityY = y;
                                    }
                                }
                            }
                        }
                    }
                    if (highestProbabilityX == -5) {
                        targetX = lowestProbabilityX;
                        targetY = lowestProbabilityY;
                        shouldFlag = false;
                    }

                    if (targetX == -5) {
                        targetX = 0;
                        targetY = 0;
                    }

                    Cell highestProbabilityCell = gameGrid[targetX][targetY];
                    Jaibel.setRotate(180 + Math.toDegrees(Math.atan2(Jaibel.getLayoutY()-highestProbabilityCell.graphic.getLayoutY(), Jaibel.getLayoutX() - highestProbabilityCell.graphic.getLayoutX())));
                    Jaibel.relocate(
                            Jaibel.getLayoutX() + Math.cos(Math.toRadians(Jaibel.getRotate())) * size / 2,
                            Jaibel.getLayoutY() + Math.sin(Math.toRadians(Jaibel.getRotate())) * size / 2
                    );

                    if (Math.sqrt(
                            Math.pow(Jaibel.getLayoutX() - targetX*size,2)+
                                    Math.pow(Jaibel.getLayoutY() - targetY*size,2))
                            < size/4) {
                        if (!highestProbabilityCell.isFlagged && !highestProbabilityCell.isRevealed) {
                            if (shouldFlag)
                                Platform.runLater(highestProbabilityCell::flag);
                            else {
                                highestProbabilityCell.isRevealed = true;
                                Platform.runLater(highestProbabilityCell::reveal);
                            }
                        }
                        Platform.runLater(this::calcCheatPercentages);
                    }
                } else {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(32);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Platform.runLater(() -> {
                Platform.runLater(() -> gamePane.getChildren().remove(Jaibel));
            });
        });
        jaibelCheater.start();
    }

    public void stopCheat() {
        isCheating = false;
        Platform.runLater(this::calcCheatPercentages);
        jaibelCheater = null;
    }

    private void calcChanceOfUnknownBomb() {
        chanceOfUnknownBomb = (bombAmount - cellsFlagged) * (100d / (cellsHorizontal * cellsVertical - cellsRevealed - cellsFlagged));
    }

    private void calcCheatPercentages() {
        if (isCheating) {
            for (Cell[] i : gameGrid) {
                for (Cell j : i) {
                    j.calcCheatPercentage();
                }
            }

            highestProbabilityX = -5;
            highestProbabilityY = -5;

            lowestProbabilityX = -5;
            lowestProbabilityY = -5;
            int lowestPercentage = 101;

            for (int x = 0; x < cellsHorizontal; x++) {
                for (int y = 0; y < cellsVertical; y++) {
//                    System.out.println("X="+x+" Y="+y);
                    int currPercentage = (int) Math.round(gameGrid[x][y].myPercentageChance);

                    if (currPercentage >= 100 && !gameGrid[x][y].isFlagged) {
                        highestProbabilityX = x;
                        highestProbabilityY = y;
//                        System.out.println("highestProbabilityX = " + highestProbabilityX);
//                        System.out.println("highestProbabilityY = " + highestProbabilityY);
                    }

//                    System.out.println("currPercentage = " + currPercentage);

                    if (currPercentage == 0 && !gameGrid[x][y].isRevealed) {
                        lowestPercentage = currPercentage;
                        lowestProbabilityX = x;
                        lowestProbabilityY = y;
                    }


                }
            }

//            System.out.println("recordX = " + recordX);
//            System.out.println("recordY = " + recordY);


        }
    }

    public void setOnFlagChanged(Runnable onFlagChanged) {
        this.onFlagChanged = onFlagChanged;
    }

    public int getBombAmount() {
        return bombAmount;
    }

    public int getCellsFlagged() {
        return cellsFlagged;
    }

    public boolean isCheating() {
        return isCheating;
    }

    public int getCellsRevealed() {
        return cellsRevealed;
    }

    private class Cell {
        private int x;
        private int y;
        private int bombCount;

        private StackPane graphic;
        private Rectangle background;

        private boolean isBomb;
        private boolean isRevealed;
        private boolean isFlagged;

        private double myPercentageChance = -1;

        private Label bombCountLabel;
//        private Label cheatPercentageLabel = null;

        private Map<String, String> logs = new HashMap<>();

        private Cell selected = null;

        private ImageView myFlag = null;
        private Rectangle whiteEffect = null;


        private Cell(int x, int y) {
            this.x = x;
            this.y = y;
            this.bombCount = -1;
            this.isBomb = false;
            this.isRevealed = false;
            this.isFlagged = false;

            graphic = new StackPane();
            background = new Rectangle();
            graphic.getChildren().add(background);
            gamePane.getChildren().add(graphic);

            background.setFill(new RadialGradient(
                    0,
                    .1,
                    0.5,
                    0.5,
                    0.7,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(130, 130, 130)),
                    new Stop(1, Color.rgb(70, 70, 70))
            ));
            background.setStroke(Color.WHITE);

            graphic.setOnMousePressed(event -> {
                if (!hasWon && !hasLost && !this.isFlagged)
                    graphic.setEffect(new Bloom(0.5));
            });

            graphic.setOnMouseReleased(event -> {
                graphic.setEffect(null);
            });

            graphic.setOnMouseClicked(event -> {
                if (isCheating) {
                    Platform.runLater(this::calcCheatPercentage);
                }
                System.out.println("chanceOfUnknownBomb = " + chanceOfUnknownBomb);
                if (!hasWon && !hasLost) {
                    if (event.getButton().equals(MouseButton.PRIMARY)) {
                        if (!isFlagged) {
                            if (!isRevealed) {
                                reveal();
                            } else {
                                revealNeighbors();
                            }
                        }
                    } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                        if (!isRevealed) {
                            if (!isFlagged) {
                                flag();
                            } else {
                                unflag();
                            }
                        }
                    } else if (event.getButton().equals(MouseButton.MIDDLE)) {
//                        gamePane.getChildren().add(new Line(x*size+size/2,y*size+size/2,selected.x*size+size/2,selected.y*size+size/2));
                        if (selected != null) {
                            new Thread(() -> {
                                Line connected = new Line(x*size+size/2,y*size+size/2,selected.x*size+size/2,selected.y*size+size/2);
                                Platform.runLater(() -> gamePane.getChildren().add(connected));
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Platform.runLater(() -> gamePane.getChildren().remove(connected));
                            }).start();
                        }
                        System.out.println("x = " + x);
                        System.out.println("y = " + y);
                        System.out.println("bombCount = " + bombCount);
                        System.out.println("getFlaggedNeighbors(): " + getFlaggedNeighbors());
                        System.out.println("getRevealedNeighbors(): " + getRevealedNeighbors());
                        System.out.println("neighborCount(): " + neighborCount());
                        System.out.println("logs.get(\"neighbors Revealed\"): " + logs.get("neighborsRevealed"));
                    }
                }
            });
        }

        private void revealNeighbors() {
            int flaggedNeighbors = 0;

            for (int i = x - 1; i <= x + 1; i++)
                for (int j = y - 1; j <= y + 1; j++)
                    if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical)
                        if (gameGrid[i][j].isFlagged)
                            flaggedNeighbors++;

            System.out.println("flaggedNeighbors: " + flaggedNeighbors);
            System.out.println("bombAmount: " + bombCount);


            if (flaggedNeighbors == bombCount)
                for (int i = x - 1; i <= x + 1; i++)
                    for (int j = y - 1; j <= y + 1; j++)
                        if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical)
                            if (!gameGrid[i][j].isRevealed)
                                gameGrid[i][j].reveal();
        }

        private void countBombs() {
            if (!isBomb) {
                bombCount = 0;

                for (int i = x - 1; i <= x + 1; i++) {
//                    System.out.println("This is a j loop");
                    for (int j = y - 1; j <= y + 1; j++) {
//                        System.out.println("j: " + j);
                        if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                            if (gameGrid[i][j].isBomb) {
                                bombCount++;
                            }
                        }
                    }
                }
            }
        }

        private void reveal() {
            this.countBombs();

            if (isCheating && !currentlyMassRevealing && !hasLost) {
                Platform.runLater(MineSweeperGame.this::calcCheatPercentages);
            }

            if (!isFlagged) {
                isRevealed = true;
                cellsRevealed++;

                if (isBomb) {
                    if (!hasLost) {
                        background.setFill(new RadialGradient(
                                0,
                                .1,
                                0.5,
                                0.5,
                                0.5,
                                true,
                                CycleMethod.NO_CYCLE,
                                new Stop(0, Color.rgb(100, 0, 0)),
                                new Stop(1, Color.rgb(255, 0, 0))
                        ));

                        lost(this);
                    }
                    ImageView jaibel = new ImageView(jaibelImage);
                    jaibel.fitWidthProperty().bind(background.widthProperty());
                    jaibel.fitHeightProperty().bind(background.heightProperty());

                    graphic.getChildren().add(jaibel);

                } else {
                    background.setFill(new RadialGradient(
                            0,
                            .1,
                            0.5,
                            0.5,
                            0.7,
                            true,
                            CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(180, 180, 180)),
                            new Stop(1, Color.rgb(120, 120, 120))
                    ));
                }

                if (cellsHorizontal * cellsVertical - bombAmount <= cellsRevealed) {
                    if (!hasLost && !hasWon) won();
                }


                if (bombCount > 0) {
                    bombCountLabel = new Label("" + bombCount);
                    bombCountLabel.setTextFill(colorMap[bombCount - 1]);
                    bombCountLabel.setFont(new Font(size / 1.5));
                    graphic.getChildren().add(bombCountLabel);
                }

                if (bombCount == 0) {
                    if (!currentlyMassRevealing) {
                        System.out.println("Should Mass reveal");
                        massReveal(this);
                    }

                }
                // Rewrite to non recursive function
//                if (bombCount == 0) {
//                    for (int i = x - 1; i <= x + 1; i++) {
//                        for (int j = y - 1; j <= y + 1; j++) {
//                            if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
//                                if (!(i == x && j == y)) {
//                                    Cell curr = gameGrid[i][j];
//                                    if (!curr.isRevealed && !curr.isFlagged) {
//                                        curr.reveal();
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }

            } else if (!isBomb) {
                Line line1 = new Line(0, 0, 0, 0);
                line1.endXProperty().bind(background.widthProperty().multiply(0.7));
                line1.endYProperty().bind(background.heightProperty().multiply(0.7));
                Line line2 = new Line(0, 0, 0, 0);
                line2.startXProperty().bind(background.widthProperty().multiply(0.7));
                line2.endYProperty().bind(background.heightProperty().multiply(0.7));
                Line[] lines = {line1, line2};
                for (Line i : lines) {
                    i.setStrokeWidth(5);
                    i.setStroke(Color.DARKRED);
                }
                graphic.getChildren().add(line1);
                graphic.getChildren().add(line2);
                System.out.println("IsflaggedCorrectly");
            }

            if (!hasLost && !isFlagged) {
                whiteEffect = new Rectangle(0, 0, Color.WHITE);
                graphic.getChildren().add(whiteEffect);
                whiteEffect.widthProperty().bind(graphic.widthProperty());
                whiteEffect.heightProperty().bind(graphic.heightProperty());

                new Thread(() -> {
                    int frames = 10;
                    double bit = 1d / frames;

                    for (int i = 0; i < frames; i++) {
                        Platform.runLater(() -> {
//                            System.out.println(whiteEffect.getOpacity());
                            whiteEffect.setOpacity(whiteEffect.getOpacity() - bit);
                            whiteEffect.arcWidthProperty().bind(graphic.widthProperty().divide(3));
                            whiteEffect.arcHeightProperty().bind(graphic.heightProperty().divide(3));

                        });

                        try {
                            Thread.sleep(32);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Platform.runLater(() -> {
                        graphic.getChildren().remove(whiteEffect);
                        whiteEffect = null;
                    });
                }).start();
            }

        }

        private void scale() {
            background.setWidth(size);
            background.setHeight(size);
            graphic.relocate(x * size, y * size);
            background.setStrokeWidth(10);
            background.setStrokeWidth(map(cellsHorizontal * cellsVertical, 64, 6400, 10, 1) / 130 * size);
            background.setArcHeight(size / 3);
            background.setArcWidth(size / 3);
            if (bombCountLabel != null)
                bombCountLabel.setFont(new Font(size / 1.5));

//            if (cheatPercentageLabel != null)
//                cheatPercentageLabel.setFont(new Font(size / 3));
        }

        private void flag() {
            isFlagged = true;
            if (myFlag == null) {
                myFlag = new ImageView(flagImage);
            }
            cellsFlagged++;
            graphic.getChildren().add(myFlag);
            myFlag.fitWidthProperty().bind(background.widthProperty());
            myFlag.fitHeightProperty().bind(background.heightProperty());

            if (onFlagChanged != null) onFlagChanged.run();
            Platform.runLater(MineSweeperGame.this::calcCheatPercentages);
        }

        private void unflag() {
            isFlagged = false;
            cellsFlagged--;
            graphic.getChildren().remove(myFlag);

            if (onFlagChanged != null) onFlagChanged.run();
            Platform.runLater(MineSweeperGame.this::calcCheatPercentages);
        }

        private int getFlaggedNeighbors() {
            int flaggedNeighbors = 0;
            for (int i = x - 1; i <= x + 1; i++) {
                for (int j = y - 1; j <= y + 1; j++) {
                    if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                        if (!(i == x && j == y)) {
                            Cell curr = gameGrid[i][j];

                            if (curr.isFlagged) flaggedNeighbors++;
                        }
                    }
                }
            }
            return flaggedNeighbors;
        }

        private int getRevealedNeighbors() {
            int revealedNeighbors = 0;
            for (int i = x - 1; i <= x + 1; i++) {
                for (int j = y - 1; j <= y + 1; j++) {
                    if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                        if (!(i == x && j == y)) {
                            Cell curr = gameGrid[i][j];

                            if (curr.isRevealed) revealedNeighbors++;
                        }
                    }
                }
            }
            return revealedNeighbors;
        }

        private int localCheatScore() {
            if (bombCount == getFlaggedNeighbors()) return 0;
            if (bombCount - getFlaggedNeighbors() == neighborCount() - getRevealedNeighbors() - getFlaggedNeighbors()) return 0;
//            return (neighborCount() - getRevealedNeighbors() - getFlaggedNeighbors()) - (bombCount - (bombCount-getFlaggedNeighbors()));
            return neighborCount() - getRevealedNeighbors() - (bombCount);
        }

        private int neighborCount() {
            int neighborCount = 0;
            for (int i = x - 1; i <= x + 1; i++) {
                for (int j = y - 1; j <= y + 1; j++) {
                    if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                        if (!(i == x && j == y)) {
                            neighborCount++;
                        }
                    }
                }
            }
            return neighborCount;
        }

        private void calcCheatPercentage() {
            calcChanceOfUnknownBomb();
//            if (isCheating && cheatPercentageLabel == null) {
//                cheatPercentageLabel = new Label();
//                graphic.getChildren().add(cheatPercentageLabel);
//            }

//            if (cheatPercentageLabel != null) {
                if (!isCheating || isFlagged || isRevealed) {
//                    graphic.getChildren().remove(cheatPercentageLabel);
//                    cheatPercentageLabel = null;
                } else {
                    myPercentageChance = chanceOfUnknownBomb;

                    boolean hasValidNeighbor = false;

                    theLoop:
                    for (int i = x - 1; i <= x + 1; i++) {
                        for (int j = y - 1; j <= y + 1; j++) {
                            if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                                if (!(i == x && j == y)) {
                                    Cell curr = gameGrid[i][j];
                                    if (curr.isRevealed) {
                                        hasValidNeighbor = true;
                                        break theLoop;
                                    }
                                }
                            }
                        }
                    }



                    if (hasValidNeighbor) {
                        ArrayList<Cell> candidates = new ArrayList<>();
                        for (int i = x - 1; i <= x + 1; i++) {
                            for (int j = y - 1; j <= y + 1; j++) {
                                if (i >= 0 && j >= 0 && i < cellsHorizontal && j < cellsVertical) {
                                    if (!(i == x && j == y)) {
                                        Cell curr = gameGrid[i][j];
                                        if (curr.isRevealed && !curr.isFlagged) candidates.add(curr);

                                    }
                                }
                            }
                        }



                        selected = null;
                        if (candidates.size() > 0) {
                            selected = candidates.get(candidates.size()-1);
                            for (int i = candidates.size()-2; i >= 0; i--) {
                                Cell curr = candidates.get(i);
                                if (curr.localCheatScore() < selected.localCheatScore())
                                    selected = curr;
                            }
                        }



                        if (selected !=null) {

                            double remainingBombs = selected.bombCount - selected.getFlaggedNeighbors();
                            double remainingSpots = selected.neighborCount() - selected.getRevealedNeighbors() - selected.getFlaggedNeighbors();

                            myPercentageChance = 100d / remainingSpots * remainingBombs;
                            if (myPercentageChance > 100 || myPercentageChance < 0) {
                                System.out.println("neighborCount() = " + neighborCount());
                                System.out.println("bombCount = " + bombCount);
                                System.out.println("getRevealedNeighbors() = " + getRevealedNeighbors());
                                System.out.println("remainingBombs: " + remainingBombs);
                                System.out.println("remainingSpots: " + remainingSpots);
                                System.out.println("myPercentageChance: " + myPercentageChance);

                            }
                        }

                    }



//                    cheatPercentageLabel.setText(Math.round(myPercentageChance) + "%");
                }
//            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cell cell = (Cell) o;
            return x == cell.x &&
                    y == cell.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Cell{" +
                    "x=" + x +
                    ", y=" + y +
                    ", bombCount=" + bombCount +
                    ", isBomb=" + isBomb +
                    ", isRevealed=" + isRevealed +
                    ", isFlagged=" + isFlagged +
                    '}';
        }
    }

    private static double map(double number, double minOne, double maxOne, double minTwo, double maxTwo) {
        return minTwo + (maxTwo - minTwo) * ((number - minOne) / (maxOne - minOne));
    }

}

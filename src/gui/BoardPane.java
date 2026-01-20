// language: java
package gui;

import core.Board;
import enums.Color;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class BoardPane extends Region {
    private final int size;
    private final BiConsumer<Integer,Integer> onCellClicked;
    private final Canvas canvas;

    // cache last drawn board & legal moves so we can redraw on resize
    private Board lastBoard = null;
    private List<int[]> lastLegalMoves = null;

    public BoardPane(int size, double initialCellSize, BiConsumer<Integer,Integer> onCellClicked) {
        this.size = size;
        this.onCellClicked = onCellClicked;
        this.canvas = new Canvas(size * initialCellSize, size * initialCellSize);

        // Allow mouse clicks on the canvas
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);

        getChildren().add(canvas);

        // Make this Region resizable
        setMinSize(0, 0);
        // do not set a fixed preferred size so parent layouts can shrink the region
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private void handleClick(MouseEvent e) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cell = Math.min(w, h) / size;
        double boardWidth = cell * size;
        double boardHeight = cell * size;
        double offsetX = (w - boardWidth) / 2.0;
        double offsetY = (h - boardHeight) / 2.0;

        int col = (int)((e.getX() - offsetX) / cell);
        int row = (int)((e.getY() - offsetY) / cell);

        if (row >= 0 && row < size && col >= 0 && col < size) {
            if (onCellClicked != null) onCellClicked.accept(row, col);
        }
    }

    @Override
    protected void layoutChildren() {
        double w = Math.max(0, getWidth());
        double h = Math.max(0, getHeight());
        // ensure canvas fills the region
        canvas.setWidth(w);
        canvas.setHeight(h);
        canvas.relocate(0, 0);
        if (lastBoard != null) {
            drawBoard(lastBoard, lastLegalMoves);
        } else {
            drawEmpty();
        }
    }


    public void drawBoard(Board board, List<int[]> legalMoves) {
        // cache state for future redraws
        this.lastBoard = board;
        this.lastLegalMoves = legalMoves;

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> drawBoard(board, legalMoves));
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cell = Math.min(w, h) / size;
        double boardWidth = cell * size;
        double boardHeight = cell * size;
        double offsetX = (w - boardWidth) / 2.0;
        double offsetY = (h - boardHeight) / 2.0;

        // clear background
        gc.setFill(javafx.scene.paint.Color.DARKGREEN);
        gc.fillRect(0, 0, w, h);

        // grid lines
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        for (int r = 0; r <= size; r++) {
            double y = offsetY + r * cell;
            gc.strokeLine(offsetX, y, offsetX + boardWidth, y);
        }
        for (int c = 0; c <= size; c++) {
            double x = offsetX + c * cell;
            gc.strokeLine(x, offsetY, x, offsetY + boardHeight);
        }

        // legal set
        Set<String> legalSet = null;
        if (legalMoves != null && !legalMoves.isEmpty()) {
            legalSet = new HashSet<>();
            for (int[] mv : legalMoves) {
                if (mv != null && mv.length >= 2) legalSet.add(mv[0] + "," + mv[1]);
            }
        }

        // pieces and markers
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                enums.Color col = board.getCell(r, c);
                double x = offsetX + c * cell;
                double y = offsetY + r * cell;

                boolean empty = (col == null || col == Color.EMPTY);
                if (empty && legalSet != null && legalSet.contains(r + "," + c)) {
                    gc.setFill(javafx.scene.paint.Color.LIGHTYELLOW);
                    double rad = cell * 0.12;
                    gc.fillOval(x + cell/2 - rad, y + cell/2 - rad, rad*2, rad*2);
                }

                if (col == Color.BLACK) {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.fillOval(x + cell*0.06, y + cell*0.06, cell * 0.88, cell * 0.88);
                } else if (col == Color.WHITE) {
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.fillOval(x + cell*0.06, y + cell*0.06, cell * 0.88, cell * 0.88);
                    gc.setStroke(javafx.scene.paint.Color.GRAY);
                    gc.strokeOval(x + cell*0.06, y + cell*0.06, cell * 0.88, cell * 0.88);
                }
            }
        }
    }

    public void drawEmpty() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::drawEmpty);
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.DARKGREEN);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}

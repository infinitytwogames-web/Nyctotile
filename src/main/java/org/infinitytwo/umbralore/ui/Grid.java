package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.builder.UIBuilder;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.joml.Vector2i;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Grid extends UI {
    protected Map<UI, Cell> uis = new ConcurrentHashMap<>();
    protected int columns;
    protected int rows;
    protected int margin;
    protected int padding;
    protected int cellSize;

    protected Grid(UIBatchRenderer renderer) {
        super(renderer);
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getMargin() {
        return margin;
    }

    public int getPadding() {
        return padding;
    }

    public UI get(int row, int column) {
        return get(row * columns + column);
    }

    public UI get(int index) {
        int i = 0;
        for (UI ui : uis.keySet()) {
            if (i++ == index)
                return ui;
        }
        return null;
    }

    private boolean layoutDirty = true;

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        layoutDirty = true;
    }

    @Override
    public void draw() {
        if (layoutDirty) {
            updateSize();
            layoutDirty = false;
        }
        super.draw();
        for (Map.Entry<UI, Cell> entry : uis.entrySet()) {
            UI ui = entry.getKey();
            Cell cell = entry.getValue();

            ui.setSize(cellSize);
            ui.setOffset(
                    (cell.x * (cellSize + margin)) + padding,
                    (cell.y * (cellSize + margin)) + padding
            );

            ui.draw();
        }
    }

    public int put(UI ui, int row, int column) {
//        if (row >= rows || column >= columns)
//            throw new IllegalArgumentException("Row/Column position out of bounds.");

        ui.anchor = anchor;
        ui.pivot = new Pivot(0, 0);
        ui.offset = new Vector2i(padding);
        ui.parent = this;

        uis.put(ui, new Cell(column, row)); // column = x, row = y
        return uis.size() - 1;
    }

    private void updateSize() {
        super.setSize(
                (columns * (cellSize + margin)) + (padding * 2),
                (rows * (cellSize + margin)) + (padding * 2)
        );
    }

    public void setCellSize(int size) {
        cellSize = size;
    }

    protected record Cell(int x, int y) {}

    public static class Builder<T extends Grid> extends UIBuilder<T> {

        public Builder(UIBatchRenderer renderer, T element) {
            super(renderer, element);
        }

        public Builder<T> rows(int rows) { ui.rows = rows; return this; }
        public Builder<T> columns(int columns) { ui.columns = columns; return this; }
        public Builder<T> cellSize(int size) { ui.cellSize = size; return this; }
        public Builder<T> margin(int margin) { ui.margin = margin; return this; }
        public Builder<T> padding(int padding) { ui.padding = padding; return this; }

        @Override
        public UIBuilder<T> applyDefault() {
            return this;
        }
    }
}

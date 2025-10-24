package org.infinitytwo.umbralore.core.ui.display;

import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.ui.UI;
import org.infinitytwo.umbralore.core.ui.builder.UIBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Grid extends UI {
    protected Map<UI, Cell> uis = new LinkedHashMap<>();
    protected int columns;
    protected int rows;
    protected int space;
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

    public int getSpace() {
        return space;
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
                    (cell.x * space) + (cell.x * (cellSize)) + padding,
                    (cell.y * space) + (cell.y * (cellSize)) + padding
            );

            ui.draw();
        }
    }

    public int put(UI ui, int row, int column) {
        ui.setParent(this);

        uis.put(ui, new Cell(column, row)); // column = x, row = y
        return uis.size() - 1;
    }

    public void updateSize() {
        super.setSize(
                (columns * space) + (columns * (cellSize)) + padding,
                (rows * space) + (rows * (cellSize)) + padding
        );
    }

    public void setCellSize(int size) {
        cellSize = size;
    }

    protected record Cell(int x, int y) {
    }

    public static class Builder<T extends Grid> extends UIBuilder<T> {

        public Builder(UIBatchRenderer renderer, T element) {
            super(element);
        }

        public Builder<T> rows(int rows) {
            ui.rows = rows;
            return this;
        }

        public Builder<T> columns(int columns) {
            ui.columns = columns;
            return this;
        }

        public Builder<T> cellSize(int size) {
            ui.cellSize = size;
            return this;
        }

        public Builder<T> margin(int margin) {
            ui.space = margin;
            return this;
        }

        public Builder<T> padding(int padding) {
            ui.padding = padding;
            return this;
        }

        @Override
        public UIBuilder<T> applyDefault() {
            return this;
        }
    }
}

package org.infinitytwo.umbralore.core.ui.display;

import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.ui.UI;
import org.infinitytwo.umbralore.core.ui.builder.UIBuilder;
import org.joml.Vector2i;

import java.util.LinkedHashMap;
import java.util.Map;

public class Grid extends UI {
    protected Map<UI, Cell> uis = new LinkedHashMap<>();
    protected int columns;
    protected int rows;
    protected int space;
    protected int padding;
    protected Vector2i cellSize = new Vector2i();
    protected Screen screen;

    public Grid(Screen renderer) {
        super(renderer.getUIBatchRenderer());
        screen = renderer;
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
    public void onMouseClicked(MouseButtonEvent e) {
    
    }
    
    @Override
    public void onMouseHover(MouseHoverEvent e) {
    
    }
    
    @Override
    public void onMouseHoverEnded() {
    
    }
    
    @Override
    public void cleanup() {
    
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
                    (cell.x * space) + (cell.x * (cellSize.x)) + padding,
                    (cell.y * space) + (cell.y * (cellSize.y)) + padding
            );
        }
    }

    public int put(UI ui, int row, int column) {
        ui.setParent(this);
        screen.register(ui);

        uis.put(ui, new Cell(column, row)); // column = x, row = y
        return uis.size() - 1;
    }

    public void updateSize() {
        super.setSize(
                (columns * space) + (columns * (cellSize.x)) + padding,
                (rows * space) + (rows * (cellSize.y)) + padding
        );
    }

    public void setCellSize(int size) {
        setCellSize(size,size);
    }
    
    public void setCellSize(int width, int height) {
        cellSize.set(width,height);
    }

    protected record Cell(int x, int y) {
    }

    public static class Builder<T extends Grid> extends UIBuilder<T> {

        public Builder(T element) {
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
            ui.cellSize.set(size);
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
        public Builder<T> applyDefault() {
            return this;
        }
        
        public Builder<T> cellSize(int width, int height) {
            ui.setCellSize(width,height);
            return this;
        }
    }

    public void clearCells() {
        uis.clear();
    }
}

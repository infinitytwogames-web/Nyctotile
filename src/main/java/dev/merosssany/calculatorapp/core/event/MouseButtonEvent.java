package dev.merosssany.calculatorapp.core.event;

public class MouseButtonEvent extends Event {
    private final int button;
    private final int action;

    public MouseButtonEvent(int button, int action) {
        this.button = button;
        this.action = action;
    }

    public int getButton() {
        return button;
    }

    public int getAction() {
        return action;
    }
}


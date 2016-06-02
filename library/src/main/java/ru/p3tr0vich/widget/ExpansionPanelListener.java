package ru.p3tr0vich.widget;

public interface ExpansionPanelListener {
    void onExpanding(ExpansionPanel panel);

    void onCollapsing(ExpansionPanel panel);

    void onExpanded(ExpansionPanel panel);

    void onCollapsed(ExpansionPanel panel);
}
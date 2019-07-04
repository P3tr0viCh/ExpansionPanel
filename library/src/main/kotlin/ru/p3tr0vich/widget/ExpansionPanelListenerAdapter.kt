package ru.p3tr0vich.widget

abstract class ExpansionPanelListenerAdapter : ExpansionPanelListener {
    override fun onExpanding(panel: ExpansionPanel) {}

    override fun onCollapsing(panel: ExpansionPanel) {}

    override fun onExpanded(panel: ExpansionPanel) {}

    override fun onCollapsed(panel: ExpansionPanel) {}
}
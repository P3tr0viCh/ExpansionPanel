package ru.p3tr0vich.widget

interface ExpansionPanelListener {
    fun onExpanding(panel: ExpansionPanel)

    fun onCollapsing(panel: ExpansionPanel)

    fun onExpanded(panel: ExpansionPanel)

    fun onCollapsed(panel: ExpansionPanel)
}
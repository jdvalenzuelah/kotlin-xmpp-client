package ui.textUi

data class Option(val label: String, override val final: Boolean = false, val action: () -> Unit): IMenuOption {
    override fun render(): String  = label

    override operator fun invoke() = action()
}


package ui.textUi

data class Option(val label: String, override val final: Boolean = false, val action: (Int) -> Unit): IMenuOption {

    override fun render(): String  = label

    override fun execute(id: Int) = action(id)

}

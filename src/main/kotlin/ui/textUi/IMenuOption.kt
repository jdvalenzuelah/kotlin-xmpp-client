package ui.textUi

interface IMenuOption {
    val final: Boolean
    fun render(): String
    fun execute(id: Int)
}

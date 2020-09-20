package ui.textUi

interface IMenuOption {
    val final: Boolean
    fun render(): String
    operator fun invoke()
}

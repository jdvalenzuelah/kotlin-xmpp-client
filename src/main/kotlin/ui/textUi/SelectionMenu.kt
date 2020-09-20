package ui.textUi

class SelectionMenu(
    private val title: String,
    private val options: List<IMenuOption>,
    private val instructions: String?
): IMenuOption, ILoopedOption {

    override val final: Boolean = false

    private var run = true

    private fun renderOption(index: Int, option: IMenuOption) = "\t${index+1}. ${option.render()}"

    override fun render(): String {
        val opts = options
            .mapIndexed { index, option -> renderOption(index, option) }
            .joinToString(separator = "\n")

        val baseRender = "$title\n$opts"
        return instructions?.let { "$baseRender\n$it" } ?: baseRender

    }

    private fun validateSelectionInput(input: String): Boolean {
        val index = input.toIntOrNull() ?: return false
        return index > 0 && index <= options.size
    }

    private fun invalidOption() = "Opcion ingresada no es valida! porfavor intente de nuevo."

    override fun stop() {
        this.run = false
    }

    override fun start() {
        while(run){
            println(this.render())
            val selection = readLine() ?: continue
            if(validateSelectionInput(selection)){
                val option = options[selection.toInt() - 1]
                option()
                if(option.final)
                    stop()
            }
            else
                println(invalidOption())
        }
    }

    override operator fun invoke() = start()

    @MenuDsl
    class MenuBuilder {
        private val options = mutableListOf<IMenuOption>()
        private var title: String = ""
        private var help: String? = null

        operator fun IMenuOption.unaryPlus() {
            options.add(this)
        }

        fun title(init: () -> String) {
            this.title = init()
        }

        fun help(init: () -> String) {
            this.help = init()
        }

        fun build(): SelectionMenu = SelectionMenu(title, options, help)

    }

}

@DslMarker private annotation class MenuDsl

fun menu(init: SelectionMenu.MenuBuilder.() -> Unit) = SelectionMenu.MenuBuilder().apply(init).build()

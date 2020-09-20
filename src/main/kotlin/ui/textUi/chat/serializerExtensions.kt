package ui.textUi.chat

import chat.core.management.models.RegisteredUser

private class TableGenerator(header: List<String>) {

    data class Column(val label: String, var minWidth: Int)

    private val cols = mapCols(header)

    private val rows = mutableListOf<List<String>>()

    init {
        require(header.isNotEmpty()) { "Header cannot be empty!" }
    }

    private fun mapCols(args: List<String>) = args.map { Column(it, it.length + 2) }

    private fun updateCols(args: List<String>) {
        for(i in 0..cols.size) {
            if(i == args.size)
                break

            if(args[i].length > cols[i].minWidth)
                cols[i].minWidth = args[i].length + 2

        }
    }

    fun addRow(vararg values: String) = addRow(values.toList())

    fun addRow(args: List<String>) {
        rows.add(args)
        updateCols(args)
    }


    private fun getTop() = "╔" + cols.map { "═".repeat(it.minWidth) }.joinToString(separator = "╦") + "╗"
    private fun getMiddle() = "╠" + cols.map { "═".repeat(it.minWidth) }.joinToString(separator = "╬") + "╣"
    private fun getBottom() = "╚" + cols.map { "═".repeat(it.minWidth) }.joinToString(separator = "╩") + "╝"


    private fun renderHeader() =  "║" + cols.map { it.label.fill(it.minWidth) }.joinToString(separator = "║") + "║"

    private fun String.fill(target: Int): String {
        return if(target > this.length)
            this + " ".repeat(target - this.length)
        else
            this
    }

    private fun renderRow(vals: List<String>): String = vals.take(cols.size)
        .mapIndexed { index, value -> value.fill(cols[index].minWidth) }
        .joinToString(separator = "║")

    private fun renderRows() = rows.map { "║" + renderRow(it) + "║"}.joinToString(separator = "\n")

    fun render(): String {

        val header = getTop() + "\n" + renderHeader() + "\n" + getMiddle()
        val body = renderRows()
        val footer = getBottom()

        return header + "\n" +  body + "\n" + footer
    }

    companion object {
        fun withHeader(vararg cols: String) = TableGenerator(cols.toList())
    }
}

fun List<RegisteredUser>.asUnicodeTable(): String {
    val generator = TableGenerator.withHeader("Nombre de usuario", "Nombre", " Email")
    this.forEach {
        generator.addRow(it.username, it.name ?: "", it.email ?: "")
    }
    return generator.render()
}


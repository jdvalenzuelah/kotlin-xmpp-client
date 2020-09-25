package ui

import chat.core.management.Chat
import chat.core.management.SecurityMode
import chat.core.management.connection
import chat.core.management.withChat
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import ui.textUi.chat.ChatUI

data class Args(val parser: ArgParser) {
    val domain by parser.storing("--domain", help = "Chat server domain").default("redes2020.xyz")

    val host by parser.storing("--host", help = "Chat server host").default("redes2020.xyz")

    val secureMode by parser.mapping(
        "--secure" to SecurityMode.required,
        "--insecure" to SecurityMode.disabled,
        help = "Security mode"
    )
        .default(SecurityMode.disabled)
}

fun main(args: Array<String>) = mainBody {
    val arguments = Args(ArgParser(args))

    val conn = connection {
        config {
            domain = arguments.domain
            host = arguments.host
            securityMode = arguments.secureMode
        }
    }
    val chat = Chat(conn)
    ChatUI(chat).start()

}

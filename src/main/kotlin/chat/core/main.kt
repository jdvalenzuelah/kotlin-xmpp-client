package chat.core

import chat.core.management.*

fun main() {

    val conn = connection {
        config {
            user(username = "test", password = "test")
            domain = "jabber.hot-chilli.net"
            host = "jabber.hot-chilli.net"
        }
    }

    conn.connect()
    conn.login()
    conn.disconnect()

    val accountManager = accountManager(conn)

    accountManager.createAccount("test", "test")

    conn.connect()
    conn.login("test", "test")

    val chatManager = chatManager(conn)

    val chatU = chatManager.chatWith("test") { _, msg, chat ->
        println(msg.from)
        println(msg.body)
        chat.send("response")
    }

    chatU.send("Hello world!")

    while (true)
        continue

}

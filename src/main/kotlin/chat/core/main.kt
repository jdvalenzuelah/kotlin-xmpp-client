package chat.core

import chat.core.management.*

fun main() {
    val conn = connection {
        config {
            domain = "redes2020.xyz"
            host = "redes2020.xyz"
            securityMode = SecurityMode.disabled
        }
    }

    withChat(conn) {
        //createAccount("test12354551111111", "test31213255")
        login("test12354555", "test31213255")
        getRegisteredUsers()
        logout()
    }

}

package chat.core

import chat.core.management.*
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level


fun main() {

    Configurator.currentConfig()
        .level(Level.DEBUG)
        .activate()

    val conn = connection {
        config {
            domain = "redes2020.xyz"
            host = "redes2020.xyz"
            securityMode = SecurityMode.disabled
        }
    }

    withChat(conn) {
        createAccount("test12354551111111", "test31213255")
        login("test12354555", "test31213255")
        getRegisteredUsers()
        logout()
    }

}

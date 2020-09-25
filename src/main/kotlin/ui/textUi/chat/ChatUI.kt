package ui.textUi.chat

import chat.core.management.Chat
import chat.core.management.MessageHandler
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.Jid
import org.pmw.tinylog.Logger
import ui.textUi.Option
import ui.textUi.menu

class ChatUI(chat: Chat) {

    data class ChatWrapper(val jid: EntityBareJid, val message: Message, val chat: org.jivesoftware.smack.chat2.Chat) {
        fun render(): String {
            return "De: $jid\nMensaje: ${message.body}"
        }
    }

    data class RoomMsgWrapper(val message: Message, val mcu: MultiUserChat) {
        fun render(): String {
            return "De: ${message.from}\nMensaje: ${message.body}"
        }
    }

    data class Roster(val jid: Jid, val presence: Presence?)

    private val messagePool = mutableListOf<ChatWrapper>()
    private val roomMessagePool = mutableListOf<RoomMsgWrapper>()
    private val rosterPool = mutableListOf<Roster>()

    private val rosterListener = object : RosterListener {
        override fun entriesAdded(addresses: MutableCollection<Jid>?) {
            addresses
                ?.map { Roster(it, null) }
                ?.also { rosterPool.addAll(it) }
        }

        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
            if(addresses != null)
                rosterPool.removeIf { it.jid in addresses }
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            Logger.warn("Not sure what to do with updates!")
        }

        override fun presenceChanged(presence: Presence?) {
            println(presence)
            if(presence == null)
                return
            val foundIndex = rosterPool.indexOfFirst { it.jid.localpartOrNull == presence.from.localpartOrNull }

            if(foundIndex != -1) {
                rosterPool[foundIndex] = rosterPool[foundIndex].copy(presence = presence)
            }

        }
    }

    private val startMenu = menu {
        title { "Chat Redes 2020" }

        help { "Ingrese una opcion: " }

        +Option("Iniciar sesion.", final = true) {
            val credentials = getLogInInfo()

            showMenuOnSuccess {
                executeAndLogError("Usuario o contraseña incorrectos") {
                    chat.login(credentials.first, credentials.second)
                }
            }

        }

        +Option("Crear cuenta.", final = true) {
            val credentials = getLogInInfo()
            showMenuOnSuccess {
                tryExecuteAndLogError("Usuario ya esta en uso!") {
                    chat.createAccount(credentials.first, credentials.second)
                    chat.login(credentials.first, credentials.second)
                }
            }
        }

        +Option("Salir", final = true){}
    }

    private val loggedInMenu = menu {
        title { "Bienvenido" }

        help { "Ingrese una opcion: " }

        +Option("Mostrar usuarios registrados.") {
            tryExecuteAndLogError("No se pudieron obtener los usuarios registrados") {
                val table = chat.getRegisteredUsers().asUnicodeTable("Nombre de usuario", "Nombre", " Email") {
                    listOf(it.username, it.name ?: "", it.email ?: "")
                }
                println(table)
                enterToContinue()
                true
            }
        }

        +Option("Mostrar contactos.") {
            tryExecuteAndLogError("No se pudieron obtener los usuarios registrados") {
                val table = chat.getContacts().asUnicodeTable("Nombre", "JID") {
                    listOf(it.contactName, it.jid.toString())
                }
                println(table)
                enterToContinue()
                true
            }
        }

        +Option("Agregar contactos.") {
            tryExecuteAndLogError("No se pudieron obtener los usuarios registrados") {
                val userName = getNonEmptyString("Ingrese nombre de usuario: ")
                val contactName = getNonEmptyString("Ingrese el nombre del contacto: ")
                chat.addContact(userName, contactName)
            }
        }

        +Option("Nuevo chat") {
            tryExecuteAndLogError("No se pudo iniciar el chat") {
                val user = getNonEmptyString("Ingrese nombre de usuario: ")
                val dm = chat.chatWith(user, defaultMessageHandler)
                val msg = getNonEmptyString("Ingrese mensaje a enviar: ")
                dm.send(msg)
                true
            }
        }

        +Option("Ver notificaciones:") {
            tryExecuteAndLogError("No se pudo mostrar notificaciones") {
                if(messagePool.isNotEmpty()) {
                    println("Mensajes nuevos: ")
                    val renderedPool = messagePool
                        .mapIndexed { index, chatWrapper -> listOf("${index + 1}", chatWrapper.jid.toString()) }
                        .asUnicodeTable("No.", "De"){ it }
                    println(renderedPool)
                    menu {
                        help { "Ingrese notificacion a expandir: " }
                        +Option("", final = true) { pos ->
                            val message = messagePool.removeAt(pos)
                            println(message.render())
                            val res = getNonEmptyString("Desea continuar? y/N")
                            if(res.toLowerCase() in listOf("yes", "y")) {
                                val reply = getNonEmptyString("Ingrese mensage: ")
                                message.chat.send(reply)
                            }
                        }
                    }.start()
                } else {
                    println("No hay notificaciones a mostrar!")
                    enterToContinue()
                }
                true
            }
        }

        +Option("Ver estado de usuarios."){
            tryExecuteAndLogError("Error inesperado, intente de nuevo mas tarde!"){
                val renderedRoster = rosterPool
                    .asUnicodeTable("Usuario.", "Tipo", "Estado"){
                        listOf(it.jid.toString(), it.presence?.type?.toString() ?: "N/A", it.presence?.status ?: "N/A")
                    }
                println(renderedRoster)
                enterToContinue()
                true
            }
        }

        +Option("Cambiar estado") {
            tryExecuteAndLogError("Error inesperado intente de nuevo mas tarde!") {
                menu {
                    help { "Ingrese una opcion: " }
                    +Option("Disponible", final = true) {
                        chat.sendPresence(Presence.Type.available, 50, "Available", Presence.Mode.available)
                    }

                    +Option("No Disponible", final = true) {
                        chat.sendPresence(Presence.Type.unavailable, 50, "Available", Presence.Mode.away)
                    }
                }.start()
                true
            }
        }

        +Option("Crear nueva sala.") {
            tryExecuteAndLogError("Error inesperado, intente de nuevo mas tarde") {
                val rname = getNonEmptyString("Ingrese el nombre de la sala: ")
                val muc = chat.chatGroup(rname) { msg, mcu ->
                    roomMessagePool.add(RoomMsgWrapper(msg, mcu))
                }
                val cont = getNonEmptyString("Enviar mensaje? y/N")

                if(cont.toLowerCase() in listOf("y", "yes")) {
                    val msg = getNonEmptyString("Ingrese mensaje a enviar: ")
                    muc.sendMessage(msg)
                }

                true
            }
        }

        +Option("Entrar a una sala.") {
            tryExecuteAndLogError("Error inesperado, intente de nuevo mas tarde") {
                val rname = getNonEmptyString("Ingrese el nombre de la sala: ")
                val muc = chat.joinChatGroup(rname) { msg, mcu ->
                    roomMessagePool.add(RoomMsgWrapper(msg, mcu))
                }

                val cont = getNonEmptyString("Enviar mensaje? y/N")

                if(cont.toLowerCase() in listOf("y", "yes")) {
                    val msg = getNonEmptyString("Ingrese mensaje a enviar: ")
                    muc.sendMessage(msg)
                }
                true
            }
        }

        +Option("Ver notificaciones grupales:") {
            tryExecuteAndLogError("No se pudo mostrar notificaciones") {
                if(roomMessagePool.isNotEmpty()) {
                    println("Mensajes nuevos: ")
                    val renderedPool = roomMessagePool
                        .mapIndexed { index, chatWrapper -> listOf("${index + 1}", chatWrapper.message.from.toString()) }
                        .asUnicodeTable("No.", "De"){ it }
                    println(renderedPool)
                    menu {
                        help { "Ingrese notificacion a expandir: " }
                        +Option("", final = true) { pos ->
                            val message = roomMessagePool.removeAt(pos)
                            println(message.render())
                            val res = getNonEmptyString("Desea continuar? y/N")
                            if(res.toLowerCase() in listOf("yes", "y")) {
                                val reply = getNonEmptyString("Ingrese mensage: ")
                                message.mcu.sendMessage(reply)
                            }
                        }
                    }.start()
                } else {
                    println("No hay notificaciones a mostrar!")
                    enterToContinue()
                }
                true
            }
        }

        +Option("Cerrar sesion.", final = true){
            tryExecuteAndLogError("No se pudo eliminar la cuenta"){
                chat.logout()
                true
            }
        }

        +Option("Eliminar cuenta.", final = true){
            tryExecuteAndLogError("No se pudo eliminar la cuenta"){
                chat.deleteAccount()
            }
        }

    }

    private val defaultMessageHandler: MessageHandler = fun (jid: EntityBareJid, message: Message, chat: org.jivesoftware.smack.chat2.Chat) {
        Logger.info("Nuevo mensaje $message")
        messagePool.add(ChatWrapper(jid, message, chat))
    }

    init {
        chat.updateRosterListener(rosterListener)
    }

    private fun showMenuOnSuccess(action: () -> Boolean) {
        if(action())
            loggedInMenu.start()
    }

    private fun executeAndLogError(errorMsg: String, action: () -> Boolean): Boolean = action().also {
        if(!it)
            println(errorMsg)
    }

    private fun tryExecuteAndLogError(errorMsg: String, action: () -> Boolean): Boolean = try {
        executeAndLogError(errorMsg, action)
    }catch (e: Exception) {
        println("Error inesperado, por favor intente de nuevo mas tarde")
        Logger.error(e)
        false
    }


    private fun whileNullOrEmpty(action: () -> String?): String {
        var value: String?
        do {
            value = action()
        } while (value.isNullOrEmpty())
        return value
    }

    private fun getLogInInfo(): Pair<String, String> {
        val userName = getNonEmptyString("Ingrese nombre de usuario: ")
        val password = getNonEmptyString("Ingrese Contraseña: ")
        return Pair(userName, password)
    }

    private fun getNonEmptyString(msg: String): String {
        return whileNullOrEmpty {
            println(msg)
            readLine()
        }
    }

    private fun enterToContinue() {
        println("Presione enter para continuar")
        readLine()
    }

    fun start() {
        startMenu.start()
    }

}

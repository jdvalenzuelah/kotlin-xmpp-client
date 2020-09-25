package ui.textUi.chat

import chat.core.management.Chat
import chat.core.management.MessageHandler
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.RosterListener
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

    private val messagePool = mutableListOf<ChatWrapper>()
    private val rosterPool = mutableListOf<Jid>()

    private val rosterListener = object : RosterListener {
        override fun entriesAdded(addresses: MutableCollection<Jid>?) {
            if(addresses != null)
                rosterPool.addAll(addresses)
        }

        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
            if(addresses != null)
                rosterPool.removeAll(addresses)
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            if(addresses != null)
                rosterPool.removeAll(addresses)
        }

        override fun presenceChanged(presence: Presence?) {
            TODO("Not yet implemented")
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
                true
            }
        }

        +Option("Mostrar contactos.") {
            tryExecuteAndLogError("No se pudieron obtener los usuarios registrados") {
                val table = chat.getContacts().asUnicodeTable("Nombre", "JID") {
                    listOf(it.contactName, it.jid.toString())
                }
                println(table)
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
                    }
                }.start()
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

    fun start() {
        startMenu.start()
    }

}

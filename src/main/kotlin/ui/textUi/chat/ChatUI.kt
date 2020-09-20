package ui.textUi.chat

import chat.core.management.Chat
import org.pmw.tinylog.Logger
import ui.textUi.Option
import ui.textUi.menu

class ChatUI(chat: Chat) {

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
                println(chat.getRegisteredUsers().asUnicodeTable())
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

    private fun showMenuOnSuccess(action: () -> Boolean) {
        if(action())
            loggedInMenu()
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

    private fun getLogInInfo(): Pair<String, String> {
        var userName: String?
        var password: String?
        do {
            print("Ingrese nombre de usuario: ")
            userName = readLine()
            print("Ingrese Contraseña: ")
            password = readLine()
            val shouldRepeat = userName.isNullOrEmpty() || password.isNullOrEmpty()
            if(shouldRepeat)
                println("Usuario/Contraseña no pueden estar vacios! intente de nuevo")
        } while(shouldRepeat)
        return Pair(userName!!, password!!)
    }

    fun start() {
        startMenu()
    }

}

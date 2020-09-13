package chat.core.management

import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.StanzaError
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.sasl.SASLErrorException
import org.jivesoftware.smackx.iqregister.AccountManager
import org.jivesoftware.smackx.search.ReportedData
import org.jivesoftware.smackx.search.UserSearchManager
import org.jivesoftware.smackx.xdata.Form
import org.jxmpp.jid.DomainBareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.parts.Localpart
import org.pmw.tinylog.Logger

class Chat(private val conn: Connection) {

    private var loggedIn: Boolean = false

    private val managers = object {
        var accountManager = AccountManager.getInstance(conn)
        var chatManager = ChatManager.getInstanceFor(conn)
        var roster = Roster.getInstanceFor(conn)
        var userSearch = UserSearchManager(conn)

        fun update() {
            accountManager = AccountManager.getInstance(conn)
            chatManager = ChatManager.getInstanceFor(conn)
            roster = Roster.getInstanceFor(conn)
            userSearch = UserSearchManager(conn)
        }
    }

    private val rosterListenerManager = object : RosterListener {
        override fun entriesAdded(addresses: MutableCollection<Jid>?) {
            TODO("Not yet implemented")
        }

        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
            TODO("Not yet implemented")
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            TODO("Not yet implemented")
        }

        override fun presenceChanged(presence: Presence?) {
            TODO("Not yet implemented")
        }
    }

    init {
        Logger.debug("Starting chat...")

        ensureConnection()

        if(!conn.configuration.username.isNullOrEmpty()) {
            login()
        } else
            Logger.debug("No user defined, skipping logging")

        if(conn.configuration.securityMode == SecurityMode.disabled)
            disableSecureMode()
    }

    private fun toggleLoggedIn() {
        loggedIn = !loggedIn
    }

    private fun XMPPConnection.isNotConnected() =  !this.isConnected

    private fun ensureConnection() {
        if(conn.isNotConnected()) {
            Logger.debug("Connecting to server...")
            conn.connect()
            managers.update()
        }
    }

    private fun disableSecureMode() {
        Logger.warn("Secure mode disable, allowing account operations over insecure connection")
        managers.accountManager.sensitiveOperationOverInsecureConnection(true)
    }

    private fun senStanza(stanza: Stanza) = executeIfLoggedIn { conn.sendStanza(stanza) }

    private fun sendPresence(type: Presence.Type) = senStanza(Presence(type))

    private fun sendPresence(type: Presence.Type, priority: Int, status: String, mode: Presence.Mode) =
        senStanza(Presence(type, status, priority, mode))

    private inline fun <T> executeIf(con: Boolean, block: () -> T): T? = if(con) block() else null

    private fun <T> executeIfLoggedIn(block: () -> T): T? = executeIf(loggedIn) { block() }

    private fun <T> executeIfNotLoggedIn(block: () -> T): T? = executeIf(!loggedIn) { block() }

    private fun <T> tryLogin(block: () -> T?): T? = try {
        block()
    } catch (e: SASLErrorException) {
        Logger.error("Unauthorized!", e)
        null
    }

    private fun login(): Boolean {
        Logger.info("Logging in")
        val res = tryLogin {
            toggleLoggedIn()
            conn.login()
            managers.update()
            available()
        }
        return res != null
    }

    fun login(username: String, password: String): Boolean {
        ensureConnection()
        Logger.info("Logging in as $username")
        val res = tryLogin {
            conn.login(username, password)
            toggleLoggedIn()
            managers.update()
            available()
        }
        return res != null
    }

    fun available() {
        executeIfLoggedIn {
            sendPresence(Presence.Type.available, 50, "Available", Presence.Mode.available)
        }
    }

    fun logout() {
        Logger.info("Logging out...")
        executeIfLoggedIn {
            Logger.debug("Sending unavailable presence")
            sendPresence(Presence.Type.unavailable)
            Logger.debug("Disconnecting from server")
            conn.disconnect()
            toggleLoggedIn()

            Logger.info("Reconnecting to server")
            ensureConnection()
        }
    }

    @Throws(AccountCreationException::class)
    fun createAccount(username: String, password: String) {
        executeIfNotLoggedIn {
            Logger.info("Creating new account $username")
            try {
                managers.accountManager.createAccount(Localpart.from(username), password)
            }catch (e: XMPPException.XMPPErrorException) {
                Logger.error("Error received from server ${e.message}")
                throw when(e.stanzaError.condition) {
                    StanzaError.Condition.conflict -> AccountConflict("Account conflict, username already used!")
                    else -> ServerException("Server returned error: ${e.message}")
                }
            }
        }
    }

    fun deleteAccount() {
        executeIfLoggedIn {
            Logger.warn("Deleting account!")
            managers.accountManager.deleteAccount()
        }
            ?: Logger.error("Must be logged in to delete account!")
    }


    private fun search(searchService: DomainBareJid, searchManager: UserSearchManager, init: Form.() -> Unit): ReportedData  {
        val searchForm = searchManager.getSearchForm(searchService)
        val answerForm = searchForm.createAnswerForm()
        answerForm.init()
        return  searchManager.getSearchResults(answerForm, searchService)
    }

    fun getRegisteredUsers() {
        val searchService = managers.userSearch.getSearchService()
            ?: throw NoSearchServiceFoundException("No search service found!")

        val data = search(searchService, managers.userSearch) {
            setAnswer("Username", true)
            setAnswer("Email", true)
            setAnswer("Name", true)
            setAnswer("search", "*")
        }

        data.rows.forEach { println(it.getValues("Username") + it.getValues("Email") + it.getValues("Name")) }

    }

    fun disconnect() {
        conn.disconnect()
    }
}

private fun UserSearchManager.getSearchService() = searchServices.firstOrNull{ it.domain.startsWith("search") }

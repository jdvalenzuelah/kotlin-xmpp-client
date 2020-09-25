package chat.core.management

import chat.core.management.models.Contact
import chat.core.management.models.RegisteredUser
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.StanzaError
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.sasl.SASLErrorException
import org.jivesoftware.smackx.filetransfer.FileTransferManager
import org.jivesoftware.smackx.iqregister.AccountManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.search.ReportedData
import org.jivesoftware.smackx.search.UserSearchManager
import org.jivesoftware.smackx.xdata.Form
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.DomainBareJid
import org.jxmpp.jid.EntityFullJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Localpart
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.util.XmppStringUtils
import org.pmw.tinylog.Logger
import java.io.File

class Chat(private val conn: Connection) {

    private var loggedIn: Boolean = false

    private var userName: String = ""

    private val managers = object {
        var accountManager = AccountManager.getInstance(conn)
        var chatManager = ChatManager.getInstanceFor(conn)
        var roster = Roster.getInstanceFor(conn)
        var userSearch = UserSearchManager(conn)
        var fileTransferManager = FileTransferManager.getInstanceFor(conn)
        var multiUserManager = MultiUserChatManager.getInstanceFor(conn)

        fun update() {
            accountManager = AccountManager.getInstance(conn)
            chatManager = ChatManager.getInstanceFor(conn)
            roster = Roster.getInstanceFor(conn).also { it.addRosterListener(rosterListenerManager) }
            userSearch = UserSearchManager(conn)
            fileTransferManager = FileTransferManager.getInstanceFor(conn)
            multiUserManager = MultiUserChatManager.getInstanceFor(conn)
        }
    }

    private var rosterListenerManager: RosterListener = object : RosterListener {
        override fun entriesAdded(addresses: MutableCollection<Jid>?) {
           Logger.info(addresses)
        }

        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
            Logger.info(addresses)
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            Logger.info(addresses)
        }

        override fun presenceChanged(presence: Presence?) {
            Logger.info(presence)
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

    fun updateRosterListener(new: RosterListener) {
        rosterListenerManager = new
        managers.update()
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

    private fun sendStanza(stanza: Stanza) = executeIfLoggedIn { conn.sendStanza(stanza) }

    private fun sendPresence(type: Presence.Type) = sendStanza(Presence(type))

    private fun sendPresence(type: Presence.Type, init: Presence.() -> Unit) {
        val p = Presence(type)
        p.init()
        sendStanza(p)
    }

    fun sendPresence(type: Presence.Type, priority: Int, status: String, mode: Presence.Mode) =
        sendStanza(Presence(type, status, priority, mode))

    private fun subscribe(to: BareJid) = sendPresence(Presence.Type.subscribe) {
        setTo(to)
    }

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
        tryLogin {
            conn.login(username, password)
            toggleLoggedIn()
            userName = username
            managers.update()
            available()
        }
        return loggedIn
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
    fun createAccount(username: String, password: String): Boolean {
        return executeIfNotLoggedIn {
            Logger.info("Creating new account $username")
            try {
                managers.accountManager.createAccount(Localpart.from(username), password)
                true
            }catch (e: XMPPException.XMPPErrorException) {
                Logger.error("Error received from server ${e.message}")
                throw when(e.stanzaError.condition) {
                    StanzaError.Condition.conflict -> AccountConflict("Account conflict, username already used!")
                    else -> ServerException("Server returned error: ${e.message}")
                }
            }
        }
            ?: false
    }

    fun deleteAccount(): Boolean {
        return executeIfLoggedIn {
            Logger.warn("Deleting account!")
            managers.accountManager.deleteAccount()
            true
        }
            ?: run {
                Logger.error("Must be logged in to delete account!")
                false
            }
    }


    private fun search(searchService: DomainBareJid, searchManager: UserSearchManager, init: Form.() -> Unit): ReportedData  {
        val searchForm = searchManager.getSearchForm(searchService)
        val answerForm = searchForm.createAnswerForm()
        answerForm.init()
        return  searchManager.getSearchResults(answerForm, searchService)
    }

    fun getRegisteredUsers(): List<RegisteredUser> {
        return executeIfLoggedIn {
            val searchService = managers.userSearch.getSearchService()
                ?: throw NoSearchServiceFoundException("No search service found!")

            val data = search(searchService, managers.userSearch) {
                setAnswer("Username", true)
                setAnswer("Email", true)
                setAnswer("Name", true)
                setAnswer("search", "*")
            }

            data.rows.map {
                RegisteredUser(
                    username = it.getValues("Username").toString(),
                    email = it.getValues("Email")?.toString(),
                    name = it.getValues("Name")?.toString()
                )
            }
        }
            ?: emptyList()

    }

    fun addContact(username: String, contactName: String, autoSubscribe: Boolean = true): Boolean {
        return executeIfLoggedIn {
            val contactJid = JidCreate.bareFrom(username)
            managers.roster.createEntry(contactJid, contactName, null)
            if(autoSubscribe)
                subscribe(contactJid)
            true
        }
            ?: false
    }

    fun getContacts(): List<Contact> {
        return executeIfLoggedIn {
            managers.roster.entries.map {
                Contact(it.name, it.jid)
            }
        }
            ?: emptyList()
    }

    fun getContactDetails(contact: BareJid): Contact? {
        return executeIfLoggedIn {
            managers.roster.getEntry(contact)
                ?.let { Contact(it.name, it.jid) }
        }
    }

    fun chatWith(user: String, handler: MessageHandler): Chat {
        val jid = JidCreate.entityBareFrom(user)
        return managers.chatManager.chatWith(jid).also {
            managers.chatManager.addIncomingListener(IncomingChatMessageListener(handler))
        }
    }

    fun chatGroup(roomName: String, handler: (Message, MultiUserChat) -> Unit): MultiUserChat {

        val rn = if(roomName.endsWith("@conference.redes2020.xyz")) roomName else "$roomName@conference.redes2020.xyz"

        val muc = managers.multiUserManager.getMultiUserChat(JidCreate.entityBareFrom(rn))

        muc.addMessageListener { msg ->
            handler(msg, muc)
        }

        muc.create(Resourcepart.from(userName))
            .makeInstant()

        return muc

    }

    fun joinChatGroup(roomName: String, handler: (Message, MultiUserChat) -> Unit): MultiUserChat {

        val rn = if(roomName.endsWith("@conference.redes2020.xyz")) roomName else "$roomName@conference.redes2020.xyz"

        val muc = managers.multiUserManager.getMultiUserChat(JidCreate.entityBareFrom(rn))

        muc.addMessageListener { msg ->
            handler(msg, muc)
        }

        muc.join(Resourcepart.from(userName))

        return muc
    }

    fun disconnect() {
        conn.disconnect()
    }
}

private fun UserSearchManager.getSearchService() = searchServices.firstOrNull{ it.domain.startsWith("search") }

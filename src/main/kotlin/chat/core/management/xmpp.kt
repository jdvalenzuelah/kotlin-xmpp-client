package chat.core.management

import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.iqregister.AccountManager
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Localpart

fun connection(init: ConnectionBuilder.() -> Unit): XMPPTCPConnection {
    val conn = ConnectionBuilder()
    conn.init()
    return conn.build()
}

typealias MessageHandler = (jid: EntityBareJid, message: Message, chat: Chat) -> Unit

fun ChatManager.chatWith(user: String, handler: MessageHandler): Chat {
    val jid = JidCreate.entityBareFrom(user)
    return  chatWith(jid).also { addIncomingListener(IncomingChatMessageListener(handler)) }
}

fun chatManager(conn: XMPPConnection): ChatManager = ChatManager.getInstanceFor(conn)

fun AccountManager.createAccount(user: String, password: String) {
    createAccount(Localpart.from(user), password)
}

fun accountManager(conn: XMPPConnection): AccountManager = AccountManager.getInstance(conn)

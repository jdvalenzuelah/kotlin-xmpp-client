package chat.core.management

import org.jivesoftware.smack.ConnectionConfiguration
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

typealias  SecurityMode = ConnectionConfiguration.SecurityMode

typealias Connection = XMPPTCPConnection

fun ChatManager.chatWith(user: String, handler: MessageHandler): Chat {
    val jid = JidCreate.entityBareFrom(user)
    return  chatWith(jid).also { addIncomingListener(IncomingChatMessageListener(handler)) }
}

fun AccountManager.createAccount(user: String, password: String) {
    createAccount(Localpart.from(user), password)
}

fun withChat(conn: Connection, block: chat.core.management.Chat.() -> Unit ) {
    val chat = Chat(conn)
    chat.block()
    chat.disconnect()
}

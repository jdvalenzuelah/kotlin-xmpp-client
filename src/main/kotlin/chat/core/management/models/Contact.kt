package chat.core.management.models

import org.jxmpp.jid.BareJid

data class Contact(val contactName: String, val jid: BareJid)

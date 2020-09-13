package chat.core.management

import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import java.lang.UnsupportedOperationException

class ConnectionConfigBuilder {

    private val xmppConnectionBuilder = XMPPTCPConnectionConfiguration.builder()

    var domain: String
        get() = throw UnsupportedOperationException("Read only property")
        set(value) {
            xmppConnectionBuilder.setXmppDomain(value)
        }

    var host: String
        get() = throw UnsupportedOperationException("Read only property")
        set(value) {
            xmppConnectionBuilder.setHost(value)
        }

    var securityMode: ConnectionConfiguration.SecurityMode
    get() = throw UnsupportedOperationException("Read only property")
    set(value) {
        xmppConnectionBuilder
            .setSecurityMode(value)
    }

    internal fun user(username: String, password: String) {
        xmppConnectionBuilder.setUsernameAndPassword(username, password)
    }

    internal fun build(): XMPPTCPConnectionConfiguration {
        return xmppConnectionBuilder.build()
    }

}

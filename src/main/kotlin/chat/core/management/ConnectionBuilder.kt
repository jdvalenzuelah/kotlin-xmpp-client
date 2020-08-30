package chat.core.management

import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration

class ConnectionBuilder {

    private lateinit var config: XMPPTCPConnectionConfiguration

    fun config(init: ConnectionConfigBuilder.() -> Unit ) {
        val configBuilder = ConnectionConfigBuilder()
        configBuilder.init()
        config = configBuilder.build()
    }

    internal fun build(): XMPPTCPConnection {
        return XMPPTCPConnection(config)
    }

}

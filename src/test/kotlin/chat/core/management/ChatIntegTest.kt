package chat.core.management

import org.jivesoftware.smack.packet.Presence
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.jxmpp.jid.impl.JidCreate
import java.io.File
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ChatIntegTest {

    private val testDomain = "redes2020.xyz"
    private val testHost = "redes2020.xyz"

    private val connection = connection {
        config {
            domain = testDomain
            host = testHost
            securityMode = SecurityMode.disabled
        }
    }

    private val chat = Chat(connection)

    private val username = UUID.randomUUID().toString()
    private val password = UUID.randomUUID().toString()
    private val roomName = UUID.randomUUID().toString()

    @Test
    @Order(1)
    fun `should create a new account`() {
        assertTrue(chat.createAccount(username, password))

    }

    @Test
    @Order(2)
    fun `should log in to an existing account`() {
        assertTrue(chat.login(username, password))
    }

    @Test
    @Order(3)
    fun `should retrieve at least one registered user`() {
        assertTrue(chat.getRegisteredUsers().isNotEmpty())
    }

    @Test
    @Order(4)
    fun `should add contact to roster`() {
        val usernameToAdd = "test12354555" // already on server
        assertTrue(chat.addContact(usernameToAdd, "test contact"))
    }

    @Test
    @Order(5)
    fun `should retrieve at least one contact`() {
        assertTrue(chat.getContacts().isNotEmpty())
    }

    @Test
    @Order(6)
    fun `should detailed contact info`() {
        val jid = JidCreate.bareFrom("test12354555")
        assertNotNull(chat.getContactDetails(jid))
    }

    @Test
    @Order(7)
    fun `should be able to create a new chat`() {
        assertDoesNotThrow {
            chat.chatWith("test12354555@redes2020.xyz") {_,_,_-> /* Empty handler */ }
        }
    }

    @Test
    @Order(8)
    fun `should be able to create a new room`() {
        chat.chatGroup(roomName) { _, _ -> /* Empty handler */ }
    }

    @Test
    @Order(9)
    fun `should be able to join an existing room`() {
        chat.joinChatGroup("testG"){ _, _ -> /* Empty handler */ }
    }

    @Test
    @Order(10)
    fun `should send presence message`() {
        chat.sendPresence(Presence.Type.available, 1, "Disponible", Presence.Mode.available)
    }

    @Test
    @Order(11)
    fun `should send files to user chat`() {
        TODO("Not yet implemented")
    }

    @Test
    @Order(12)
    fun `should send files to group chat`() {
        TODO("Not yet implemented")
    }

    @Test
    @Order(13)
    fun `should delete logged in account`() {
        assertTrue(chat.deleteAccount())
    }

}

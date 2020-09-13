package chat.core.management

abstract class AccountCreationException(msg: String?) : Exception(msg)

class AccountConflict(msg: String?): AccountCreationException(msg)

class ServerException(msg: String?): AccountCreationException(msg)

class NoSearchServiceFoundException(msg: String?) : Exception(msg)

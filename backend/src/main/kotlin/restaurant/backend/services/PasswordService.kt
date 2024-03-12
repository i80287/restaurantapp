package restaurant.backend.services

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PasswordService {
    fun encodePassword(password: String): UUID {
        return UUID.nameUUIDFromBytes(password.toByteArray(Charsets.UTF_8))
    } 
}
package restaurant.backend.domain

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PasswordEncoder {
    final fun encode(password: String): UUID {
        return UUID.nameUUIDFromBytes(password.toByteArray(Charsets.UTF_8))
    }
}

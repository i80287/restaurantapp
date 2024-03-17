package restaurant.backend.domain

import io.jsonwebtoken.Claims
import lombok.AccessLevel
import lombok.NoArgsConstructor
import restaurant.backend.dto.Role
import java.util.*

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class JwtUtils {
    companion object {
        fun generate(claims: Claims): JwtAuthentication {
            return JwtAuthentication(
                username = claims.subject,
                roles = getRoles(claims)
            )
        }

        private fun getRoles(claims: Claims): MutableSet<Role> {
            return Collections.singleton(Role.valueOf(claims.get("role", String::class.java)))
        }
    }
}

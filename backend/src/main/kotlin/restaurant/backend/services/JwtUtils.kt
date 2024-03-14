package restaurant.backend.services

import io.jsonwebtoken.Claims
import lombok.AccessLevel
import lombok.NoArgsConstructor
import restaurant.backend.domain.JwtAuthentication
import restaurant.backend.dto.Role
import java.util.stream.Collectors

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class JwtUtils {
    companion object {
        fun generate(claims: Claims): JwtAuthentication {
            return JwtAuthentication(
                username = claims.subject,
                firstName = claims.get("firstName", String::class.java),
                roles = getRoles(claims)
            )
        }

        private fun getRoles(claims: Claims): MutableSet<Role> {
            val roles: MutableList<String> = claims.get("roles", MutableList::class.java) as MutableList<String>
            return roles.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet())
        }
    }
}

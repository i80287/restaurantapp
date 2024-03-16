package restaurant.backend.domain

import lombok.Getter
import lombok.Setter
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import restaurant.backend.dto.Role

@Getter
@Setter
data class JwtAuthentication(
    private var username: String,
    private val roles: MutableSet<Role>,
    private var authenticated: Boolean = false,
) : Authentication {
    override fun getName(): String = username

    override fun getAuthorities(): MutableSet<out GrantedAuthority> = roles

    override fun getCredentials(): Any? = null

    override fun getDetails(): Any? = null

    override fun getPrincipal(): String = username

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}

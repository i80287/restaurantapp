package restaurant.backend.domain

import lombok.Getter
import lombok.Setter
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import restaurant.backend.dto.Role

@Getter
@Setter
class JwtAuthentication(
    private var authenticated: Boolean = false, 
    private var username: String,
    private var firstName: String,
    private val roles: MutableSet<Role>
) : Authentication {
    override fun getName(): String = firstName

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = roles

    override fun getCredentials(): Any? = null

    override fun getDetails(): Any? = null

    override fun getPrincipal(): Any = username

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}

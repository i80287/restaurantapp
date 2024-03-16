package restaurant.backend.filter

import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import restaurant.backend.domain.JwtAuthentication
import restaurant.backend.domain.JwtUtils
import restaurant.backend.services.JwtProvider
import java.io.IOException


@Component
@RequiredArgsConstructor
class JwtFilter @Autowired constructor(private val jwtProvider: JwtProvider) : GenericFilterBean() {
    companion object {
        private const val AUTHORIZATION: String = "Authorization"
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        val token: String? = getTokenFromRequest(request as HttpServletRequest)
        if (token != null && jwtProvider.validateAccessToken(token)) {
            val claims: Claims = jwtProvider.getAccessClaims(token)
            val jwtInfoToken: JwtAuthentication = JwtUtils.generate(claims)
            jwtInfoToken.isAuthenticated = true
            SecurityContextHolder.getContext().authentication = jwtInfoToken
        }
        filterChain.doFilter(request, response)
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        val bearer: String? = request.getHeader(AUTHORIZATION)
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7)
        }
        return null
    }
}

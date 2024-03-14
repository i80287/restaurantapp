package restaurant.backend.services

import io.jsonwebtoken.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import jakarta.security.auth.message.AuthException
import org.springframework.beans.factory.annotation.Autowired
import restaurant.backend.util.LoggingHelper
import org.springframework.security.core.context.SecurityContextHolder
import restaurant.backend.domain.JwtAuthentication
import restaurant.backend.domain.JwtRequest
import restaurant.backend.domain.JwtResponse
import restaurant.backend.dto.UserDto
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthService(@Autowired private val userService: UserService,
                  @Autowired private val jwtProvider: JwtProvider) :
    LoggingHelper<AuthService>(AuthService::class.java) {
    private final val refreshStorage = ConcurrentHashMap<String, String>()

    fun login(authRequest: JwtRequest): JwtResponse {
        val user: UserDto = retrieveUserOrThrow(authRequest.login)
        val password: String? = user.password
        if (password != null && password == authRequest.password) {
            val accessToken = jwtProvider.generateAccessToken(user)
            val refreshToken = jwtProvider.generateRefreshToken(user)
            refreshStorage[user.login] = refreshToken
            return JwtResponse(accessToken, refreshToken)
        }
        
        throw AuthException("Incorrect password")
    }
    
    fun getAccessToken(refreshToken: String): JwtResponse {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            val claims: Claims = jwtProvider.getRefreshClaims(refreshToken)
            val login: String = claims.subject
            val savedRefreshToken: String? = refreshStorage[login]
            if (savedRefreshToken != null && savedRefreshToken == refreshToken) {
                val user = retrieveUserOrThrow(login)
                return JwtResponse(jwtProvider.generateAccessToken(user), null)
            }
        }

        throw AuthException("Invalid refresh JWT token")
    }

    fun refresh(refreshToken: String): JwtResponse? {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            val claims: Claims = jwtProvider.getRefreshClaims(refreshToken)
            val login: String = claims.subject
            val saveRefreshToken: String? = refreshStorage[login]
            if (saveRefreshToken != null && saveRefreshToken == refreshToken) {
                val user: UserDto = retrieveUserOrThrow(login)
                val accessToken = jwtProvider.generateAccessToken(user)
                val newRefreshToken = jwtProvider.generateRefreshToken(user)
                refreshStorage[user.login] = newRefreshToken
                return JwtResponse(accessToken, newRefreshToken)
            }
        }

        throw AuthException("Invalid refresh JWT token")
    }

    fun getAuthentication(): JwtAuthentication =
        SecurityContextHolder.getContext().authentication as JwtAuthentication
    
    private final fun retrieveUserOrThrow(login: String): UserDto =
        userService.retrieveUserByLogin(login)
                ?: throw AuthException("User $login not found")
}

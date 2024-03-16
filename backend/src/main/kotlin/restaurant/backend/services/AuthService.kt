package restaurant.backend.services

import io.jsonwebtoken.*
import org.springframework.stereotype.Component
import jakarta.security.auth.message.AuthException
import org.springframework.beans.factory.annotation.Autowired
import restaurant.backend.util.LoggingHelper
import org.springframework.security.core.context.SecurityContextHolder
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repositories.UserRepository
import restaurant.backend.domain.JwtAuthentication
import restaurant.backend.domain.JwtRequest
import restaurant.backend.domain.JwtResponse
import restaurant.backend.domain.PasswordEncoder
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthService @Autowired constructor(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
) :
    LoggingHelper<AuthService>(AuthService::class.java) {
    private final val refreshStorage = ConcurrentHashMap<String, String>()

    @Throws(AuthException::class)
    fun login(authRequest: JwtRequest): JwtResponse {
        val user: UserEntity = retrieveUserOrThrow(authRequest.login)
        if (user.passwordHash == passwordEncoder.encode(authRequest.password)) {
            val accessToken = jwtProvider.generateAccessToken(user)
            val refreshToken = jwtProvider.generateRefreshToken(user)
            refreshStorage[user.login] = refreshToken
            return JwtResponse(accessToken, refreshToken, user.userId!!)
        }

        throw AuthException("Incorrect password")
    }

    @Throws(AuthException::class)
    fun getAccessToken(refreshToken: String): JwtResponse {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            val claims: Claims = jwtProvider.getRefreshClaims(refreshToken)
            val login: String = claims.subject
            val savedRefreshToken: String? = refreshStorage[login]
            if (savedRefreshToken != null && savedRefreshToken == refreshToken) {
                return JwtResponse(jwtProvider.generateAccessToken(retrieveUserOrThrow(login)), null, null)
            }
        }

        throw AuthException("Invalid refresh JWT token")
    }

    @Throws(AuthException::class)
    fun refresh(refreshToken: String): JwtResponse? {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            val claims: Claims = jwtProvider.getRefreshClaims(refreshToken)
            val login: String = claims.subject
            val saveRefreshToken: String? = refreshStorage[login]
            if (saveRefreshToken != null && saveRefreshToken == refreshToken) {
                val user: UserEntity = retrieveUserOrThrow(login)
                val accessToken = jwtProvider.generateAccessToken(user)
                val newRefreshToken = jwtProvider.generateRefreshToken(user)
                refreshStorage[user.login] = newRefreshToken
                return JwtResponse(accessToken, newRefreshToken, null)
            }
        }

        throw AuthException("Invalid refresh JWT token")
    }

    fun getAuthentication(): JwtAuthentication =
        SecurityContextHolder.getContext().authentication as JwtAuthentication

    @Throws(AuthException::class)
    private final fun retrieveUserOrThrow(login: String): UserEntity =
        userRepository.findByLogin(login)
            ?: throw AuthException("User $login not found")
}

package restaurant.backend.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.util.LoggingHelper

import javax.crypto.SecretKey
import java.security.Key
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class JwtProvider(
    @Value("\${jwt.secret.access}") jwtAccessSecret: String,
    @Value("\${jwt.secret.refresh}") jwtRefreshSecret: String
) : LoggingHelper<JwtProvider>(JwtProvider::class.java) {

    private val jwtAccessSecret: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret))
    private val jwtRefreshSecret: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret))

    fun generateAccessToken(user: UserEntity): String {
        val now: LocalDateTime = LocalDateTime.now()
        val accessExpirationInstant: Instant = now
            .plusMinutes(10)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val accessExpiration: Date = Date.from(accessExpirationInstant)
        return Jwts.builder()
            .setSubject(user.login)
            .setExpiration(accessExpiration)
            .signWith(jwtAccessSecret)
            .claim("role", user.role)
            .compact()
    }

    fun generateRefreshToken(user: UserEntity): String {
        val now: LocalDateTime = LocalDateTime.now()
        val refreshExpirationInstant: Instant = now
            .plusDays(30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val refreshExpiration: Date = Date.from(refreshExpirationInstant)
        return Jwts.builder()
            .setSubject(user.login)
            .setExpiration(refreshExpiration)
            .signWith(jwtRefreshSecret)
            .compact()
    }

    fun validateAccessToken(accessToken: String): Boolean {
        return validateToken(accessToken, jwtAccessSecret)
    }

    fun validateRefreshToken(refreshToken: String): Boolean {
        return validateToken(refreshToken, jwtRefreshSecret)
    }

    fun validateToken(token: String, jwtSecret: Key): Boolean {
        val (message, exception) =
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (expEx: ExpiredJwtException) {
            "Token expired" to expEx
        } catch (unsEx: UnsupportedJwtException) {
            "Unsupported jwt" to unsEx
        } catch (mjEx: MalformedJwtException) {
            "Malformed jwt" to mjEx
        } catch (sEx: SignatureException) {
            "Invalid signature" to sEx
        } catch (e: Throwable) {
            "Invalid token" to e
        }
        log.info(message, exception)
        return false
    }

    fun getAccessClaims(token: String): Claims {
        return getClaims(token, jwtAccessSecret)
    }

    fun getRefreshClaims(token: String): Claims {
        return getClaims(token, jwtRefreshSecret)
    }

    fun getClaims(token: String, jwtSecret: Key): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .body
    }
}

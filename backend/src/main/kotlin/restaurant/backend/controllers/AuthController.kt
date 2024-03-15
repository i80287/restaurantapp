package restaurant.backend.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import restaurant.backend.domain.JwtRequest
import restaurant.backend.domain.JwtResponse
import restaurant.backend.domain.RefreshJwtRequest
import restaurant.backend.services.AuthService

@RestController
@RequestMapping("/auth")
class AuthController(@Autowired private val authService: AuthService) {
    @PostMapping("/login")
    fun login(@RequestBody authRequest: JwtRequest): ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.login(authRequest))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }

    @PostMapping("/token")
    fun getNewAccessToken(@RequestBody request: RefreshJwtRequest) : ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.getAccessToken(request.refreshToken))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }

    @PostMapping("/refresh")
    fun getNewRefreshToken(@RequestBody request: RefreshJwtRequest): ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.refresh(request.refreshToken))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }
}

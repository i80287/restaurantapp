package restaurant.backend.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import restaurant.backend.domain.JwtRequest
import restaurant.backend.domain.JwtResponse
import restaurant.backend.domain.RefreshJwtRequest
import restaurant.backend.services.UserService
import restaurant.backend.dto.UserDto
import restaurant.backend.services.AuthService


@RestController
@RequestMapping("users")
class UserController @Autowired constructor(
        private val service: UserService,
        private val authService: AuthService
) : ControllerHelper() {

    @PostMapping("/auth/login")
    fun login(@RequestBody authRequest: JwtRequest): ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.login(authRequest))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }

    @PostMapping("/auth/token")
    fun getNewAccessToken(@RequestBody request: RefreshJwtRequest) : ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.getAccessToken(request.refreshToken))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }

    @PostMapping("/auth/refresh")
    fun getNewRefreshToken(@RequestBody request: RefreshJwtRequest): ResponseEntity<JwtResponse> = try {
        ResponseEntity.ok(authService.refresh(request.refreshToken))
    } catch (ex: Throwable) {
        ResponseEntity.badRequest().header(ex.toString()).build()
    }

    @GetMapping("/get/byid/{id}")
    fun getUserById(@PathVariable("id") userId: Int) : ResponseEntity<UserDto> = responseFromNullable(service.retrieveUserById(userId))

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/get/bylogin/{login}")
    fun getUserByLogin(@PathVariable("login") login: String) : ResponseEntity<UserDto> = responseFromNullable(service.retrieveUserByLogin(login))

    @GetMapping("/get/all")
    fun getUsers() : ResponseEntity<List<UserDto>> = ResponseEntity.ok(service.retrieveAll())

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    fun addUser(@RequestBody user: UserDto) : ResponseEntity<String> {
        println("addUser: ${authService.getAuthentication().principal}")
        return responseFromAddedId(service.addUser(user))
    }

    private final fun fromNullableJwtResponse(token: JwtResponse?): ResponseEntity<JwtResponse> =
        when (token){
            null -> ResponseEntity.badRequest().body(JwtResponse(null, null))
            else -> ResponseEntity.ok(token)
        }
}

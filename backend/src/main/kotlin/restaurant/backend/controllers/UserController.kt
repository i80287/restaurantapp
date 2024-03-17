package restaurant.backend.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import restaurant.backend.services.UserService
import restaurant.backend.dto.UserDto

@RestController
@RequestMapping("/users")
class UserController @Autowired constructor(private val service: UserService) : ControllerHelper() {
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get/all")
    fun getUsers(): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(service.retrieveAll())

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get/byid/{id}")
    fun getUserById(@PathVariable("id") userId: Int): ResponseEntity<UserDto> =
        responseFromNullable(service.retrieveUserById(userId))

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get/bylogin/{login}")
    fun getUserByLogin(@PathVariable("login") login: String): ResponseEntity<UserDto> =
        responseFromNullable(service.retrieveUserByLogin(login))

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add")
    fun addUser(@RequestBody user: UserDto): ResponseEntity<String> =
        responseFromBoolStatus(service.addUser(user))

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/delete/byid/{id}")
    fun deleteUserById(@PathVariable("id") userId: Int): ResponseEntity<String> =
        responseFromBoolStatus(service.deleteUserById(userId))

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/delete/bylogin/{login}")
    fun deleteUserByLogin(@PathVariable("login") userLogin: String): ResponseEntity<String> =
        responseFromBoolStatus(service.deleteUserByLogin(userLogin))
}

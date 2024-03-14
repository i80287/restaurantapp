package restaurant.backend.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import restaurant.backend.services.UserService
import restaurant.backend.dto.UserDto

@RestController
@RequestMapping("/users")
class UserController @Autowired constructor(private val service: UserService) : ControllerHelper() {
    @GetMapping("/get/byid/{id}")
    fun getUserById(@PathVariable("id") userId: Int) : ResponseEntity<UserDto> = responseFromNullable(service.retrieveUserById(userId))

    @GetMapping("/get/bylogin/{login}")
    fun getUserByLogin(@PathVariable("login") login: String) : ResponseEntity<UserDto> = responseFromNullable(service.retrieveUserByLogin(login))

    @GetMapping("/get/all")
    fun getUsers() : ResponseEntity<List<UserDto>> = ResponseEntity.ok(service.retrieveAll())

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    fun addUser(@RequestBody user: UserDto) : ResponseEntity<String> {
        return responseFromAddedId(service.addUser(user))
    }
}

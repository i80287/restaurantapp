package restaurant.backend.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.services.UserService
import restaurant.backend.dto.UserDto
import java.util.Optional

@RestController
@RequestMapping("users")
class UserController(val service: UserService) {
    companion object {
        private inline fun <reified T> fromOptional(item: Optional<T>): ResponseEntity<T> {
            return if (item.isPresent)
                ResponseEntity.ok(item.get())
            else
                ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/get/{id}")
    fun getUserById(@PathVariable("id") userId: Int) : ResponseEntity<UserDto> = fromOptional(service.retrieveUserById(userId))
    
    @GetMapping("/get")
    fun getUsers() : ResponseEntity<List<UserDto>> = ResponseEntity.ok(service.retrieveAll())

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    fun addUser(@RequestBody user: UserDto) : ResponseEntity<Boolean>
        = ResponseEntity.ok(service.addUser(user))
}

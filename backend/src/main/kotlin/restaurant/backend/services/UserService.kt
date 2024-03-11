package restaurant.backend.services

import org.springframework.stereotype.Service
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repository.UserRepository
import restaurant.backend.dto.UserDto
import restaurant.backend.dto.DishDto
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto
import java.util.Optional

@Service
class UserService(private val userRepository: UserRepository) {
    
    fun retrieveUserById(userId: Int): Optional<UserDto> {
        return userRepository.findById(userId).map { it: UserEntity -> UserDto(it) }
    }

    fun retrieveAll(): List<UserDto> {
        return userRepository.findAll().map { it: UserEntity -> UserDto(it) }
    }

    fun addUser(user: UserDto): Boolean {
        try {
            userRepository.save(
                UserEntity(
                    login = user.login,
                    passwordHash = user.password.hashCode().toLong(),
                    isAdmin = user.isAdmin)
            )
            return true
        } catch (ex: Throwable) {
            print("[>>>] $ex\n")
            return false
        }
    }
}

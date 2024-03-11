package restaurant.backend.services

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repository.UserRepository
import restaurant.backend.dto.UserDto
import restaurant.backend.dto.DishDto
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(private val userRepository: UserRepository) {
    companion object {
        private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(UserService::class.java)
    }

    fun retrieveUserById(userId: Int): UserDto?  {
        val userEntity: Optional<UserEntity> = userRepository.findById(userId)
        return when {
            userEntity.isPresent -> UserDto(userEntity.get())
            else -> null
        }
    }

    fun retrieveUserByLogin(login: String): UserDto? =
        when (val userEntity: UserEntity? = userRepository.findByLogin(login)) {
            null -> null
            else -> UserDto(userEntity)
        }

    fun retrieveAll(): List<UserDto> {
        return userRepository.findAll().map { it: UserEntity -> UserDto(it) }
    }

    fun addUser(user: UserDto): Int? = try {
        val e = UserEntity(
            login = user.login,
            passwordHash = user.password.hashCode().toLong(),
            isAdmin = user.isAdmin
        )
        // TODO:
        val e1 = userRepository.save(
            e
        )
        logger.info("ptr eql: ${e === e1}")
        e1.userId
    } catch (ex: Throwable) {
        logger.error("[>>>] $ex\n")
        null
    }
}

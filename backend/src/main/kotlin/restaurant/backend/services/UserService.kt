package restaurant.backend.services

import org.springframework.stereotype.Service
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repository.UserRepository
import restaurant.backend.dto.UserDto
import restaurant.backend.util.LoggingHelper
import java.util.Optional

@Service
class UserService(private val userRepository: UserRepository, private val passwordService: PasswordService)
        : LoggingHelper<UserService>(UserService::class.java) {
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

    fun tryAddUser(user: UserDto): Int? = try {
        userRepository.save(UserEntity(
            login = user.login,
            passwordHash = passwordService.encodePassword(user.password!!),
            isAdmin = user.isAdmin
        )).userId
    } catch (ex: Throwable) {
        debugLogOnIncorrectData(user, "UserService::addUser(UserDto)", ex)
        null
    }
}

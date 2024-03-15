package restaurant.backend.services
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repositories.UserRepository
import restaurant.backend.domain.PasswordEncoder
import restaurant.backend.dto.UserDto
import restaurant.backend.util.LoggingHelper
import java.util.Optional

@Service
class UserService(private val userRepository: UserRepository, private val passwordEncoder: PasswordEncoder)
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

    fun addUser(user: UserDto): Int? = try {
        userRepository.save(UserEntity(
            login = user.login,
            passwordHash = passwordEncoder.encode(user.password!!),
            role = user.role
        )).userId
    } catch (ex: Throwable) {
        debugLogOnIncorrectData(user, "UserService::addUser(UserDto)", ex)
        null to "Can't"
    }
}

package restaurant.backend.services
import org.springframework.data.repository.findByIdOrNull
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

    fun addUser(user: UserDto): Pair<Boolean, String> {
        try {
            val login = user.login
            val role = user.role
            val newUserId: Int = userRepository.save(UserEntity(
                login = login,
                passwordHash = passwordEncoder.encode(user.password!!),
                role = role
            )).userId!!
            return true to "Added user with id $newUserId, login $login and role $role"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(user, "UserService::addUser(UserDto)", ex)
            return false to "Can't add user: incorrect data"
        }
    }

    fun deleteUserById(userId: Int): Pair<Boolean, String> {
        try {
            val userEntity: UserEntity = userRepository.findByIdOrNull(userId)
                    ?: return false to "User with id $userId does not exist"
            userRepository.deleteById(userId)
            return true to "Deleted user with id $userId, login ${userEntity.login} and role ${userEntity.role}"
        } catch (ex: Throwable) {
            logError("UserService::deleteUserById(int)", ex)
            return false to "Internal server error: can't delete user with id $userId"
        }
    }

    fun deleteUserByLogin(userLogin: String): Pair<Boolean, String> {
        try {
            val userEntity: UserEntity = userRepository.findByLogin(userLogin)
                    ?: return false to "User with login $userLogin does not exist"
            val userId = userEntity.userId!!
            userRepository.deleteById(userId)
            return true to "Deleted user with id $userId, login $userLogin and role ${userEntity.role}"
        } catch (ex: Throwable) {
            logError("UserService::deleteUserByLogin(String)", ex)
            return false to "Internal server error: can't delete user with login $userLogin"
        }
    }
}

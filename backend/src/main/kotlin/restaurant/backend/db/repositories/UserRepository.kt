package restaurant.backend.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import restaurant.backend.db.entities.UserEntity

@Repository
interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByLogin(login: String): UserEntity?
}

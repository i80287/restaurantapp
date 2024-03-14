package restaurant.backend.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import restaurant.backend.db.entities.DishEntity

@Repository
interface DishRepository : JpaRepository<DishEntity, Int> {
    fun findByName(name: String): DishEntity?
}

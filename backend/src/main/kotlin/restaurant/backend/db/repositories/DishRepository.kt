package restaurant.backend.db.repositories

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import restaurant.backend.db.entities.DishEntity

@Repository
interface DishRepository : JpaRepository<DishEntity, Int> {
    fun findByName(name: String): DishEntity?

    fun findAllByOrderByDishIdAsc(): List<DishEntity>

    @Transactional
    @Modifying
    @Query(
        "UPDATE dishes SET price = :price WHERE dish_id = :dishId",
        nativeQuery = true
    )
    fun updatePriceById(@Param(value = "dishId") dishId: Int, @Param(value = "price") price: Int)

    @Transactional
    @Modifying
    @Query(
        "UPDATE dishes SET quantity = :quantity WHERE dish_id = :dishId",
        nativeQuery = true
    )
    fun updateQuantityById(@Param(value = "dishId") dishId: Int, @Param(value = "quantity") quantity: Int)

    @Transactional
    @Modifying
    @Query(
        "UPDATE dishes SET cook_time = :cookTime WHERE dish_id = :dishId",
        nativeQuery = true
    )
    fun updateCookTimeById(@Param(value = "dishId") dishId: Int, @Param(value = "cookTime") cookTime: Long)

    @Transactional
    @Modifying
    @Query(
        "UPDATE dishes SET \"name\" = :name WHERE dish_id = :dishId",
        nativeQuery = true
    )
    fun updateNameById(@Param(value = "dishId") dishId: Int, @Param(value = "name") name: String)
}

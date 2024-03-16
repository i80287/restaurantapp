package restaurant.backend.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderDishEntity

@Repository
interface OrderDishRepository : JpaRepository<OrderDishEntity, Long> {
    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM order_dishes WHERE order_id = :orderId",
        nativeQuery = true
    )
    fun deleteOrderDishesByOrderId(@Param("orderId") orderId: Int)

    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM order_dishes WHERE order_id = :orderId AND dish_id = :dishId",
        nativeQuery = true
    )
    fun deleteOrderDishByOrderIdAndDishId(@Param("orderId") orderId: Int, @Param("dishId") dishId: Int)
}

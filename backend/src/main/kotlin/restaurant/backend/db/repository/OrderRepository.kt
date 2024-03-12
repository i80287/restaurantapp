package restaurant.backend.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderEntity

@Repository
interface OrderRepository : JpaRepository<OrderEntity, Int>, CustomOrderRepository {
    @Transactional
    @Modifying
    @Query(value = "UPDATE orders SET is_ready = TRUE WHERE order_id = :orderId",
           nativeQuery = true)
    fun setOrderReady(@Param("orderId") orderId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE orders SET started_cooking = TRUE WHERE order_id = :orderId",
           nativeQuery = true)
    fun setOrderStartedCooking(@Param("orderId") orderId: Int)
}

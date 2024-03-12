package restaurant.backend.db.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDto

interface CustomOrderRepository {
    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    fun addOrder(orderDto: OrderDto): OrderEntity

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    fun addDishToOrder(orderAddDishDto: OrderAddDishDto): OrderEntity?
}

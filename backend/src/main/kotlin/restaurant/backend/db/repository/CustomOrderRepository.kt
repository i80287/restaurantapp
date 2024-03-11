package restaurant.backend.db.repository

import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.dto.OrderDto

interface CustomOrderRepository {
    @Transactional
    fun addOrder(orderDto: OrderDto): OrderEntity
}

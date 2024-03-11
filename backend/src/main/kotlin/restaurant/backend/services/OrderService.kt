package restaurant.backend.services

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.repository.OrderDishRepository
import restaurant.backend.db.repository.OrderRepository
import restaurant.backend.dto.DishDto
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto

@Service
class OrderService @Autowired constructor(
    private val orderRepository: OrderRepository,
    private val orderDishRepository: OrderDishRepository,
    private val orderScheduler: OrderScheduler) {
    fun retrieveAllOrders(): List<OrderDto> = orderRepository.findAll().map { order: OrderEntity -> OrderDto(order) }

    fun tryAddOrder(orderDto: OrderDto): Int? = try {
        val order: OrderEntity = orderRepository.addOrder(orderDto)
        orderScheduler.addOrder(order)
        order.orderId
    } catch (ex: Throwable) {
        null
    }

    fun onReadyOrder(orderId: Int) {
        orderRepository.onReadyOrder(orderId)
    }
}

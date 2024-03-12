package restaurant.backend.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.repository.OrderDishRepository
import restaurant.backend.db.repository.OrderRepository
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDto
import java.util.*

@Service
class OrderService @Autowired constructor(
    private val orderRepository: OrderRepository,
    private val orderDishRepository: OrderDishRepository,
    private val orderScheduler: OrderScheduler)
        : ServiceHelper<OrderService>(OrderService::class.java) {

    fun retrieveAllOrders(): List<OrderDto> = orderRepository.findAll().map { order: OrderEntity -> OrderDto(order) }

    fun retrieveOrderById(orderId: Int): OrderDto? {
        val orderEntity: Optional<OrderEntity> = orderRepository.findById(orderId)
        return when {
            orderEntity.isPresent -> OrderDto(orderEntity.get())
            else -> null
        }
    }

    fun tryAddOrder(orderDto: OrderDto): Int? {
        val order: OrderEntity = try {
            orderRepository.addOrder(orderDto)
        } catch (ex: Throwable) {
            // Incorrect data from the user
            debugLogOnIncorrectData(orderDto, "OrderService::tryAddOrder(OrderDto)", ex)
            return null
        }

        try {
            orderScheduler.addOrder(order)
            return order.orderId
        } catch (ex: Throwable) {
            orderRepository.deleteById(order.orderId)
            logger.error("Internal error in the OrderScheduler::addOrder(OrderEntity)\nCould not add order $order\nException: $ex\n")
            return null
        }
    }

    fun onReadyOrder(orderId: Int) {
        orderRepository.onReadyOrder(orderId)
    }

    fun tryAddDishToOrder(orderAddDishDto: OrderAddDishDto): Boolean {
        val updatedOrderEntity: OrderEntity = try {
            orderRepository.addDishToOrder(orderAddDishDto) ?: return false
        } catch (ex: Throwable) {
            debugLogOnIncorrectData(orderAddDishDto, "OrderService::tryAddDishToOrder(OrderAddDishDto)", ex)
            return false
        }

        val dishEntity: DishEntity = updatedOrderEntity
            .dishes
            .find { orderDishEntity: OrderDishEntity -> orderDishEntity.dishId == orderAddDishDto.dishId }!!
            .dish!!
        orderScheduler.addDishesToOrder(dishEntity, orderAddDishDto)
        return true
    }
}

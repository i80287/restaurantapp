package restaurant.backend.dto

import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderEntity

data class OrderDto(
    var orderId: Int = -1,
    val orderOwnerId: Int,
    val startTime: Long? = null,
    val isReady: Boolean = false,
    val orderDishes: HashSet<OrderDishDto> = HashSet()
) {
    constructor(orderEntity: OrderEntity) : this(
        orderId = orderEntity.orderId,
        orderOwnerId = orderEntity.userId,
        startTime = orderEntity.startTime,
        isReady = orderEntity.isReady,
        orderDishes = orderEntity.dishes.map { it: OrderDishEntity -> OrderDishDto(it) }.toHashSet()) {
        assert(orderEntity.user != null)
    }
}

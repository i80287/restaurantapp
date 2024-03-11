package restaurant.backend.dto

import restaurant.backend.db.entities.OrderDishEntity

data class OrderDishDto (val dishId: Int, val orderedCount: Int) {
    constructor(orderDishEntity: OrderDishEntity) : this(orderDishEntity.dishId, orderDishEntity.orderedCount)
}

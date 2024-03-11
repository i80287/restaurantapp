package restaurant.backend.dto

import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderDishEntity

data class DishDto(
    val dishId: Int? = null,
    val name: String,
    val quantity: Int,
    val cookTime: Long
) {
    constructor(dish: DishEntity) : this(dish.dishId, dish.name, dish.quantity, dish.cookTime)

    fun toDish(): DishEntity = DishEntity(dishId, name, quantity, cookTime)
}

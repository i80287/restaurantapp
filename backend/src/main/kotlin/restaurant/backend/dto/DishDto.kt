package restaurant.backend.dto

import restaurant.backend.db.entities.DishEntity

data class DishDto(
    val dishId: Int?,
    val name: String,
    val quantity: Int,
    val cookTime: Long,
    val price: Int
) {
    constructor(dish: DishEntity) : this(dish.dishId, dish.name, dish.quantity, dish.cookTime, dish.price)

    fun toDishWithId(): DishEntity = DishEntity(dishId!!, name, quantity, cookTime, price)

    fun toDishWithoutId(): DishEntity = DishEntity(name =  name, quantity =  quantity, cookTime =  cookTime, price = price)
}

package restaurant.interactor.dto

data class DishDto(
    val dishId: Int?,
    val name: String,
    val quantity: Int,
    val cookTime: Long,
    val price: Int
)

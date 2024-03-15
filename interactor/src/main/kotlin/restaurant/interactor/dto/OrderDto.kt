package restaurant.interactor.dto

data class OrderDto(
    var orderId: Int? = null,
    val orderOwnerId: Int,
    val startTime: Long? = null,
    val isReady: Boolean = false,
    val startedCooking: Boolean = false,
    val orderDishes: HashSet<OrderDishDto> = HashSet()
)

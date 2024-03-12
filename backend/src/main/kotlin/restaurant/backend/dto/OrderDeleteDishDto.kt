package restaurant.backend.dto

data class OrderDeleteDishDto(val orderId: Int, val dishId: Int, val deletingCount: Int)

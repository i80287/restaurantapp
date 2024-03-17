package restaurant.interactor.dto

data class OrderAddDishDto(val orderId: Int, val dishId: Int, val addingCount: Int = 1)

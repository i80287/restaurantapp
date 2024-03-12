package restaurant.backend.dto

data class OrderAddDishDto(val orderId: Int, val dishId: Int, val addingCount: Int = 1) {
    init {
        assert(addingCount > 0)
    }
}

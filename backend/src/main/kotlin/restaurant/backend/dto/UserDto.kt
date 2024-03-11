package restaurant.backend.dto

import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.UserEntity

data class UserDto(
    val login: String,
    val password: String? = null,
    val userId: Int? = null,
    val isAdmin: Boolean = false,
    val orders: MutableList<OrderDto>? = null
) {
    constructor(userEntity: UserEntity) : this(
        login = userEntity.login,
        password = null,
        userId = userEntity.userId,
        isAdmin = userEntity.isAdmin,
        orders = userEntity.orders.map { itOrder: OrderEntity -> OrderDto(itOrder) }.toMutableList())
}

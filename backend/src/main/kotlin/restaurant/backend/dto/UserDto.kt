package restaurant.backend.dto

import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.UserEntity

data class UserDto(
    val userId: Int? = null,
    val login: String,
    val password: String? = null,
    var role: Role,
    val orders: MutableList<OrderDto> = mutableListOf(),
) {
    constructor(userEntity: UserEntity) : this(
        userId = userEntity.userId,
        login = userEntity.login,
        password = null,
        role = userEntity.role,
        orders = userEntity.orders.map { itOrder: OrderEntity -> OrderDto(itOrder) }.toMutableList()
    )

    override fun toString(): String {
        return "UserDto(userId=$userId,login=$login,password=null,role=$role,orders=$orders)"
    }
}

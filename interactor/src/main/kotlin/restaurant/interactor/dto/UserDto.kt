package restaurant.interactor.dto

data class UserDto(
    val userId: Int? = null,
    val login: String,
    val password: String? = null,
    var role: Role,
    val orders: MutableList<OrderDto> = mutableListOf(),
) {
    override fun toString(): String {
        return "UserDto(userId=$userId,login=$login,password=null,role=$role,orders=$orders)"
    }
}

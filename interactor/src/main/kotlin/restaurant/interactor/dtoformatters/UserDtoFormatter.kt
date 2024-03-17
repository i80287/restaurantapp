package restaurant.interactor.dtoformatters

import restaurant.interactor.dto.UserDto
import kotlin.math.max

private const val ID_COLUMN_NAME = "user id"
private const val LOGIN_COLUMN_NAME = "login"
private const val ROLE_COLUMN_NAME = "role"
private const val ORDERS_COUNT_COLUMN_NAME = "orders count"

class UserDtoFormatter(users: Array<UserDto>) : DtoFormatter() {
    constructor(user: UserDto) : this(arrayOf(user))

    val stringTable: String = createTable(users)

    private fun createTable(users: Array<UserDto>): String {
        var maxIdLen = 0
        var maxLoginLen = 0
        var maxRoleLen = 0
        var maxOrdersCountLen = 0
        for (user in users) {
            maxIdLen = max(maxIdLen, user.userId!!.toString().length)
            maxLoginLen = max(maxLoginLen, user.login.length)
            maxRoleLen = max(maxRoleLen, user.role.toString().length)
            maxOrdersCountLen = max(maxOrdersCountLen, user.orders.size.toString().length)
        }

        val idColumnLen = max(maxIdLen, ID_COLUMN_NAME.length)
        val loginColumnLen = max(maxLoginLen, LOGIN_COLUMN_NAME.length)
        val roleColumnLen = max(maxRoleLen, ROLE_COLUMN_NAME.length)
        val ordersCountColumnLen = max(maxOrdersCountLen, ORDERS_COUNT_COLUMN_NAME.length)
        val sepLine: String = makeSepLine(
            idColumnLen,
            loginColumnLen,
            roleColumnLen,
            ordersCountColumnLen
        )
        val sb = StringBuilder(sepLine.length * (3 + users.size * 2))
        sb.append(sepLine)
        appendTableLine(
            sb,
            ID_COLUMN_NAME to idColumnLen,
            LOGIN_COLUMN_NAME to loginColumnLen,
            ROLE_COLUMN_NAME to roleColumnLen,
            ORDERS_COUNT_COLUMN_NAME to ordersCountColumnLen
        )
        sb.append(sepLine)
        for (user in users) {
            appendUserLine(
                sb,
                user,
                idColumnLen,
                loginColumnLen,
                roleColumnLen,
                ordersCountColumnLen
            )
            sb.append(sepLine)
        }
        return sb.toString()
    }

    private fun appendUserLine(
        sb: StringBuilder,
        userDto: UserDto,
        idColumnLen: Int,
        loginColumnLen: Int,
        roleColumnLen: Int,
        ordersCountColumnLen: Int,
    ) {
        appendTableLine(
            sb,
            userDto.userId!!.toString() to idColumnLen,
            userDto.login to loginColumnLen,
            userDto.role.toString() to roleColumnLen,
            userDto.orders.size.toString() to ordersCountColumnLen,
        )
    }
}

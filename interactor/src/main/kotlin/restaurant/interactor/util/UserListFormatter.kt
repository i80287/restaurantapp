package restaurant.interactor.util

import restaurant.interactor.dto.UserDto
import kotlin.math.max

private const val ID_COLUMN_NAME = "user id"
private const val LOGIN_COLUMN_NAME = "login"
private const val ROLE_COLUMN_NAME = "role"
private const val ORDERS_COUNT_COLUMN_NAME = "orders count"

class UserListFormatter(private val users: Array<UserDto>) {
    override fun toString(): String {
        var maxIdLen: Int = 0
        var maxLoginLen: Int = 0
        var maxRoleLen: Int = 0
        var maxOrdersCountLen: Int = 0
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
        val sepLine: String = "+-" +
                              "-".repeat(idColumnLen) +
                              "-+-" +
                              "-".repeat(loginColumnLen) +
                              "-+-" +
                              "-".repeat(roleColumnLen) +
                              "-+-" +
                              "-".repeat(ordersCountColumnLen) +
                              "-+\n"
        val sb = StringBuilder(sepLine.length * (3 + users.size * 2))
        sb.append(sepLine)
        appendHeaderLine(sb,
                         idColumnLen,
                         loginColumnLen,
                         roleColumnLen,
                         ordersCountColumnLen)
        sb.append(sepLine)
        for (user in users) {
            appendUserLine(sb,
                           user,
                           idColumnLen,
                           loginColumnLen,
                           roleColumnLen,
                           ordersCountColumnLen)
            sb.append(sepLine)
        }
        return sb.toString()
    }

    private fun appendHeaderLine(sb: StringBuilder,
                                 idColumnLen: Int,
                                 loginColumnLen: Int,
                                 roleColumnLen: Int,
                                 ordersCountColumnLen: Int) {
        sb.append("| ")
        appendStringWithFill(sb, ID_COLUMN_NAME, idColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, LOGIN_COLUMN_NAME, loginColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, ROLE_COLUMN_NAME, roleColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, ORDERS_COUNT_COLUMN_NAME, ordersCountColumnLen)
        sb.append(" |\n")
    }
    
    private fun appendUserLine(sb: StringBuilder,
                               userDto: UserDto,
                               idColumnLen: Int,
                               loginColumnLen: Int,
                               roleColumnLen: Int,
                               ordersCountColumnLen: Int) {
        sb.append("| ")
        appendStringWithFill(sb, userDto.userId!!.toString(), idColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, userDto.login, loginColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, userDto.role.toString(), roleColumnLen)
        sb.append(" | ")
        appendStringWithFill(sb, userDto.orders.size.toString(), ordersCountColumnLen)
        sb.append(" |\n")
    }
    
    private fun appendStringWithFill(sb: StringBuilder, column: String, fullColumnLen: Int) {
        sb.append(column)
        val freeAppendLen = fullColumnLen - column.length
        if (freeAppendLen > 0) {
            sb.append(CharArray(freeAppendLen) { ' ' })
        }
    }
}
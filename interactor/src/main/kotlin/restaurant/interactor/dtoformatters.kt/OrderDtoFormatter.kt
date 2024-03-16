package restaurant.interactor.dtoformatters.kt

import restaurant.interactor.dto.OrderDto
import kotlin.math.max

private const val ORDER_ID_COLUMN_NAME = "order id"
private const val OWNER_ID_COLUMN_NAME = "owner id"
private const val START_TIME_COLUMN_NAME = "order creation time"
private const val READY_COLUMN_NAME = "is order ready"
private const val STARTED_COOKING_COLUMN_NAME = "is order started cooking"
private const val DISH_ID_COLUMN_NAME = "dish id"
private const val ORDERED_COUNT_COLUMN_NAME = "number of ordered dishes"

class OrderDtoFormatter(orders: Array<OrderDto>) : DtoFormatter() {
    constructor(order: OrderDto) : this(arrayOf(order))

    val stringTable = createTable(orders)

    private fun createTable(orders: Array<OrderDto>): String {
        var maxIdLen: Int = 0
        var maxOwnerIdLen: Int = 0
        var maxStartTimeLen: Int = 0
        val maxIsReadyLen: Int = false.toString().length
        val maxStartedCookingLen: Int = false.toString().length
        var maxDishIdLen: Int = 0
        var maxOrderedCountLen: Int = 0
        var totalDataRows = 0

        for (order in orders) {
            maxIdLen = max(maxIdLen, order.orderId!!.toString().length)
            maxOwnerIdLen = max(maxOwnerIdLen, order.orderOwnerId.toString().length)
            maxStartTimeLen = max(maxStartTimeLen, java.util.Date(order.startTime!!).toString().length)
            for (dish in order.orderDishes) {
                maxDishIdLen = max(maxDishIdLen, dish.dishId.toString().length)
                maxOrderedCountLen = max(maxOrderedCountLen, dish.orderedCount.toString().length)
            }
            totalDataRows += order.orderDishes.size
        }

        val idColumnLen = max(maxIdLen, ORDER_ID_COLUMN_NAME.length)
        val ownerIdColumnLen = max(maxOwnerIdLen, OWNER_ID_COLUMN_NAME.length)
        val startTimeColumnLen = max(maxStartTimeLen, START_TIME_COLUMN_NAME.length)
        val isReadyColumnLen = max(maxIsReadyLen, READY_COLUMN_NAME.length)
        val startedCookingColumnLen = max(maxStartedCookingLen, STARTED_COOKING_COLUMN_NAME.length)
        val dishIdColumnLen = max(maxDishIdLen, DISH_ID_COLUMN_NAME.length)
        val orderedCountColumnLen = max(maxOrderedCountLen, ORDERED_COUNT_COLUMN_NAME.length)
        val sepLine: String = makeSepLine(
            idColumnLen,
            ownerIdColumnLen,
            startTimeColumnLen,
            isReadyColumnLen,
            startedCookingColumnLen,
            dishIdColumnLen,
            orderedCountColumnLen
        )

        val sb = StringBuilder(sepLine.length * (3 + orders.size + totalDataRows))
        sb.append(sepLine)
        appendTableLine(
            sb,
            ORDER_ID_COLUMN_NAME to idColumnLen,
            OWNER_ID_COLUMN_NAME to ownerIdColumnLen,
            START_TIME_COLUMN_NAME to startTimeColumnLen,
            READY_COLUMN_NAME to isReadyColumnLen,
            STARTED_COOKING_COLUMN_NAME to startedCookingColumnLen,
            DISH_ID_COLUMN_NAME to dishIdColumnLen,
            ORDERED_COUNT_COLUMN_NAME to orderedCountColumnLen
        )
        sb.append(sepLine)
        for (order: OrderDto in orders) {
            appendOrderLines(
                sb,
                order,
                idColumnLen,
                ownerIdColumnLen,
                startTimeColumnLen,
                isReadyColumnLen,
                startedCookingColumnLen,
                dishIdColumnLen,
                orderedCountColumnLen
            )
            sb.append(sepLine)
        }
        return sb.toString()
    }

    private fun appendOrderLines(
        sb: StringBuilder,
        order: OrderDto,
        idColumnLen: Int,
        ownerIdColumnLen: Int,
        startTimeColumnLen: Int,
        isReadyColumnLen: Int,
        startedCookingColumnLen: Int,
        dishIdColumnLen: Int,
        orderedCountColumnLen: Int,
    ) {
        val dishes = order.orderDishes.toTypedArray()
        if (dishes.isEmpty()) {
            assert(false)
            return
        }

        appendTableLine(
            sb,
            order.orderId!!.toString() to idColumnLen,
            order.orderOwnerId.toString() to ownerIdColumnLen,
            java.util.Date(order.startTime!!).toString() to startTimeColumnLen,
            order.isReady.toString() to isReadyColumnLen,
            order.startedCooking.toString() to startedCookingColumnLen,
            dishes[0].dishId.toString() to dishIdColumnLen,
            dishes[0].orderedCount.toString() to orderedCountColumnLen
        )

        for (i in 1..<dishes.size) {
            appendTableLine(
                sb,
                "" to idColumnLen,
                "" to ownerIdColumnLen,
                "" to startTimeColumnLen,
                "" to isReadyColumnLen,
                "" to startedCookingColumnLen,
                dishes[i].dishId.toString() to dishIdColumnLen,
                dishes[i].orderedCount.toString() to orderedCountColumnLen
            )
        }
    }
}

package restaurant.interactor.dtoformatters.kt

import restaurant.interactor.dto.DishDto
import kotlin.math.max

private const val ID_COLUMN_NAME = "dish id"
private const val NAME_COLUMN_NAME = "name"
private const val QUANTITY_COLUMN_NAME = "available quantity"
private const val COOK_TIME_COLUMN_NAME = "cook time (in seconds)"
private const val PRICE_COLUMN_NAME = "price"

class DishDtoFormatter(dishes: Array<DishDto>) : DtoFormatter() {
    val stringTable = createTable(dishes)

    private fun createTable(dishes: Array<DishDto>): String {
        var maxIdLen = 0
        var maxNameLen = 0
        var maxQuantityLen = 0
        var maxCookTimeLen = 0
        var maxPriceLen = 0

        for (dish in dishes) {
            maxIdLen = max(maxIdLen, dish.dishId!!.toString().length)
            maxNameLen = max(maxNameLen, dish.name.length)
            maxQuantityLen = max(maxQuantityLen, dish.quantity.toString().length)
            maxCookTimeLen = max(maxCookTimeLen, toSeconds(dish.cookTime).toString().length)
            maxPriceLen = max(maxPriceLen, dish.price.toString().length)
        }

        val idColumnLen = max(maxIdLen, ID_COLUMN_NAME.length)
        val nameColumnLen = max(maxNameLen, NAME_COLUMN_NAME.length)
        val quantityColumnLen = max(maxQuantityLen, QUANTITY_COLUMN_NAME.length)
        val cookTimeColumnLen = max(maxCookTimeLen, COOK_TIME_COLUMN_NAME.length)
        val priceColumnLen = max(maxPriceLen, PRICE_COLUMN_NAME.length)
        val sepLine = makeSepLine(
            idColumnLen,
            nameColumnLen,
            quantityColumnLen,
            cookTimeColumnLen,
            priceColumnLen
        )
        val sb = StringBuilder(sepLine.length * (3 + dishes.size * 2))
        sb.append(sepLine)
        appendTableLine(
            sb,
            ID_COLUMN_NAME to idColumnLen,
            NAME_COLUMN_NAME to nameColumnLen,
            QUANTITY_COLUMN_NAME to quantityColumnLen,
            COOK_TIME_COLUMN_NAME to cookTimeColumnLen,
            PRICE_COLUMN_NAME to priceColumnLen
        )
        sb.append(sepLine)
        for (dish in dishes) {
            appendDishLine(
                sb,
                dish,
                idColumnLen,
                nameColumnLen,
                quantityColumnLen,
                cookTimeColumnLen,
                priceColumnLen
            )
            sb.append(sepLine)
        }
        return sb.toString()
    }

    private fun toSeconds(millis: Long) = millis / 1000

    private fun appendDishLine(
        sb: StringBuilder,
        dish: DishDto,
        idColumnLen: Int,
        nameColumnLen: Int,
        quantityColumnLen: Int,
        cookTimeColumnLen: Int,
        priceColumnLen: Int,
    ) {
        appendTableLine(
            sb,
            dish.dishId!!.toString() to idColumnLen,
            dish.name to nameColumnLen,
            dish.quantity.toString() to quantityColumnLen,
            toSeconds(dish.cookTime).toString() to cookTimeColumnLen,
            dish.price.toString() to priceColumnLen
        )
    }
}

package restaurant.interactor.util

import restaurant.interactor.dto.DishDto
import restaurant.interactor.services.BackendRequestService

class CommandExecutor(private val service: BackendRequestService, private val interactor: UserInteractor) {
    fun addDish() {
        val dishName: String = interactor.requestNonBlankString("Input name of the new dish")
        val minDishQuantity = 1
        val quantity: Int = interactor.requestIntAtLeastOrDefault(
            minDishQuantity,
            "Input number of dishes to be added to the restaurant",
            minDishQuantity)
        val cookTimeInMilliseconds: Long = interactor
            .requestPositiveInt("Input cooking time of the dish in seconds")
            .toLong() * 1000
        val price: Int = interactor.requestPositiveInt("Input price of the dish")
        val response = service.addDish(DishDto(
            dishId = null,
            name = dishName,
            quantity = quantity,
            cookTime = cookTimeInMilliseconds,
            price = price))
        println(response)
    }

    fun exit() {
        println("Exit requested by user")
    }
}
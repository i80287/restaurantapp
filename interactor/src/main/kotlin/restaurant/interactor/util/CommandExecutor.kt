package restaurant.interactor.util

import restaurant.interactor.dto.UserDto
import restaurant.interactor.services.BackendRequestService

private const val NEW_DISH_MIN_QUANTITY = 1

class CommandExecutor(private val service: BackendRequestService, private val interactor: UserInteractor) {
    fun addDish() {
        val response = service.addDish(
            requestNewDishName(),
            requestNewDishQuantity(),
            requestNewDishCookTime(),
            requestNewDishPrice())
        interactor.notify(response)
    }

    fun removeDish() {
        val response = service.removeDish(requestExistingDishName())
        interactor.notify(response)
    }

    fun updateDishPrice() {
        val response = service.updateDishPrice(requestExistingDishName(), requestExistingDishNewPrice())
        interactor.notify(response)
    }

    fun updateDishQuantity() {
        val response = service.updateDishQuantity(requestExistingDishName(), requestExistingDishNewQuantity())
        interactor.notify(response)
    }

    fun updateDishCookTime() {
        val response = service.updateDishCookTime(requestExistingDishName(), requestExistingDishNewCookTime())
        interactor.notify(response)
    }

    fun updateDishName() {
        val response = service.updateDishName(requestExistingDishName(), requestExistingDishNewName())
        interactor.notify(response)
    }

    fun getAllUsers() {
        val userList: Array<UserDto> = service.getAllUsers() ?: return
        interactor.notify(UserListFormatter(userList).toString())
    }

    fun exit() {
        interactor.notifyExit()
    }

    private fun requestNewDishName(): String =
        interactor.requestNonBlankString("Input name of the new dish")

    private fun requestExistingDishName(): String =
        interactor.requestNonBlankString("Input name of the existing dish")

    private fun requestExistingDishNewName(): String =
        interactor.requestNonBlankString("Input new name for the existing dish")

    private fun requestNewDishQuantity(): Int =
        interactor.requestIntAtLeastOrDefault(
            NEW_DISH_MIN_QUANTITY,
            "Input number of dishes to be added to the restaurant",
            default =  NEW_DISH_MIN_QUANTITY
        )

    private fun requestExistingDishNewQuantity(): Int =
        interactor.requestIntAtLeast(0,
            "Input new number of dishes to be in the restaurant")

    private fun requestNewDishCookTime(): Long =
        interactor
            .requestPositiveInt("Input cooking time of the dish in seconds")
            .toLong() * 1000

    private fun requestExistingDishNewCookTime(): Long =
        interactor
            .requestPositiveInt("Input new cooking time of the dish in seconds")
            .toLong() * 1000
    private fun requestNewDishPrice(): Int = interactor.requestPositiveInt("Input price of the dish")

    private fun requestExistingDishNewPrice(): Int = interactor.requestPositiveInt("Input new price of the dish")
}
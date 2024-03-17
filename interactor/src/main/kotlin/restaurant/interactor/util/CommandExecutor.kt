package restaurant.interactor.util

import restaurant.interactor.dto.OrderDishDto
import restaurant.interactor.dtoformatters.UserDtoFormatter
import restaurant.interactor.dto.Role
import restaurant.interactor.dtoformatters.DishDtoFormatter
import restaurant.interactor.dtoformatters.OrderDtoFormatter
import restaurant.interactor.services.BackendRequestService

private const val NEW_DISH_MIN_QUANTITY = 1

class CommandExecutor(private val service: BackendRequestService, private val interactor: UserInteractor) {
    fun addDish() {
        val response = service.addDish(
            requestNewDishName(),
            requestNewDishQuantity(),
            requestNewDishCookTime(),
            requestNewDishPrice()
        )
        interactor.notify(response)
    }

    fun deleteDishByName() {
        val dishName = requestExistingDishName()
        val response = service.removeDish(dishName)
        interactor.notify(response)
    }

    fun updateDishPrice() {
        val dishName = requestExistingDishName()
        val price = requestExistingDishNewPrice()
        val response = service.updateDishPrice(dishName, price)
        interactor.notify(response)
    }

    fun updateDishQuantity() {
        val dishName = requestExistingDishName()
        val quantity = requestExistingDishNewQuantity()
        val response = service.updateDishQuantity(dishName, quantity)
        interactor.notify(response)
    }

    fun updateDishCookTime() {
        val dishName = requestExistingDishName()
        val cookTime = requestExistingDishNewCookTime()
        val response = service.updateDishCookTime(dishName, cookTime)
        interactor.notify(response)
    }

    fun updateDishName() {
        val response = service.updateDishName(requestExistingDishName(), requestExistingDishNewName())
        interactor.notify(response)
    }

    fun getAllUsers() {
        val (userList, errorMessage) = service.getAllUsers()
        interactor.notify(
            when (userList) {
                null -> errorMessage
                else -> UserDtoFormatter(userList).stringTable
            }
        )
    }

    fun getUserById() {
        val userId: Int = requestExistingUserId()
        val (userDto, errorMessage) = service.getUserById(userId)
        interactor.notify(
            when (userDto) {
                null -> errorMessage
                else -> UserDtoFormatter(userDto).stringTable
            }
        )
    }

    fun getUserByLogin() {
        val userLogin: String = requestExistingUserLogin()
        val (userDto, errorMessage) = service.getUserByLogin(userLogin)
        interactor.notify(
            when (userDto) {
                null -> errorMessage
                else -> UserDtoFormatter(userDto).stringTable
            }
        )
    }

    fun addUser() {
        val userLogin: String = requestNewUserLogin()
        val userPassword: String = requestNewUserPassword()
        val userRole: Role = requestNewUserRole()
        val response = service.addUser(userLogin, userPassword, userRole)
        interactor.notify(response)
    }

    fun deleteUserById() {
        val userId: Int = requestExistingUserId()
        val response = service.deleteUserById(userId)
        interactor.notify(response)
    }

    fun deleteUserByLogin() {
        val userLogin: String = requestExistingUserLogin()
        val response = service.deleteUserByLogin(userLogin)
        interactor.notify(response)
    }

    fun getAllOrders() {
        val (ordersList, errorMessage) = service.getAllOrders()
        interactor.notify(
            when (ordersList) {
                null -> errorMessage
                else -> OrderDtoFormatter(ordersList).stringTable
            }
        )
    }

    fun getAllDishes() {
        val (dishesList, errorMessage) = service.getAllDishes()
        interactor.notify(
            when (dishesList) {
                null -> errorMessage
                else -> DishDtoFormatter(dishesList).stringTable
            }
        )
    }

    fun getLoggedInUserOrders() {
        val (ordersList, errorMessage) = service.getLoggedInUserOrders()
        interactor.notify(
            when (ordersList) {
                null -> errorMessage
                else -> OrderDtoFormatter(ordersList).stringTable
            }
        )
    }

    fun forceDeleteOrderById() {
        val orderId = requestExistingOrderId()
        val response = service.forceDeleteOrderById(orderId)
        interactor.notify(response)
    }

    fun addOrder() {
        val userId = service.getThisUserId()
        val dishIdsWithCounts: ArrayList<OrderDishDto> = requestNewOrderDishIdsWithCounts()
        val response = service.addOrder(userId, dishIdsWithCounts)
        interactor.notify(response)
    }

    fun addDishToOrder() {
        val orderId = requestExistingOrderId()
        val dishId = requestExistingDishId()
        val addingCount = requestPositiveDishAddingCount()
        val response = service.addDishToOrder(orderId, dishId, addingCount)
        interactor.notify(response)
    }

    fun deleteDishFromOrder() {
        val orderId = requestExistingOrderId()
        val dishId = requestExistingDishId()
        val deletingCount = requestPositiveDishDeletingCount()
        val response = service.deleteDishFromOrder(orderId, dishId, deletingCount)
        interactor.notify(response)
    }

    fun payForTheOrder() {
        // Should we really do this?
        interactor.requestPositiveInt("Enter card number")

        val orderId = requestExistingOrderId()
        val response = service.payForTheOrder(orderId)
        interactor.notify(response)
    }

    fun deleteLoggedInUserOrder() {
        val orderId = requestExistingOrderId()
        val response = service.deleteLoggedInUserOrderById(orderId)
        interactor.notify(response)
    }

    fun getOrderById() {
        val orderId = requestExistingOrderId()
        val (order, errorMessage) = service.getOrderById(orderId)
        interactor.notify(
            when (order) {
                null -> errorMessage
                else -> OrderDtoFormatter(order).stringTable
            }
        )
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
            default = NEW_DISH_MIN_QUANTITY
        )

    private fun requestExistingDishNewQuantity(): Int =
        interactor.requestIntAtLeast(
            0,
            "Input new number of dishes to be in the restaurant"
        )

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

    private fun requestExistingDishId(): Int = interactor.requestInt("Input id of the existing dish")

    private fun requestExistingUserId(): Int = interactor.requestInt("Input id of the existing user")

    private fun requestExistingUserLogin(): String = interactor.requestString("Input login of the existing user")

    private fun requestNewUserLogin(): String = interactor.requestString("Input login for the new user")

    private fun requestNewUserPassword(): String = interactor.requestString("Input password for the new user")

    private fun requestNewUserRole(): Role {
        val userRoleOption = 1
        val option: Int = interactor
            .requestIntBetween(
                userRoleOption,
                userRoleOption + 1,
                "Select role for the new user: 1 for the USER role and 2 for the ADMIN role"
            )
        return if (option == userRoleOption)
            Role.USER
        else
            Role.ADMIN
    }

    private fun requestExistingOrderId(): Int = interactor.requestInt("Input id of the existing order")

    private fun requestNewOrderDishIdsWithCounts(): ArrayList<OrderDishDto> {
        val dishIdsWithCounts = ArrayList<OrderDishDto>()
        while (true) {
            val dishId: Int =
                interactor.requestIntOrNullForEmpty("Input id of the existing dish or press Enter to stop adding dishes to the order")
                    ?: break
            val count: Int =
                interactor.requestPositiveIntOrNullForEmpty("Input number of the dishes with id $dishId that should be added to the order")
                    ?: break
            dishIdsWithCounts.add(OrderDishDto(dishId = dishId, orderedCount = count))
        }

        return dishIdsWithCounts
    }

    private fun requestPositiveDishAddingCount(): Int =
        interactor.requestPositiveInt("Input amount of dishes to be added to the order")

    private fun requestPositiveDishDeletingCount(): Int =
        interactor.requestPositiveInt("Input amount of dishes to be deleted from the order")
}

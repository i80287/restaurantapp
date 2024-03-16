package restaurant.interactor.util

import restaurant.interactor.services.BackendRequestService
import restaurant.interactor.services.LoginResponseStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UserInteractor(private val service: BackendRequestService) {
    companion object {
        private const val NEXT_COMMAND_PROMPT: String =
            "Write number of the option you want to peek:\n" +
            "   Admin options:\n" +
            "     1. Add new dish to the restaurant menu\n" +
            "     2. Delete dish by name from the restaurant menu\n" +
            "     3. Update price of the dish\n" +
            "     4. Update quantity of the dish in the restaurant menu\n" +
            "     5. Update cook time of the dish\n" +
            "     6. Update name of the dish\n" +
            "     7. Show all users\n" +
            "     8. Get user by id\n" +
            "     9. Get user by login\n" +
            "     10. Add user\n" +
            "     11. Delete user by id\n" +
            "     12. Delete user by login\n" +
            "     13. Show all orders\n" +
            "     14. Get order by id\n" +
            "     15. Delete order by id\n" +
            "   User options:\n" +
            "     16. Show all dishes\n" +
            "     17. Show this user orders\n" +
            "     18. Make new order\n" +
            "     19. Add dish to the order\n" +
            "     20. Remove dish from the order\n" +
            "     21. Pay for the order\n" +
            "     22. Delete this user order\n" +
            "     23. Relogin\n" +
            "     24. Exit\n" +
            "> "
    }

    private enum class UserState {
        SignIn, LoggedIn, ExitRequested
    }

    enum class UserCommand {
        AddDish,
        DeleteDishByName,
        UpdateDishPrice,
        UpdateDishQuantity,
        UpdateDishCookTime,
        UpdateDishName,
        GetAllUsers,
        GetUserById,
        GetUserByLogin,
        AddUser,
        DeleteUserById,
        DeleteUserByLogin,
        GetAllOrders,
        GetOrderById,
        DeleteOrderById,
        GetAllDishes,
        GetLoggedInUserOrders,
        AddOrder,
        AddDishToOrder,
        RemoveDishFromOrder,
        PayForTheOrder,
        DeleteLoggedInUserOrder,
        Relogin,
        Exit
    }

    private var currentState: UserState = UserState.SignIn

    fun loginOrRegisterUser(): Boolean {
        while (true) {
            currentState = when (currentState) {
                UserState.SignIn -> signInUser()
                UserState.LoggedIn -> {
                    notifySignedIn()
                    currentState = UserState.SignIn
                    return true
                }
                UserState.ExitRequested -> return false
            }
        }
    }

    fun nextCommand(): UserCommand {
        while (true) {
            printFlushed(NEXT_COMMAND_PROMPT)
            return when (readIntOrNull()) {
                1 -> UserCommand.AddDish
                2 -> UserCommand.DeleteDishByName
                3 -> UserCommand.UpdateDishPrice
                4 -> UserCommand.UpdateDishQuantity
                5 -> UserCommand.UpdateDishCookTime
                6 -> UserCommand.UpdateDishName
                7 -> UserCommand.GetAllUsers
                8 -> UserCommand.GetUserById
                9 -> UserCommand.GetUserByLogin
                10 -> UserCommand.AddUser
                11 -> UserCommand.DeleteUserById
                12 -> UserCommand.DeleteUserByLogin
                13 -> UserCommand.GetAllOrders
                14 -> UserCommand.GetOrderById
                15 -> UserCommand.DeleteOrderById
                16 -> UserCommand.GetAllDishes
                17 -> UserCommand.GetLoggedInUserOrders
                18 -> UserCommand.AddOrder
                19 -> UserCommand.AddDishToOrder
                20 -> UserCommand.RemoveDishFromOrder
                21 -> UserCommand.PayForTheOrder
                22 -> UserCommand.DeleteLoggedInUserOrder
                23 -> UserCommand.Relogin
                24 -> UserCommand.Exit
                else -> {
                    println("Unknown option, expected number between 1 and 23. Please, try again")
                    continue
                }
            }
        }
    }

    fun notifyExit() = notify("Exit requested by user")

    fun requestPositiveInt(prompt: String): Int = requestIntAtLeast(1, prompt)

    fun requestPositiveIntOrNullForEmpty(prompt: String): Int? = requestIntAtLeastOrNullForEmpty(1, prompt)

    fun requestIntAtLeastOrDefault(minValue: Int, prompt: String, default: Int): Int {
        val msg = "$prompt\n(input integer >= $minValue or press Enter for the default $default)\n> "
        while (true) {
            printFlushed(msg)
            val userInput: String? = readlnOrNull()
            if (userInput.isNullOrEmpty()) {
                return default
            }
            val num: Int? = userInput.toIntOrNull()
            if (num != null && num >= minValue) {
                return num
            }
            println("Incorrect input. Please, try again")
        }
    }

    fun requestIntAtLeastOrNullForEmpty(minValue: Int, prompt: String): Int? {
        val msg = "$prompt\n(input integer >= $minValue or press Enter not to input anything)\n> "
        while (true) {
            printFlushed(msg)
            val userInput: String? = readlnOrNull()
            if (userInput.isNullOrEmpty()) {
                return null
            }
            val num: Int? = userInput.toIntOrNull()
            if (num != null && num >= minValue) {
                return num
            }
            println("Incorrect input. Please, try again")
        }
    }

    fun requestIntAtLeast(minValue: Int, prompt: String): Int {
        val msg = "$prompt\n(input integer >= $minValue)\n> "
        while (true) {
            printFlushed(msg)
            val num: Int? = readIntOrNull()
            if (num != null && num >= minValue) {
                return num
            }

            println("Incorrect input. Please, try again")
        }
    }

    fun requestNonBlankString(prompt: String): String {
        val msg = "$prompt\n> "
        while (true) {
            printFlushed(msg)
            val input: String? = readlnOrNull()
            if (!input.isNullOrBlank()) {
                return input
            }

            println("Empty input. Please, try again")
        }
    }

    fun requestString(prompt: String): String {
        val msg = "$prompt\n> "
        while (true) {
            printFlushed(msg)
            val input: String? = readlnOrNull()
            if (input != null) {
                return input
            }

            println("Empty input. Please, try again")
        }
    }

    fun requestIntBetween(minValue: Int, maxValue: Int, prompt: String): Int {
        val msg = "$prompt\n(input integer between $minValue and $maxValue)\n> "
        while (true) {
            printFlushed(msg)
            val input: Int? = readIntOrNull()
            if (input != null && minValue <= input && input <= maxValue) {
                return input
            }

            println("Incorrect input. Please, try again")
        }
    }

    fun requestInt(prompt: String): Int {
        val msg = "$prompt\n(input integer number)\n> "
        while (true) {
            printFlushed(msg)
            val input: Int? = readIntOrNull()
            if (input != null) {
                return input
            }

            println("Incorrect input. Please, try again")
        }
    }

    fun requestIntOrNullForEmpty(prompt: String): Int? {
        val msg = "$prompt\n(input integer number)\n> "
        while (true) {
            printFlushed(msg)
            val userInput = readlnOrNull()
            if (userInput.isNullOrEmpty()) {
                return null
            }
            val num: Int? = userInput.toIntOrNull()
            if (num != null) {
                return num
            }
            println("Incorrect input. Please, try again")
        }
    }

    private fun requestDate(prompt: String): LocalDateTime? {
        val date: String =
            requestNonBlankString("$prompt\n(in format yyyy-MM-dd HH:mm:ss, for example, 2023-01-31 20:30:45)")
        return try {
            LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (ex: Exception) {
            null
        }
    }

    private fun signInUser(): UserState {
        while (true) {
            printFlushed("Signing in:\nInput login or press Enter to return:\n> ")
            val login: String? = readlnOrNull()
            if (login.isNullOrEmpty()) {
                return UserState.ExitRequested
            }

            return requestCorrectPassword(login)
        }
    }

    private fun requestCorrectPassword(login: String): UserState {
        printFlushed("Input password or press Enter to return:\n> ")
        val password: String? = readlnOrNull()
        if (password.isNullOrEmpty()) {
            return UserState.ExitRequested
        }

        return if (handleLoginServerResponse(service.loginUser(login, password)))
            UserState.LoggedIn
        else
            UserState.SignIn
    }

    private fun readIntOrNull(): Int? = readlnOrNull()?.toIntOrNull()

    private fun handleLoginServerResponse(status: LoginResponseStatus): Boolean {
        val errorMessage = when (status) {
            LoginResponseStatus.OK -> return true
            LoginResponseStatus.FORBIDDEN, LoginResponseStatus.INCORRECT_LOGIN_OR_PASSWORD -> "Incorrect login or password"
            LoginResponseStatus.SERVER_IS_NOT_RUNNING -> "Server is not running or not available"
            LoginResponseStatus.CONNECTION_RESET -> "Connection reset by the server"
            LoginResponseStatus.UNKNOWN -> "Unknown error"
        }
        notify(errorMessage)
        return false
    }

    fun notify(prompt: String) = println(prompt)

    private fun notifySignedIn() = notify("Signed in successfully")

    private fun printFlushed(message: String) {
        print(message)
        System.out.flush()
    }
}

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
            "     2. Remove dish from the restaurant menu\n" +
            "     3. Update price of the dish\n" +
            "     4. Update quantity of the dish in the restaurant menu\n" +
            "     5. Update cook time of the dish\n" +
            "     6. Update name of the dish\n" +
            "     7. Get all users\n" +
            "     8. Get user by id\n" +
            "     9. Get user by login\n" +
            "     10. Add user\n" +
            "     11. Delete user by id\n" +
            "     12. Delete user by login\n" +
            "   User options:\n" +
            "     13. Make new order\n" +
            "     14. Add dish to the order\n" +
            "     15. Remove dish from the order\n" +
            "     16. Get info about the order\n" +
            "     17. Pay for the order\n" +
            "     18. Exit\n" +
            "> "
    }

    private enum class UserState {
        ChooseOption, SignIn, SignUp, LoggedIn, ExitRequested
    }

    enum class UserCommand {
        AddDish,
        RemoveDish,
        UpdateDishPrice,
        UpdateDishQuantity,
        UpdateDishCookTime,
        UpdateDishName,
        GetAllUsers,
        GetUserById,
        GetUserByLogin,
        AddUser,
        DeleteUserId,
        DeleteUserLogin,
        MakeOrder,
        AddDishToOrder,
        RemoveDishFromOrder,
        GetOrderInfo,
        PayForTheOrder,
        Exit
    }

    private var currentState: UserState = UserState.ChooseOption

    fun loginOrRegisterUser(): Boolean {
        while (true) {
            currentState = when (currentState) {
                UserState.ChooseOption -> chooseSignInUp()
                UserState.SignIn -> signInUser()
                UserState.SignUp -> signUpUser()
                UserState.LoggedIn -> {
                    notifySignedIn()
                    return true
                }
                UserState.ExitRequested -> return false
            }
        }
    }

    fun nextCommand(): UserCommand {
        while (true) {
            print(NEXT_COMMAND_PROMPT)
            System.out.flush()
            return when (readIntOrNull()) {
                1 -> UserCommand.AddDish
                2 -> UserCommand.RemoveDish
                3 -> UserCommand.UpdateDishPrice
                4 -> UserCommand.UpdateDishQuantity
                5 -> UserCommand.UpdateDishCookTime
                6 -> UserCommand.UpdateDishName
                7 -> UserCommand.GetAllUsers
                8 -> UserCommand.GetUserById
                9 -> UserCommand.GetUserByLogin
                10 -> UserCommand.AddUser
                11 -> UserCommand.DeleteUserId
                12 -> UserCommand.DeleteUserLogin
                13 -> UserCommand.MakeOrder
                14 -> UserCommand.AddDishToOrder
                15 -> UserCommand.RemoveDishFromOrder
                16 -> UserCommand.GetOrderInfo
                17 -> UserCommand.PayForTheOrder
                18 -> UserCommand.Exit
                else -> {
                    println("Unknown option, expected number from 1 to 18. Please, try again")
                    continue
                }
            }
        }
    }

    fun notifyExit() = notify("Exit requested by user")

    fun requestPositiveInt(prompt: String): Int = requestIntAtLeast(1, prompt)

    fun requestIntAtLeastOrDefault(minValue: Int, prompt: String, default: Int): Int {
        val msg = "$prompt\n(input integer >= $minValue or Enter for the default $default)\n> "
        while (true) {
            print(msg)
            System.out.flush()

            val userInput: String = readlnOrNull() ?: return default
            try {
                val num = userInput.toInt()
                if (num >= minValue) {
                    return num
                }
            } catch (_: Throwable) {
            }
            println("Incorrect input. Please, try again")
        }
    }

    fun requestIntAtLeast(minValue: Int, prompt: String): Int {
        val msg = "$prompt\n(input integer >= $minValue)\n> "
        while (true) {
            print(msg)
            System.out.flush()

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
            print(msg)
            System.out.flush()

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
            print(msg)
            System.out.flush()

            val input: String? = readlnOrNull()
            if (input != null) {
                return input
            }

            println("Empty input. Please, try again")
        }
    }

    fun requestIntBetween(minValue: Int, maxValue: Int, prompt: String): Int {
        val msg = "$prompt (input integer between $minValue and $maxValue)\n> "
        while (true) {
            print(msg)
            System.out.flush()

            val input: Int? = readIntOrNull()
            if (input != null && minValue <= input && input <= maxValue) {
                return input
            }

            println("Incorrect input. Please, try again")
        }
    }

    fun requestDate(prompt: String): LocalDateTime? {
        val date: String =
            requestNonBlankString("$prompt\n(in format yyyy-MM-dd HH:mm:ss, for example, 2023-01-31 20:30:45)")
        return try {
            LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (ex: Exception) {
            null
        }
    }

    private fun chooseSignInUp(): UserState {
        while (true) {
            print(
                "Write number of the option you want to peek:\n" +
                        "     1. Sign in (using existing login)\n" +
                        "     2. Sign up (create new user)\n" +
                        "     3. Exit\n" +
                        "> "
            )
            System.out.flush()

            val userInput: String? = readlnOrNull()
            if (userInput != null && userInput.length == 1) {
                when (userInput[0]) {
                    '1' -> return UserState.SignIn
                    '2' -> return UserState.SignUp
                    '3' -> return UserState.ExitRequested
                }
            }

            println("Number 1, 2 or 3 was expected. Please, try again")
        }
    }

    private fun signInUser(): UserState {
        while (true) {
            print("Enter login or press Enter to return:\n> ")
            System.out.flush()

            val login: String? = readlnOrNull()

            if (login.isNullOrEmpty()) {
                return UserState.ChooseOption
            }

            return requestCorrectPassword(login)
        }
    }

    private fun requestCorrectPassword(login: String): UserState {
        while (true) {
            print("Enter password or press Enter to return:\n> ")
            System.out.flush()

            val password: String? = readlnOrNull()
            if (password.isNullOrEmpty()) {
                return UserState.ChooseOption
            }

            if (handleLoginServerResponse(service.loginUser(login, password))) {
                return UserState.LoggedIn
            }

            println("Please, try again")
        }
    }

    private fun signUpUser(): UserState {
        print("Write name for the new user or press Enter to return:\n> ")
        System.out.flush()

        val username: String? = readlnOrNull()
        if (username.isNullOrEmpty()) {
            return UserState.ChooseOption
        }

//        if (registry.isRegistered(username)) {
//            println("User with this name already exists, redirecting to signing in")
//            return signInUser()
//        }

        print("Write a new password or press Enter to return:\n> ")
        val password: String? = readlnOrNull()
        if (password.isNullOrEmpty()) {
            return UserState.ChooseOption
        }

        var checkPassword: String?
        while (true) {
            print("Write a this password again or press Enter to return:\n> ")
            System.out.flush()

            checkPassword = readlnOrNull()
            if (checkPassword.isNullOrEmpty()) {
                return UserState.ChooseOption
            }

            if (password == checkPassword) {
                break
            }

            print("Passwords do not match. Please, try again")
        }

        // registry.registerUser(username, password)
        return UserState.LoggedIn
    }

    private fun readIntOrNull(): Int? {
        return try {
            readlnOrNull()?.toInt()
        } catch (ex: Throwable) {
            null
        }
    }

    private fun handleLoginServerResponse(status: LoginResponseStatus): Boolean {
        val errorMessage = when (status) {
            LoginResponseStatus.OK -> return true
            LoginResponseStatus.FORBIDDEN, LoginResponseStatus.INCORRECT_LOGIN_OR_PASSWORD -> "Incorrect login or password"
            LoginResponseStatus.SERVER_IS_NOT_RUNNING -> "Server is not running or not available"
            LoginResponseStatus.UNKNOWN -> "Unknown error"
        }
        notify(errorMessage)
        return false
    }

    fun notify(prompt: String) = println(prompt)

    private fun notifySignedIn() = notify("Signed in successfully")
}

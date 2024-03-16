package restaurant.interactor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import restaurant.interactor.services.BackendRequestService
import restaurant.interactor.util.CommandExecutor
import restaurant.interactor.util.UserInteractor

@SpringBootApplication
class InteractorApplication(@Autowired private val service: BackendRequestService) : CommandLineRunner {
    private val interactor = UserInteractor(service)
    private val commandExecutor = CommandExecutor(service, interactor)

    override fun run(vararg args: String?) {
        var reloginRequested = false
        do {
            if (!interactor.loginOrRegisterUser())
                break
            reloginRequested = startExecutingCommands()
        } while (reloginRequested)
        commandExecutor.exit()
    }

    private final fun startExecutingCommands(): Boolean {
        while (true) {
            when (interactor.nextCommand()) {
                UserInteractor.UserCommand.AddDish -> commandExecutor.addDish()
                UserInteractor.UserCommand.DeleteDishByName -> commandExecutor.deleteDishByName()
                UserInteractor.UserCommand.UpdateDishPrice -> commandExecutor.updateDishPrice()
                UserInteractor.UserCommand.UpdateDishQuantity -> commandExecutor.updateDishQuantity()
                UserInteractor.UserCommand.UpdateDishCookTime -> commandExecutor.updateDishCookTime()
                UserInteractor.UserCommand.UpdateDishName -> commandExecutor.updateDishName()
                UserInteractor.UserCommand.GetAllUsers -> commandExecutor.getAllUsers()
                UserInteractor.UserCommand.GetUserById -> commandExecutor.getUserById()
                UserInteractor.UserCommand.GetUserByLogin -> commandExecutor.getUserByLogin()
                UserInteractor.UserCommand.AddUser -> commandExecutor.addUser()
                UserInteractor.UserCommand.DeleteUserById -> commandExecutor.deleteUserById()
                UserInteractor.UserCommand.DeleteUserByLogin -> commandExecutor.deleteUserByLogin()
                UserInteractor.UserCommand.GetAllOrders -> commandExecutor.getAllOrders()
                UserInteractor.UserCommand.GetOrderById -> commandExecutor.getOrderById()
                UserInteractor.UserCommand.DeleteOrderById -> commandExecutor.forceDeleteOrderById()
                UserInteractor.UserCommand.GetAllDishes -> commandExecutor.getAllDishes()
                UserInteractor.UserCommand.GetLoggedInUserOrders -> commandExecutor.getLoggedInUserOrders()
                UserInteractor.UserCommand.AddOrder -> commandExecutor.addOrder()
                UserInteractor.UserCommand.AddDishToOrder -> commandExecutor.addDishToOrder()
                UserInteractor.UserCommand.RemoveDishFromOrder -> commandExecutor.deleteDishFromOrder()
                UserInteractor.UserCommand.PayForTheOrder -> TODO() // commandExecutor.payForTheOrder()
                UserInteractor.UserCommand.DeleteLoggedInUserOrder -> commandExecutor.deleteLoggedInUserOrder()
                UserInteractor.UserCommand.Relogin -> return true
                UserInteractor.UserCommand.Exit -> return false
            }
        }
    }
}

fun main(args: Array<String>) {
	runApplication<InteractorApplication>(*args)
}

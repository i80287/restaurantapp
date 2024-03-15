package restaurant.interactor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import restaurant.interactor.domain.JwtRequest
import restaurant.interactor.services.BackendRequestService
import restaurant.interactor.util.CommandExecutor
import restaurant.interactor.util.UserInteractor

@SpringBootApplication
class InteractorApplication(@Autowired private val service: BackendRequestService) : CommandLineRunner {
    private val interactor = UserInteractor(service)
    private val commandExecutor = CommandExecutor(service, interactor)

    override fun run(vararg args: String?) {
        if (!interactor.loginOrRegisterUser()) {
            commandExecutor.exit()
            return
        }

        while (true) {
            when (interactor.nextCommand()) {
                UserInteractor.UserCommand.AddDish -> commandExecutor.addDish()
                //            UserInteractor.UserCommand.RemoveDish -> commandExecutor.removeDish()
                //            UserInteractor.UserCommand.UpdateDishPrice -> commandExecutor.updateDishPrice()
                //            UserInteractor.UserCommand.UpdateDishQuantity -> commandExecutor.updateDishQuantity()
                //            UserInteractor.UserCommand.UpdateDishCookTime -> commandExecutor.updateDishCookTime()
                //            UserInteractor.UserCommand.MakeOrder -> commandExecutor.makeOrder()
                //            UserInteractor.UserCommand.AddDishToOrder -> commandExecutor.addDishToOrder()
                //            UserInteractor.UserCommand.RemoveDishFromOrder -> commandExecutor.removeDishFromOrder()
                //            UserInteractor.UserCommand.GetOrderInfo -> commandExecutor.getOrderInfo()
                //            UserInteractor.UserCommand.PayForTheOrder -> commandExecutor.payForTheOrder()
                UserInteractor.UserCommand.Exit -> {
                    commandExecutor.exit()
                    return
                }
                else -> { }
            }
        }
    }
}

fun main(args: Array<String>) {
	runApplication<InteractorApplication>(*args)
}

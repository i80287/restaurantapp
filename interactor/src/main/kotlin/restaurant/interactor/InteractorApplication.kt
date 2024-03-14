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
    private val commandExecutor = CommandExecutor(service)

    override fun run(vararg args: String?) {
        if (!interactor.loginOrRegisterUser()) {
            commandExecutor.exitCommand()
            return
        }

        when (interactor.nextCommand()) {
            UserInteractor.UserCommand.Exit -> {
                commandExecutor.exitCommand()
                return
            }
            else -> {

            }
        }
    }
}

fun main(args: Array<String>) {
	runApplication<InteractorApplication>(*args)
}

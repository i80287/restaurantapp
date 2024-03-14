package restaurant.interactor.util

import restaurant.interactor.services.BackendRequestService

class CommandExecutor(private val service: BackendRequestService) {
    fun exitCommand() {
        println("Exit requested by user")
    }
}
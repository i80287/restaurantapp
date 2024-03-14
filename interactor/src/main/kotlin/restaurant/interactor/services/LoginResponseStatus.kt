package restaurant.interactor.services

enum class LoginResponseStatus {
    OK,
    FORBIDDEN,
    SERVER_IS_NOT_RUNNING,
    UNKNOWN
}
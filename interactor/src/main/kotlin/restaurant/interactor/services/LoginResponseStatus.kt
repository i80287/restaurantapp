package restaurant.interactor.services

enum class LoginResponseStatus {
    OK,
    FORBIDDEN,
    INCORRECT_LOGIN_OR_PASSWORD,
    SERVER_IS_NOT_RUNNING,
    UNKNOWN
}
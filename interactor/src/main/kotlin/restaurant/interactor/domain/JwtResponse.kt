package restaurant.interactor.domain

data class JwtResponse(val type: String, val accessToken: String?, val refreshToken: String?)

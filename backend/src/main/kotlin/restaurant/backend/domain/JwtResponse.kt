package restaurant.backend.domain

data class JwtResponse(val accessToken: String?, val refreshToken: String?) {
    val type: String = "Bearer"
}

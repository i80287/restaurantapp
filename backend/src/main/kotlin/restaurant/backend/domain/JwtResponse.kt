package restaurant.backend.domain

data class JwtResponse(val accessToken: String?, val refreshToken: String?, val userId: Int?) {
    val type: String = "Bearer"
}

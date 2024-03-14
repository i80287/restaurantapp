package restaurant.backend.domain

data class JwtResponse(val accessToken: String?, val refreshToken: String?) {
    companion object {
        private const val type: String = "Bearer"
    }
}

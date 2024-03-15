package restaurant.backend.controllers

import org.springframework.http.ResponseEntity

open class ControllerHelper {
    companion object {
        @JvmStatic
        protected fun responseFromBoolStatus(statusAndMessaged: Pair<Boolean, String>): ResponseEntity<String> =
            when (statusAndMessaged.first) {
                true -> ResponseEntity.ok(statusAndMessaged.second)
                false -> ResponseEntity.badRequest().body(statusAndMessaged.second)
            }

        @JvmStatic
        protected inline fun <reified T> responseFromNullable(item: T?): ResponseEntity<T> =
            when (item) {
                null -> ResponseEntity.notFound().build()
                else -> ResponseEntity.ok(item)
            }
    }
}

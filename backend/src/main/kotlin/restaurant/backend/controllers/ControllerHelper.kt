package restaurant.backend.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.Optional

open class ControllerHelper {
    companion object {
        @JvmStatic
        protected inline fun <reified T> responseFromAddedId(addedItemId: T): ResponseEntity<String> =
            when (addedItemId) {
                null -> ResponseEntity<String>("could not add: incorrect data", HttpStatus.BAD_REQUEST)
                else -> ResponseEntity<String>("new id: $addedItemId", HttpStatus.CREATED)
            }

        @JvmStatic
        protected fun responseFromBoolStatus(status: Boolean): ResponseEntity<String> =
            when (status) {
                true -> ResponseEntity.ok("success")
                false -> ResponseEntity.badRequest().build()
            }

        @JvmStatic
        protected fun responseFromBoolStatus(status: Boolean, falseMessage: String): ResponseEntity<String> =
            when (status) {
                true -> ResponseEntity.ok("success")
                false -> ResponseEntity.badRequest().body(falseMessage)
            }

        @JvmStatic
        protected inline fun <reified T> responseFromOptional(item: Optional<T>): ResponseEntity<T> =
            when (item.isPresent) {
                true -> ResponseEntity.ok(item.get())
                false -> ResponseEntity.notFound().build()
            }

        @JvmStatic
        protected inline fun <reified T> responseFromNullable(item: T?): ResponseEntity<T> =
            when (item) {
                null -> ResponseEntity.notFound().build()
                else -> ResponseEntity.ok(item)
            }
    }
}

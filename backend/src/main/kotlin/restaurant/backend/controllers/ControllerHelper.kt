package restaurant.backend.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

open class ControllerHelper {
    inline fun <reified T> responseFromAddedId(addedItemId: T): ResponseEntity<String> {
        return when (addedItemId) {
            null -> ResponseEntity<String>("incorrect request body", HttpStatus.BAD_REQUEST)
            else -> ResponseEntity<String>("new id: $addedItemId", HttpStatus.OK)
        }
    }
}

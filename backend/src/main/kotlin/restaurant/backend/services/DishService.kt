package restaurant.backend.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.repository.DishRepository
import restaurant.backend.dto.DishDto
import java.lang.Exception
import java.util.*

@Service
class DishService(private val dishRepository: DishRepository) {
    fun tryAddDish(dish: DishDto): Int? = try {
        dishRepository.save(
            DishEntity(
                name = dish.name,
                quantity = dish.quantity,
                cookTime = dish.cookTime
            )
        ).dishId
    } catch (ex: Throwable) {
        null
    }

    fun retrieveAllDishes(): List<DishDto> {
        return dishRepository.findAll().map { dish: DishEntity -> DishDto(dish) }
    }

    fun retrieveDishById(dishId: Int): DishDto? {
        val dishEntity: Optional<DishEntity> =  dishRepository.findById(dishId)
        return when {
            dishEntity.isPresent -> DishDto(dishEntity.get())
            else -> null
        }
    }

    fun retrieveDishByString(dishName: String): DishDto? =
        when (val dishEntity: DishEntity? = dishRepository.findByName(dishName)) {
            null -> null
            else -> DishDto(dishEntity)
        }
}

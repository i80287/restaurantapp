package restaurant.backend.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.repositories.DishRepository
import restaurant.backend.dto.DishDto
import restaurant.backend.util.LoggingHelper
import java.util.*

@Service
class DishService(private val dishRepository: DishRepository)
        : LoggingHelper<DishService>(DishService::class.java) {
    fun tryAddDish(dishDto: DishDto): Int? = try {
        dishRepository.save(dishDto.toDishWithoutId()).dishId
    } catch (ex: Throwable) {
        debugLogOnIncorrectData(dishDto, "DishService::tryAddDish(DishDto)", ex)
        null
    }

    fun retrieveAllDishes(): List<DishDto> {
        return dishRepository.findAll().map { dish: DishEntity -> DishDto(dish) }
    }

    fun retrieveDishById(dishId: Int): DishDto? {
        val dishEntity: Optional<DishEntity> = dishRepository.findById(dishId)
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

    fun deleteDish(dishId: Int): DishDto? {
        val dishDto = DishDto(dishRepository.findByIdOrNull(dishId) ?: return null)
        dishRepository.deleteById(dishId)
        return dishDto
    }
}

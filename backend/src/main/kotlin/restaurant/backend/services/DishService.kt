package restaurant.backend.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.repository.DishRepository
import restaurant.backend.dto.DishDto
import java.lang.Exception
import java.util.*

@Service
class DishService(private val dishRepository: DishRepository)
        : ServiceHelper<DishService>(DishService::class.java) {
    fun tryAddDish(dishDto: DishDto): Int? = try {
        dishRepository.save(
            DishEntity(
                name = dishDto.name,
                quantity = dishDto.quantity,
                cookTime = dishDto.cookTime
            )
        ).dishId
    } catch (ex: Throwable) {
        debugLogOnIncorrectData(dishDto, "DishService::tryAddDish(DishDto)", ex)
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

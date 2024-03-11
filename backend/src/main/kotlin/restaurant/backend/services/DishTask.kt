package restaurant.backend.services

import kotlinx.coroutines.*

class DishTask(val dishId: Int, val cookTime: Long, val orderTask: OrderTask, private val priority: Int) : Comparable<DishTask> {
    @Volatile var isCooked: Boolean = false
    @Volatile var isCooking: Boolean = false

    suspend inline fun startCooking(cookingCoroutineScope: CoroutineScope) {
        assert(!isCooked)
        assert(!isCooking)
        isCooking = true
        orderTask.onDishStartedCooking(this, cookingCoroutineScope.coroutineContext.job)
        delay(cookTime)
        isCooking = false
        isCooked = true
        val thisDishTask: DishTask = this
        orderTask.onDishReady(thisDishTask)
    }

    override fun compareTo(other: DishTask): Int {
        return priority.compareTo(other.priority)
    }
}

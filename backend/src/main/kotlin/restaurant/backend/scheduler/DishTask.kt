package restaurant.backend.scheduler

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

const val DEFAULT_TASK_PRIORITY = 10

class DishTask private constructor (val dishTaskOrderUniqueId: Int,
               val dishId: Int,
               private val cookTime: Long,
               val orderTask: OrderTask,
               @Volatile private var priority: Int)
        : Comparable<DishTask> {
    companion object {
        fun createTask(dishTaskOrderUniqueId: Int,
                       dishId: Int,
                       cookTime: Long,
                       orderTask: OrderTask): DishTask {
            return DishTask(dishTaskOrderUniqueId, dishId, cookTime, orderTask, DEFAULT_TASK_PRIORITY)
        }
    }

    val isCooked = AtomicBoolean()
    val isCooking = AtomicBoolean()
    private val cancelled = AtomicBoolean()

    fun cancelDishTask() {
        cancelled.set(true)
    }

    fun isDishTaskCancelled(): Boolean = cancelled.get()

    suspend fun startCooking(dishCookingJob: Job) {
        assert(!isCooked.get())
        assert(!isCooking.get())
        isCooking.set(true)
        orderTask.onDishStartedCooking(this, dishCookingJob)
        if (!cancelled.get()) {
            delay(cookTime)
        }
        isCooking.set(false)
        isCooked.set(true)
        orderTask.onDishReady(this)
    }

    fun increasePriority() {
        // In the priority queue re ordered according to their natural ordering
        --priority
    }

    override fun compareTo(other: DishTask): Int {
        return priority.compareTo(other.priority)
    }

    override fun hashCode(): Int = dishTaskOrderUniqueId xor orderTask.orderId xor dishId

    override fun equals(other: Any?): Boolean =
        other is DishTask &&
        dishId == other.dishId &&
        orderTask.orderId == other.orderTask.orderId &&
        dishTaskOrderUniqueId == other.dishTaskOrderUniqueId

    override fun toString(): String {
        return "DishTask(dishId=$dishId,orderId=${orderTask.orderId},dishTaskOrderUniqueId=${dishTaskOrderUniqueId},cookTime=$cookTime,priority=$priority,cancelled=${cancelled.get()})"
    }
}

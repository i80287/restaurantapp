package restaurant.backend.schedulers

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restaurant.backend.util.LoggingHelper
import java.util.PriorityQueue
import java.util.concurrent.PriorityBlockingQueue

class DishTaskScheduler : LoggingHelper<DishTaskScheduler>(DishTaskScheduler::class.java) {
    companion object {
        private const val PRIORITY_UPDATE_CYCLE_TIMEOUT_MILLISECONDS = 2_000L
        private const val PRIORITY_UPDATE_TIMEOUT_MILLISECONDS = 20_000L
        private const val MAX_COOKING_DISHES_PER_ONE_TIME: Int = 4
        private const val TASK_SCHEDULER_DEBUG_PRINTING: Boolean = false
    }

    private val priorityUpdateStateLock: Mutex = Mutex()
    private val dishTasksQueue = PriorityBlockingQueue<DishTask>()
    private val priorityUpdateTasksQueue = PriorityQueue<PriorityUpdateEntry>()
    private val priorityUpdateThread: Thread = createPriorityUpdateThread()
    private val cookingThreads = ArrayList<Thread>(MAX_COOKING_DISHES_PER_ONE_TIME)

    init {
        repeat(MAX_COOKING_DISHES_PER_ONE_TIME) {
            cookingThreads.add(createDishCookingThread("cookingThread-$it"))
        }
    }

    suspend fun addAll(dishTasks: ArrayList<DishTask>) {
        for (dishTask: DishTask in dishTasks) {
            dishTasksQueue.offer(dishTask)
        }
        val currentTime: Long = System.currentTimeMillis()
        priorityUpdateStateLock.withLock {
            for (dishTask: DishTask in dishTasks) {
                priorityUpdateTasksQueue.offer(PriorityUpdateEntry(currentTime, dishTask))
            }
        }
    }

    suspend fun removeAll(dishTasks: ArrayList<DishTask>) {
        val tasksHashSet: HashSet<DishTask> = dishTasks.toHashSet()
        priorityUpdateStateLock.withLock {
            priorityUpdateTasksQueue.removeIf { entry: PriorityUpdateEntry -> tasksHashSet.contains(entry.dishTask) }
        }
        dishTasks.removeAll(tasksHashSet)
    }

    private fun createPriorityUpdateThread(): Thread {
        val thread = object : Thread() {
            override fun run() = runBlocking {
                while (isActive) {
                    try {
                        delay(PRIORITY_UPDATE_CYCLE_TIMEOUT_MILLISECONDS)
                        priorityUpdateStateLock.withLock {
                            updateQueueTasksTimeout()
                        }
                    } catch (ex: InterruptedException) {
                        break
                    } catch (ex: Throwable) {
                        logError("createPriorityUpdateThread()::Thread()::run()", ex)
                    }
                }
            }

            private fun updateQueueTasksTimeout() {
                val currentTime: Long = System.currentTimeMillis()
                var limit: Int = priorityUpdateTasksQueue.size
                do {
                    val earliestTask: PriorityUpdateEntry = priorityUpdateTasksQueue.peek() ?: break
                    val dishTask: DishTask = earliestTask.dishTask
                    if (dishTask.isCooking.get() || dishTask.isCooked.get() || dishTask.isDishTaskCancelled()) {
                        priorityUpdateTasksQueue.poll()
                        continue
                    }

                    val timePassed: Long = currentTime - earliestTask.lastTimeUpdatedMillis
                    if (TASK_SCHEDULER_DEBUG_PRINTING) {
                        printTimeUpdateMessage(dishTask, timePassed)
                    }
                    val shouldUpdateEarliest: Boolean = timePassed >= PRIORITY_UPDATE_TIMEOUT_MILLISECONDS
                    if (!shouldUpdateEarliest) {
                        break
                    }
                    priorityUpdateTasksQueue.poll()
                    earliestTask.lastTimeUpdatedMillis = currentTime
                    priorityUpdateTasksQueue.offer(earliestTask)
                    updateDishTaskPriority(dishTask)
                    if (TASK_SCHEDULER_DEBUG_PRINTING) {
                        printPriorityUpdateMessage(dishTask)
                    }
                } while (--limit > 0)
            }

            private fun printTimeUpdateMessage(dishTask: DishTask, timePassed: Long) {
                println(
                    """
                    PRIORITY UPDATE SCHEDULER MESSAGE: peeked task
                        Task id: ${dishTask.dishId}
                        Order id: ${dishTask.orderTask.orderId}
                        Unique id: ${dishTask.dishTaskOrderUniqueId}
                        Priority: ${dishTask.priority}
                        Passed time since last update: $timePassed ms
                    """.trimIndent()
                )
            }

            private fun printPriorityUpdateMessage(dishTask: DishTask) {
                println(
                    """
                    PRIORITY UPDATE SCHEDULER MESSAGE: updating priority
                        Task id: ${dishTask.dishId}
                        Order id: ${dishTask.orderTask.orderId}
                        Unique id: ${dishTask.dishTaskOrderUniqueId}
                        Priority: ${dishTask.priority}
                        """.trimIndent()
                )
            }
        }

        thread.start()
        return thread
    }

    private fun createDishCookingThread(threadName: String): Thread {
        val thread = object : Thread() {
            override fun run() = runBlocking {
                while (isActive) {
                    try {
                        val dishTask: DishTask = dishTasksQueue.take()
                        if (dishTask.isDishTaskCancelled()) {
                            continue
                        }
                        if (TASK_SCHEDULER_DEBUG_PRINTING) {
                            printDishStartedCooking(dishTask)
                        }
                        dishTask.startCooking(this.coroutineContext.job)
                        if (TASK_SCHEDULER_DEBUG_PRINTING) {
                            printDishEndedCooking(dishTask)
                        }
                    } catch (ex: Throwable) {
                        logError("Error cooking new dish", "OrderScheduler::createDishCookingThread()", ex)
                    }
                }
                logInfo("$threadName exited", "OrderScheduler::createDishCookingThread()")
            }

            private fun printDishStartedCooking(dishTask: DishTask) {
                println(
                    """
                    DISH COOKING SCHEDULER MESSAGE: started cooking dish
                        Task id: ${dishTask.dishId}
                        Order id: ${dishTask.orderTask.orderId}
                        Unique id: ${dishTask.dishTaskOrderUniqueId}
                        Priority: ${dishTask.priority}
                        """.trimIndent()
                )
            }

            private fun printDishEndedCooking(dishTask: DishTask) {
                println(
                    """
                    DISH COOKING SCHEDULER MESSAGE: ended cooking dish
                        Task id: ${dishTask.dishId}
                        Order id: ${dishTask.orderTask.orderId}
                        Unique id: ${dishTask.dishTaskOrderUniqueId}
                        Priority: ${dishTask.priority}
                        """.trimIndent()
                )
            }
        }
        thread.start()
        return thread
    }

    private fun updateDishTaskPriority(dishTask: DishTask) {
        val foundAndRemoved: Boolean = dishTasksQueue.remove(dishTask)
        if (foundAndRemoved) {
            dishTask.increasePriority()
            dishTasksQueue.offer(dishTask)
        } else {
            logInfo(
                "Priority update: task not found in the dishTasksQueue",
                "DishTaskScheduler::updateDishTaskPriority(DishTask)"
            )
        }
    }

    fun destroy() {
        try {
            priorityUpdateThread.interrupt()
        } catch (ex: Throwable) {
            logDebug(
                { "Could not stop priorityUpdateThread ${priorityUpdateThread.name}" },
                "DishTaskScheduler::destroy()",
                ex
            )
        }
        priorityUpdateTasksQueue.clear()
        dishTasksQueue.clear()
        for (cookingThread: Thread in cookingThreads) {
            if (cookingThread.isAlive) {
                try {
                    cookingThread.interrupt()
                } catch (ex: Throwable) {
                    logDebug(
                        { "Could not stop cookingThread ${cookingThread.name}" },
                        "DishTaskScheduler::destroy()",
                        ex
                    )
                }
            }
        }
    }
}

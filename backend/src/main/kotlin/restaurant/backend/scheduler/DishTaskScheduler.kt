package restaurant.backend.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restaurant.backend.util.LoggingHelper
import java.util.PriorityQueue
import java.util.concurrent.PriorityBlockingQueue
import kotlin.concurrent.thread

const val PRIORITY_UPDATE_CYCLE_TIMEOUT_MILLISECONDS = 2_000L
const val PRIORITY_UPDATE_TIMEOUT_MILLISECONDS = 20_000L
const val MAX_COOKING_DISHES_PER_ONE_TIME: Int = 4

class DishTaskScheduler : LoggingHelper<DishTaskScheduler>(DishTaskScheduler::class.java) {
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

    suspend fun offer(dishTask: DishTask) {
        dishTasksQueue.offer(dishTask)
        val currentTime: Long = System.currentTimeMillis()
        priorityUpdateStateLock.withLock {
            priorityUpdateTasksQueue.offer(PriorityUpdateEntry(currentTime, dishTask))
        }
    }

    suspend fun removeAll(dishTasks: ArrayList<DishTask>) {
        val tasksHashSet: HashSet<DishTask> = dishTasks.toHashSet()
        priorityUpdateStateLock.withLock {
            priorityUpdateTasksQueue.removeIf { entry: PriorityUpdateEntry -> tasksHashSet.contains(entry.dishTask) }
        }
        dishTasks.removeAll(tasksHashSet)
    }
    
    private fun createPriorityUpdateThread(): Thread = thread {
        runBlocking {
            while (isActive) {
                try {
                    delay(PRIORITY_UPDATE_CYCLE_TIMEOUT_MILLISECONDS)
                    priorityUpdateStateLock.withLock {
                        val currentTime: Long = System.currentTimeMillis()
                        var limit: Int = priorityUpdateTasksQueue.size
                        do {
                            val earliestTask: PriorityUpdateEntry = priorityUpdateTasksQueue.peek() ?: break
                            val dishTask: DishTask = earliestTask.dishTask
                            if (dishTask.isCooked.get() || dishTask.isDishTaskCancelled()) {
                                priorityUpdateTasksQueue.poll()
                                continue
                            }
                            
                            val timePassed: Long = currentTime - earliestTask.lastTimeUpdatedMillis
                            logger.info("Task $dishTask time passed=${timePassed/1000} sec")
                            val shouldUpdateEarliest: Boolean = timePassed >= PRIORITY_UPDATE_TIMEOUT_MILLISECONDS
                            if (!shouldUpdateEarliest) {
                                break
                            }
                            priorityUpdateTasksQueue.poll()
                            earliestTask.lastTimeUpdatedMillis = currentTime
                            priorityUpdateTasksQueue.offer(earliestTask)
                            updateDishTaskPriority(dishTask)
                            logger.info("Updated priority for the $dishTask")
                        } while (--limit > 0)
                    }
                } catch (ex: InterruptedException) {
                    break
                } catch (ex: Throwable) {
                    errorLog("createPriorityUpdateThread()::Thread()::run()", ex)
                }
            }
        }
    }
    
    private fun createDishCookingThread(threadName: String): Thread = thread(name = threadName) {
        runBlocking {
            while (isActive) {
                try {
                    val dishTask: DishTask = dishTasksQueue.take()
                    val cancelled: Boolean = dishTask.isDishTaskCancelled()
                    infoLog(
                        "$threadName started $dishTask",
                        "OrderScheduler::createDishCookingThread()"
                    )
                    if (!cancelled) {
                        dishTask.startCooking(this.coroutineContext.job)
                    }
                    infoLog(
                        "$threadName ended $dishTask",
                        "OrderScheduler::createDishCookingThread()"
                    )
                } catch (ex: Throwable) {
                    errorLog("Error cooking new dish", "OrderScheduler::createDishCookingThread()", ex)
                }
            }
            infoLog("$threadName exited", "OrderScheduler::createDishCookingThread()")
        }
    }
    
    private fun updateDishTaskPriority(dishTask: DishTask) {
        val foundAndRemoved: Boolean = dishTasksQueue.remove(dishTask)
        if (foundAndRemoved) {
            dishTask.increasePriority()
            dishTasksQueue.offer(dishTask)
        } else {
            infoLog("Priority update: task not found in the dishTasksQueue", "DishTaskScheduler::updateDishTaskPriority(DishTask)")
        }
    }

    fun destroy() {
        try {
            priorityUpdateThread.interrupt()
        } catch (ex: Throwable) {
            debugLog(
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
                    debugLog(
                        { "Could not stop cookingThread ${cookingThread.name}" },
                        "DishTaskScheduler::destroy()",
                        ex
                    )
                }
            }
        }
    }
}

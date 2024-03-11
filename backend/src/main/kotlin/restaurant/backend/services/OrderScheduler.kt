package restaurant.backend.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.springframework.stereotype.Service
import restaurant.backend.db.repository.OrderRepository
import java.util.concurrent.Executors
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.concurrent.thread

const val MAX_COOKING_DISHES_PER_ONE_TIME: Int = 4

@Service
class OrderScheduler @Autowired constructor(private val orderRepository: OrderRepository, @Lazy private val orderService: OrderService) {
    companion object {
        private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(DishService::class.java)
    }

    private val cookingThreads = ArrayList<Thread>(MAX_COOKING_DISHES_PER_ONE_TIME)
    private val dishTasks = PriorityBlockingQueue<DishTask>()
    private val cookedOrdersChannel: Channel<OrderTask> = Channel()
    private val cookedOrderHandleThread: Thread = thread {
        runBlocking {
            while (this.isActive) {
                try {
                    val cookedOrderTask: OrderTask = cookedOrdersChannel.receive()
                    assert(cookedOrderTask.ready())
                    val orderId: Int = cookedOrderTask.orderId
                    if (cookingOrders.remove(orderId)!!.totalDishes() != cookedOrderTask.totalDishes()) {
                        logger.error("Incorrect order task in cookedOrderHandleThread: $orderId")
                    }

                    orderService.onReadyOrder(orderId)
                } catch (ex: Throwable) {
                    logger.error(">-----------------------------------<\n" +
                          "[>>>] COOKED_ORDER_EX_HANDLING" +
                          "[>>>] ex: $ex\n" +
                          "[>>>] message: ${ex.message}\n" +
                          "[>>>] cause: ${ex.cause}\n" +
                          ">-----------------------------------<\n")
                }
            }
        }
    }

    private val cookingOrders = ConcurrentHashMap<Int, OrderTask>()

    init {
        repeat(MAX_COOKING_DISHES_PER_ONE_TIME) {
            cookingThreads.add(thread {
                runBlocking {
                    while (this.isActive) {
                        try {
                            val dishTask: DishTask = dishTasks.take()
                            logger.info("[>>>] TOOK DISH TASK ${dishTask.dishId}\n")
                            dishTask.startCooking(this)
                        } catch (ex: Throwable) {
                            logger.error(">-----------------------------------<\n" +
                                  "[>>>] NEW_DISH_EX_HANDLING" +
                                  "[>>>] ex: $ex\n" +
                                  "[>>>] message: ${ex.message}\n" +
                                  "[>>>] cause: ${ex.cause}\n" +
                                  ">-----------------------------------<\n")
                        }
                    }
                }
            })
        }
    }

    final fun addOrder(order: OrderEntity) {
        val orderTask = OrderTask(order = order, scheduler = this)

        logger.info("[>>>] ADDING ${orderTask.dishesTasks.size} TASKS\n")
        for (dishTask: DishTask in orderTask.dishesTasks) {
            logger.info("[>>>] ADDING DISH TASK ${dishTask.dishId}\n")
            dishTasks.offer(dishTask)
        }
        cookingOrders[order.orderId!!] = orderTask
    }

    final suspend fun cancelDishCooking(dish: OrderDishEntity) {
        cancelDishCooking(dish.orderId, dish.dishId)
    }

    final suspend fun cancelDishCooking(orderId: Int, dishId: Int) {
        cookingOrders[orderId]!!.cancelDishCooking(dishId)
    }

    final suspend fun onOrderReady(orderTask: OrderTask) {
        cookedOrdersChannel.send(orderTask)
    }
}

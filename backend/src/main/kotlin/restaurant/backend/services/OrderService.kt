package restaurant.backend.services

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.UserEntity
import restaurant.backend.db.repositories.DishRepository
import restaurant.backend.db.repositories.OrderRepository
import restaurant.backend.db.repositories.UserRepository
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.schedulers.OrderScheduler
import restaurant.backend.util.LoggingHelper
import restaurant.backend.util.PaidOrderStatus
import java.util.*

@Service
class OrderService @Autowired constructor(
    private val orderRepository: OrderRepository,
    private val dishRepository: DishRepository,
    private val userRepository: UserRepository,
) : LoggingHelper<OrderService>(OrderService::class.java), DisposableBean {

    private final val orderScheduler = OrderScheduler(this)

    @PostConstruct
    fun initSchedulerOnAppInitialization() {
        orderScheduler.scheduleOrdersOnAppInitialization(orderRepository.findAll())
    }

    final fun retrieveAllOrders(): List<OrderDto> =
        orderRepository.findAllByOrderByOrderIdAscStartedCookingAsc().map { order: OrderEntity -> OrderDto(order) }

    final fun retrieveAllUserOrders(userId: Int, issuerLogin: String): List<OrderDto>? {
        try {
            if (verifyUserHasThisId(issuerLogin, userId))
                return orderRepository.findAllByUserIdOrderByOrderIdAsc(userId).map { order: OrderEntity -> OrderDto(order) }
        } catch (ex: Throwable) {
            logError("OrderService::retrieveAllUserOrders(int)", ex)
        }
        return null
    }

    final fun retrieveOrderById(orderId: Int): OrderDto? {
        val orderEntity: Optional<OrderEntity> = orderRepository.findById(orderId)
        return when {
            orderEntity.isPresent -> OrderDto(orderEntity.get())
            else -> null
        }
    }

    final suspend fun addOrder(orderDto: OrderDto): Pair<Boolean, String> {
        if (orderDto.orderDishes.size <= 0) {
            return false to "Can't add order without dishes"
        }
        for (orderDishDto: OrderDishDto in orderDto.orderDishes) {
            if (orderDishDto.orderedCount <= 0) {
                return false to "Can't add dish with non-positive count (dish id=${orderDishDto.dishId})"
            }
        }

        val order: OrderEntity = try {
            withContext(Dispatchers.IO) {
                orderRepository.addOrder(orderDto)
            }
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            // Incorrect data from the user
            logDebugOnIncorrectData(orderDto, "OrderService::addOrder(OrderDto)", ex)
            return false to "Can't add order: incorrect data provided"
        } catch (ex: Throwable) {
            logError("OrderService::addOrder(OrderDto)", ex)
            return false to "Can't add order $orderDto"
        }

        val orderId: Int = order.orderId!!
        return try {
            orderScheduler.addOrder(order)
            true to "Added order with id $orderId"
        } catch (ex: Throwable) {
            withContext(Dispatchers.IO) {
                orderRepository.deleteById(orderId)
            }
            logError("Could not add order ${order.orderId}", "OrderScheduler::addOrder(OrderEntity)", ex)
            false to "Was not able to add order"
        }
    }

    final suspend fun deleteOrder(orderId: Int, deleterLogin: String): Pair<Boolean, String> {
        val response = verifyUserOwnsOrder(deleterLogin, orderId)
        val userOwnsOrder = response.first
        return when {
            userOwnsOrder -> forceDeleteOrder(orderId)
            else -> response
        }
    }

    final suspend fun forceDeleteOrder(orderId: Int): Pair<Boolean, String> {
        if (!orderScheduler.lockOrderIfCanBeChanged(orderId))
            return false to "Can't delete ready or not existing order"

        return try {
            withContext(Dispatchers.IO) {
                orderRepository.deleteById(orderId)
            }
            orderScheduler.deleteOrder(orderId)
            true to "Deleted order with id $orderId"
        } catch (ex: Throwable) {
            logError("OrderService::deleteOrder(int)", ex)
            false to "Can't delete order: internal server error"
        } finally {
            // Order is not unlocked intentionally as it was deleted
        }
    }

    final fun onReadyOrder(orderId: Int) {
        orderRepository.setOrderReady(orderId)
    }

    final suspend fun addDishToOrder(orderAddDishDto: OrderAddDishDto, adderLogin: String): Pair<Boolean, String> {
        val orderId = orderAddDishDto.orderId
        val response = verifyUserOwnsOrder(adderLogin, orderId)
        if (!response.first) {
            return response
        }

        val addingCount = orderAddDishDto.addingCount
        if (addingCount <= 0)
            return false to "Can't add dish to the order: non-positive count"

        if (!orderScheduler.lockOrderIfCanBeChanged(orderId))
            return false to "Can't add dish to the ready or not existing order"

        return try {
            val dishId: Int = orderAddDishDto.dishId
            val dishEntity: DishEntity = withContext(Dispatchers.IO) {
                val nullableDishEntity: DishEntity? = dishRepository.findByIdOrNull(dishId)
                if (nullableDishEntity != null) {
                    orderRepository.addDishToOrder(orderAddDishDto)
                }
                return@withContext nullableDishEntity
            } ?: return false to "Can't add dish: order with id $orderId doesn't have dish with id $dishId"

            orderScheduler.addDishesToOrder(dishEntity, orderAddDishDto)
            true to "Added $addingCount dishes with id $dishId and name ${dishEntity.name}"
        } catch (ex: UnsupportedOperationException) {
            logError("OrderService::addDishToOrder(OrderAddDishDto)", ex)
            false to "Can't add dish to the ready order"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(orderAddDishDto, "OrderService::addDishToOrder(OrderAddDishDto)", ex)
            false to "Can't add dish: incorrect data provided"
        } finally {
            orderScheduler.unlockOrder(orderId)
        }
    }

    final suspend fun deleteDishFromOrder(
        orderDeleteDishDto: OrderDeleteDishDto,
        deleterLogin: String,
    ): Pair<Boolean, String> {
        val orderId = orderDeleteDishDto.orderId
        val response = verifyUserOwnsOrder(deleterLogin, orderId)
        if (!response.first) {
            return response
        }

        val deletingCount = orderDeleteDishDto.deletingCount
        if (deletingCount <= 0)
            return false to "Can't delete dish from the order: non-positive count"

        if (!orderScheduler.lockOrderIfCanBeChanged(orderId))
            return false to "Can't delete dish from the ready or not existing order"

        return try {
            val deletedSuccessfully: Boolean = withContext(Dispatchers.IO) {
                orderRepository.deleteDishFromOrder(orderDeleteDishDto)
            }
            if (!deletedSuccessfully) {
                false to "Can't delete dish from the order: incorrect data"
            }

            orderScheduler.cancelDishes(orderDeleteDishDto)
            true to "Deleted $deletingCount dishes"
        } catch (ex: UnsupportedOperationException) {
            logError("OrderService::deleteDishFromOrder(OrderDeleteDishDto)", ex)
            false to "Can't delete dish from the ready order"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(
                orderDeleteDishDto,
                "OrderService::deleteDishFromOrder(OrderDeleteDishDto)",
                ex
            )
            false to "Can't delete dish from the order: incorrect data"
        } finally {
            orderScheduler.unlockOrder(orderId)
        }
    }

    final fun onOrderPaid(orderId: Int): PaidOrderStatus {
        try {
            val order: OrderEntity = orderRepository.findByIdOrNull(orderId)
                ?: return PaidOrderStatus.ORDER_DOES_NOT_EXIST
            if (!order.isReady) {
                return PaidOrderStatus.ORDER_IS_NOT_READY
            }
            orderRepository.onReadyOrderPaid(orderId)
            return PaidOrderStatus.OK
        } catch (ex: Throwable) {
            logError("(id=$orderId)", "OrderService::onPaidOrder(PaidOrderDto)", ex)
            return PaidOrderStatus.OTHER_ERROR
        }
    }

    final fun notifyFirstOrderDishStartedCooking(orderId: Int) {
        orderRepository.setOrderStartedCooking(orderId)
    }

    private final fun verifyUserHasThisId(userLogin: String, userId: Int): Boolean {
        val userEntity: UserEntity = userRepository.findByLogin(userLogin)
            ?: return false

        return userEntity.userId == userId
    }

    private final suspend fun verifyUserOwnsOrder(userLogin: String, orderId: Int): Pair<Boolean, String> {
        val userEntity: UserEntity? = withContext(Dispatchers.IO) {
            userRepository.findByLogin(userLogin)
        }
        if (userEntity == null) {
            return false to "User that performs the request no longer exists"
        }

        if (!userHasOrderWithId(userEntity, orderId)) {
            return false to "You don't have any order with id $orderId"
        }

        return true to ""
    }

    private final fun userHasOrderWithId(user: UserEntity, orderId: Int): Boolean {
        for (order in user.orders) {
            if (order.orderId == orderId) {
                return true
            }
        }
        return false
    }

    override fun destroy() {
        orderScheduler.destroy()
    }
}

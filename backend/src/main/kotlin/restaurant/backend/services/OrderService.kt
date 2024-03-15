package restaurant.backend.services

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.repositories.OrderRepository
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
    private val orderRepository: OrderRepository
) : LoggingHelper<OrderService>(OrderService::class.java), DisposableBean {

    private final val orderScheduler = OrderScheduler(this)

    final fun retrieveAllOrders(): List<OrderDto> = orderRepository.findAllByOrderByOrderIdAsc().map { order: OrderEntity -> OrderDto(order) }


    final fun retrieveOrderById(orderId: Int): OrderDto? {
        val orderEntity: Optional<OrderEntity> = orderRepository.findById(orderId)
        return when {
            orderEntity.isPresent -> OrderDto(orderEntity.get())
            else -> null
        }
    }

    final suspend fun addOrder(orderDto: OrderDto): Pair<Int?, String> {
        if (orderDto.orderDishes.size <= 0) {
            return null to "Can't add order without dishes"
        }
        for (orderDishDto: OrderDishDto in orderDto.orderDishes) {
            if (orderDishDto.orderedCount <= 0) {
                return null to "Can't add dish with non-positive count (dish id=${orderDishDto.dishId})"
            }
        }

        val order: OrderEntity = try {
            withContext(Dispatchers.IO) {
                orderRepository.addOrder(orderDto)
            }
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            // Incorrect data from the user
            debugLogOnIncorrectData(orderDto, "OrderService::addOrder(OrderDto)", ex)
            return null to "Can't add order: incorrect data provided"
        } catch (ex: Throwable) {
            errorLog("OrderService::addOrder(OrderDto)", ex)
            return null to "Can't add order $orderDto"
        }

        val orderId: Int = order.orderId!!
        return try {
            orderScheduler.addOrder(order)
            orderId to ""
        } catch (ex: Throwable) {
            withContext(Dispatchers.IO) {
                orderRepository.deleteById(orderId)
            }
            errorLog("Could not add order $order", "OrderScheduler::addOrder(OrderEntity)", ex)
            null to "Was not able to add order"
        }
    }

    final suspend fun deleteOrder(orderId: Int): Boolean {
        return orderScheduler.lockOrderIfCanBeChanged(orderId) && try {
            withContext(Dispatchers.IO) {
                orderRepository.deleteById(orderId)
            }
            orderScheduler.deleteOrder(orderId)
            true
        } catch (ex: Throwable) {
            errorLog("OrderService::deleteOrder(int)", ex)
            false
        } finally {
            orderScheduler.unlockOrder(orderId)
        }
    }

    final fun onReadyOrder(orderId: Int) {
        orderRepository.setOrderReady(orderId)
    }

    final suspend fun addDishToOrder(orderAddDishDto: OrderAddDishDto): Boolean {
        return orderAddDishDto.addingCount > 0 &&
                orderScheduler.lockOrderIfCanBeChanged(orderAddDishDto.orderId)
                && try {
            val updatedOrderEntity: OrderEntity = withContext(Dispatchers.IO) {
                orderRepository.addDishToOrder(orderAddDishDto)
            }

            val dishEntity: DishEntity = updatedOrderEntity
                .dishes
                .find { orderDishEntity: OrderDishEntity -> orderDishEntity.dishId == orderAddDishDto.dishId }!!
                .dish!!

            orderScheduler.addDishesToOrder(dishEntity, orderAddDishDto)
            true
        } catch (ex: UnsupportedOperationException) {
            errorLog("OrderService::addDishToOrder(OrderAddDishDto)", ex)
            false
        } catch (ex: Throwable) {
            debugLogOnIncorrectData(orderAddDishDto, "OrderService::addDishToOrder(OrderAddDishDto)", ex)
            false
        } finally {
            orderScheduler.unlockOrder(orderAddDishDto.orderId)
        }
    }

    final suspend fun deleteDishFromOrder(orderDeleteDishDto: OrderDeleteDishDto): Boolean {
        return orderDeleteDishDto.deletingCount > 0 &&
                orderScheduler.lockOrderIfCanBeChanged(orderDeleteDishDto.orderId) &&
                try {
                    val deletedSuccessfully: Boolean = withContext(Dispatchers.IO) {
                        orderRepository.deleteDishFromOrder(orderDeleteDishDto)
                    }
                    if (deletedSuccessfully) {
                        orderScheduler.cancelDishes(orderDeleteDishDto)
                    }
                    deletedSuccessfully
                } catch (ex: UnsupportedOperationException) {
                    errorLog("OrderService::deleteDishFromOrder(OrderDeleteDishDto)", ex)
                    false
                } catch (ex: Throwable) {
                    debugLogOnIncorrectData(
                        orderDeleteDishDto,
                        "OrderService::deleteDishFromOrder(OrderDeleteDishDto)",
                        ex
                    )
                    false
                } finally {
                    orderScheduler.unlockOrder(orderDeleteDishDto.orderId)
                }
    }

    final fun onOrderPaid(orderId: Int): PaidOrderStatus {
        try {
            val order: OrderEntity = orderRepository.findByIdOrNull(orderId)
                ?: return PaidOrderStatus.ORDER_DOES_NOT_EXIST
            if (!order.isReady) {
                return PaidOrderStatus.ORDER_IS_NOT_READY
            }
            orderRepository.onReadyOrderPaid(order)
            return PaidOrderStatus.OK
        } catch (ex: Throwable) {
            debugLogOnIncorrectData("(id=$orderId)", "OrderService::onPaidOrder(PaidOrderDto)", ex)
            return PaidOrderStatus.OTHER_ERROR
        }
    }

    final fun notifyFirstOrderDishStartedCooking(orderId: Int) {
        orderRepository.setOrderStartedCooking(orderId)
    }

    @PostConstruct
    fun initSchedulerOnAppInitialization() {
        orderScheduler.scheduleOrdersOnAppInitialization(orderRepository.findAll())
    }

    override fun destroy() {
        orderScheduler.destroy()
    }
}

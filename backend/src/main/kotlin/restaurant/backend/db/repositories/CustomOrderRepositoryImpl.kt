package restaurant.backend.db.repositories

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderDishWeakEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.repositories.CustomOrderRepository
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.util.LoggingHelper

open class CustomOrderRepositoryImpl(@Autowired private val entityManager: EntityManager) :
    LoggingHelper<CustomOrderRepositoryImpl>(CustomOrderRepositoryImpl::class.java), CustomOrderRepository {

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun addOrder(orderDto: OrderDto): OrderEntity {
        val orderId: Int = entityManager
            .createNativeQuery(
                "INSERT INTO orders(user_id) VALUES (${orderDto.orderOwnerId}) RETURNING order_id;",
                Int::class.java
            )
            .singleResult as Int
        logDebug("Added order with id $orderId", "CustomOrderRepositoryImpl::addOrder()")

        var count: Int = orderDto.orderDishes.size
        assert(count > 0)
        val valuesBuffer = StringBuilder(count * "(00, 00, 00)".length)
        for (orderDishDto: OrderDishDto in orderDto.orderDishes) {
            val orderedCount: Int = orderDishDto.orderedCount
            assert(orderedCount > 0)
            valuesBuffer.append("($orderId, ${orderDishDto.dishId}, ${orderedCount})")
            if (--count > 0) {
                valuesBuffer.append(',')
            }
        }

        entityManager
            .createNativeQuery(
                """
                DO $$
                DECLARE tmprow RECORD;
                BEGIN FOR tmprow IN
                    INSERT INTO order_dishes(order_id, dish_id, ordered_count) VALUES $valuesBuffer RETURNING *
                LOOP
                    UPDATE dishes SET quantity = quantity - tmprow.ordered_count WHERE dish_id = tmprow.dish_id;
                END LOOP;
                END; $$;
                """.trimIndent()
            ).executeUpdate()
        return entityManager.find(OrderEntity::class.java, orderId)
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun addDishToOrder(orderAddDishDto: OrderAddDishDto) {
        val addingCount: Int = orderAddDishDto.addingCount
        assert(addingCount > 0)
        val dishId: Int = orderAddDishDto.dishId
        val orderId: Int = orderAddDishDto.orderId
        entityManager
            .createNativeQuery(
                """
                UPDATE dishes SET quantity = quantity - $addingCount WHERE dish_id = $dishId;
                INSERT INTO order_dishes(order_id, dish_id, ordered_count)
                VALUES ($orderId, $dishId, $addingCount)
                ON CONFLICT(order_id, dishId) DO UPDATE SET ordered_count = ordered_count + excluded.adding_count;
                UPDATE orders SET is_ready = FALSE WHERE order_id = $orderId;
                """.trimIndent()
            )
            .executeUpdate()
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun deleteDishFromOrder(orderDeleteDishDto: OrderDeleteDishDto): Boolean {
        val deletingCount: Int = orderDeleteDishDto.deletingCount
        assert(deletingCount > 0)
        val dishId: Int = orderDeleteDishDto.dishId
        val orderId: Int = orderDeleteDishDto.orderId
        val orderDishEntity: OrderDishWeakEntity = try {
            entityManager.createNativeQuery(
                "SELECT * FROM order_dishes WHERE order_id = $orderId AND dish_id = $dishId;",
                OrderDishWeakEntity::class.java
            )
                .singleResult as OrderDishWeakEntity
        } catch (ex: Throwable) {
            return false
        }

        val currentOrderCount = orderDishEntity.orderedCount
        if (deletingCount > currentOrderCount) {
            return false
        }
        val relationId: Long = orderDishEntity.relationId
        val sqlStrQuery = if (deletingCount < currentOrderCount) {
            """
            UPDATE order_dishes SET ordered_count = ordered_count - $deletingCount WHERE relation_id = ${relationId};
            UPDATE dishes SET quantity = quantity + $deletingCount WHERE dish_id = $dishId;
            """.trimIndent()
        } else {
            """
            DELETE FROM order_dishes WHERE relation_id = ${relationId};
            UPDATE dishes SET quantity = quantity + $deletingCount WHERE dish_id = $dishId;
            """.trimIndent()
        }
        entityManager.createNativeQuery(sqlStrQuery).executeUpdate()
        return true
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun onReadyOrderPaid(orderId: Int) {
        val sqlStrQuery =
            """
            WITH od AS (DELETE FROM order_dishes WHERE order_id = $orderId RETURNING dish_id, ordered_count)
            UPDATE restaurant_info SET "value" = "value" + (
                SELECT sum(od.ordered_count * d.price)
                FROM od
                JOIN dishes d
                ON od.dish_id = d.dish_id
            )
            WHERE "key" = 'revenue';
            DELETE FROM orders WHERE order_id = $orderId;
            """.trimIndent()
        entityManager.createNativeQuery(sqlStrQuery).executeUpdate()
    }
}

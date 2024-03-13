package restaurant.backend.db.repository

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderDishWeakEntity
import restaurant.backend.db.entities.OrderEntity
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
        debugLog("Added order with id $orderId", "CustomOrderRepositoryImpl::addOrder()")

        var count: Int = orderDto.orderDishes.size
        assert(count > 0)
        val valuesBuffer = StringBuilder(count * "(0, 0, 0)".length)
        for (orderDishDto: OrderDishDto in orderDto.orderDishes) {
            val orderedCount: Int = orderDishDto.orderedCount
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
                DELETE FROM dishes WHERE quantity <= 0;
                """.trimIndent()).executeUpdate()
        return entityManager.find(OrderEntity::class.java, orderId)
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun addDishToOrder(orderAddDishDto: OrderAddDishDto): OrderEntity? {
        val addingCount: Int = orderAddDishDto.addingCount
        val dishId: Int = orderAddDishDto.dishId
        val orderId: Int = orderAddDishDto.orderId
        assert(addingCount > 0)
        return entityManager
            .createNativeQuery(
                """
            UPDATE dishes SET quantity = quantity - $addingCount WHERE dish_id = $dishId;
            INSERT INTO order_dishes(order_id, dish_id, ordered_count)
            VALUES ($orderId, $dishId, $addingCount)
            ON CONFLICT DO UPDATE SET ordered_count = ordered_count + excluded.adding_count;
            UPDATE orders SET is_ready = FALSE WHERE order_id = $orderId RETURNING *;
            """.trimIndent(), OrderEntity::class.java
            )
            .singleResult as OrderEntity
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun deleteDishFromOrder(orderDeleteDishDto: OrderDeleteDishDto): Boolean {
        val deletingCount: Int = orderDeleteDishDto.deletingCount
        val dishId: Int = orderDeleteDishDto.dishId
        val orderId: Int = orderDeleteDishDto.orderId
        if (deletingCount <= 0) {
            return false
        }

        val orderDishEntity: OrderDishWeakEntity = try {
            entityManager.createNativeQuery(
                "SELECT * FROM order_dishes WHERE order_id = $orderId AND dish_id = $dishId;",
                OrderDishWeakEntity::class.java)
                .singleResult as OrderDishWeakEntity
        } catch (ex: Throwable) {
            return false
        }

        if (deletingCount > orderDishEntity.orderedCount) {
            return false
        }

        entityManager.createNativeQuery(
            """
            UPDATE order_dishes SET ordered_count = ordered_count - $deletingCount
            WHERE relation_id = ${orderDishEntity.relationId};
            UPDATE dishes SET quantity = quantity + $deletingCount WHERE dish_id = $dishId;
            """.trimIndent()
        ).executeUpdate()
        return true
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class], readOnly = false)
    override fun onReadyOrderPaid(order: OrderEntity) {
        assert(order.isReady)
        val orderId: Int = order.orderId!!
        entityManager
            .createNativeQuery("""
            WITH od AS (DELETE FROM order_dishes WHERE order_id = $orderId RETURNING dish_id, ordered_count)
            UPDATE restaurant_info SET "value" = "value" + (
                SELECT sum(od.ordered_count * d.price)
                FROM od
                JOIN dishes d
                ON od.dish_id = d.dish_id
            ) WHERE "key" = 'revenue';
            DELETE FROM orders WHERE order_id = $orderId;""".trimIndent()
            ).executeUpdate()
    }
}

package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "order_dishes")
@NoArgsConstructor
data class OrderDishEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(name = "relation_id", unique = true, updatable = false, insertable = false)
    var relationId: Long? = null,

    @Basic(optional = false)
    @Column(name = "order_id", nullable = false, updatable = false)
    var orderId: Int,

    @Basic(optional = false)
    @Column(name = "dish_id", nullable = false, updatable = false)
    var dishId: Int,

    @Basic(optional = false)
    @Column(name = "ordered_count", nullable = false)
    var orderedCount: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
    val order: OrderEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", referencedColumnName = "dish_id", insertable = false, updatable = false)
    val dish: DishEntity? = null,
)

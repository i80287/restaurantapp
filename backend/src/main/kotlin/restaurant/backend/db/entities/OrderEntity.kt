package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import kotlin.jvm.Transient

@Entity
@Table(name = "orders")
@NoArgsConstructor
data class OrderEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(name = "order_id")
    var orderId: Int = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Int,

    @Column(name = "start_time", nullable = false)
    var startTime: Long = System.currentTimeMillis(),

    @Column(name = "is_ready", nullable = false, updatable = true)
    var isReady: Boolean = false,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    val dishes: MutableList<OrderDishEntity> = mutableListOf(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, nullable = false)
    var user: UserEntity? = null
)

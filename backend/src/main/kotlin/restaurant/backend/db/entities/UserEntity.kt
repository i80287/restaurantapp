package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor

@Entity
@Table(name = "users")
@NoArgsConstructor
class UserEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Int = 0,

    @Basic(optional = false)
    @Column(name = "login", unique = true, nullable = false)
    val login: String,

    @Basic(optional = false)
    @Column(name = "password_hash", nullable = false)
    val passwordHash: Long,

    @Basic(optional = false)
    @Column(name = "is_admin", nullable = false)
    val isAdmin: Boolean,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    val orders: MutableList<OrderEntity> = mutableListOf()
)

package restaurant.backend.db.entities

import restaurant.backend.dto.Role
import jakarta.persistence.*
import lombok.NoArgsConstructor
import java.util.*

@Entity
@Table(name = "users")
@NoArgsConstructor
data class UserEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Int? = null,

    @Basic(optional = false)
    @Column(name = "login", unique = true, nullable = false)
    val login: String,

    @Basic(optional = false)
    @Column(name = "password_hash", nullable = false, columnDefinition = "uuid")
    val passwordHash: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    val role: Role,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    val orders: MutableList<OrderEntity> = mutableListOf()
) {
    override fun toString(): String {
        return "UserEntity(userId=$userId,login=$login,passwordHash=$passwordHash,role=$role,orders=${orders.size})"
    }
}

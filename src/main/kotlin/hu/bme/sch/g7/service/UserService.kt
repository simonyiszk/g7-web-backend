package hu.bme.sch.g7.service

import hu.bme.sch.g7.dao.UserRepository
import hu.bme.sch.g7.model.UserEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Suppress("RedundantModalityModifier") // Spring transactional proxy requires it not to be final
@Service
open class UserService(
        val users: UserRepository
) {

    @Transactional
    open fun save(user: UserEntity) {
        users.save(user)
    }

    @Transactional(readOnly = true)
    open fun getById(id: String): UserEntity = users.findByPekId(id).orElseThrow()

    @Transactional(readOnly = true)
    open fun exists(id: String) = users.existsByPekId(id)

    @Transactional(readOnly = true)
    fun searchByG7Id(g7id: String): Optional<UserEntity> {
        return users.findByG7id(g7id)
    }

}
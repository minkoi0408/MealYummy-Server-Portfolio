package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.auth.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByRoleCode(String roleCode);
}

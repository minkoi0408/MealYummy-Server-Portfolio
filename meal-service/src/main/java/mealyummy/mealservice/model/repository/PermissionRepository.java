package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.auth.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface PermissionRepository extends MongoRepository<Permission,String> {
    long countByPermissionCodeIn(Collection<String> permissionCodes);
}

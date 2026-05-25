package mealyummy.mealservice.model.repository.subscription;

import mealyummy.mealservice.model.entity.subscription.Bundle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BundleRepository extends MongoRepository<Bundle, String> {
    Optional<Bundle> findByIdAndIsActiveTrue(String id);
}

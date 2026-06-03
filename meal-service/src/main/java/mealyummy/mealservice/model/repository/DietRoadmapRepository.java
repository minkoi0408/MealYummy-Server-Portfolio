package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.DietRoadmap;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DietRoadmapRepository extends MongoRepository<DietRoadmap, String> {

    Optional<DietRoadmap> findFirstByUserIdOrderByGeneratedAtDesc(String userId);

    List<DietRoadmap> findAllByUserIdOrderByGeneratedAtDesc(String userId);

    void deleteByIdAndUserId(String id, String userId);
}

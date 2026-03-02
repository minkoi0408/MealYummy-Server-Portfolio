package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {
    List<Tag> findAllByActiveTrue();
    boolean existsByNameAndActiveTrue(String name);
    List<Tag> findByNameInAndActiveTrue(List<String> names);
}

package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {
}

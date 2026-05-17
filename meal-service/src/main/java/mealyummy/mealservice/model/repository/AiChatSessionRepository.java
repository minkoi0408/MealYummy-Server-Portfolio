package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.AiChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatSessionRepository extends MongoRepository<AiChatSession, String> {
}

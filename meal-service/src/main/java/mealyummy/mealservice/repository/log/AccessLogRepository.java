package mealyummy.mealservice.repository.log;

import mealyummy.mealservice.entity.log.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessLogRepository extends MongoRepository<AccessLog, String> {
    Page<AccessLog> findByUsernameContainingIgnoreCaseOrIpAddressContainingIgnoreCase(String username, String ipAddress, Pageable pageable);
}
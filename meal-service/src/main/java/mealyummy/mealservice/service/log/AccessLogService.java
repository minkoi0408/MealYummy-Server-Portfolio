package mealyummy.mealservice.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.entity.log.AccessLog;
import mealyummy.mealservice.repository.log.AccessLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    @Async
    public void saveLogAsync(AccessLog logEntry) {
        try {
            accessLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save access log asynchronously: {}", e.getMessage());
        }
    }

    public Page<AccessLog> getLogs(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.trim().isEmpty()) {
            return accessLogRepository.findByUsernameContainingIgnoreCaseOrIpAddressContainingIgnoreCase(search, search, pageable);
        }
        return accessLogRepository.findAll(pageable);
    }
}
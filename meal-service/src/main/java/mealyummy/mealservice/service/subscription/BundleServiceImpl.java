package mealyummy.mealservice.service.subscription;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.model.repository.subscription.BundleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BundleServiceImpl implements BundleService {

    private final BundleRepository bundleRepository;

    @Override
    public List<Bundle> getAllActiveBundles() {
        return bundleRepository.findAll().stream()
                .filter(Bundle::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Bundle getBundleById(String id) {
        return bundleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUNDLE_NOT_FOUND));
    }

    @Override
    public Bundle createBundle(Bundle bundle) {
        return bundleRepository.save(bundle);
    }
}

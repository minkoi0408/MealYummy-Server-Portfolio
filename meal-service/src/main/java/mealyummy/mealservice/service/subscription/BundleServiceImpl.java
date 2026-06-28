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

    @Override
    public org.springframework.data.domain.Page<Bundle> getAllBundles(org.springframework.data.domain.Pageable pageable) {
        return bundleRepository.findAll(pageable);
    }

    @Override
    public Bundle updateBundle(String id, Bundle bundle) {
        Bundle existingBundle = bundleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUNDLE_NOT_FOUND));

        existingBundle.setName(bundle.getName());
        existingBundle.setDescription(bundle.getDescription());
        existingBundle.setDurations(bundle.getDurations());
        existingBundle.setFeatures(bundle.getFeatures());
        existingBundle.setActive(bundle.isActive());

        return bundleRepository.save(existingBundle);
    }

    @Override
    public String changeState(String id) {
        Bundle bundle = bundleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUNDLE_NOT_FOUND));

        String msg = "Gói " + bundle.getName();
        if (bundle.isActive()) {
            bundle.setActive(false);
            msg += " đã được ngừng kích hoạt.";
        } else {
            bundle.setActive(true);
            msg += " đã được kích hoạt.";
        }

        bundleRepository.save(bundle);
        return msg;
    }
}

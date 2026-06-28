package mealyummy.mealservice.service.subscription;

import mealyummy.mealservice.model.entity.subscription.Bundle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BundleService {
    List<Bundle> getAllActiveBundles();
    Page<Bundle> getAllBundles(Pageable pageable);
    Bundle getBundleById(String id);
    Bundle createBundle(Bundle bundle);
    Bundle updateBundle(String id, Bundle bundle);
    String changeState(String id);
}

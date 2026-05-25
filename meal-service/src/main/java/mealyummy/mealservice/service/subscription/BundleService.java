package mealyummy.mealservice.service.subscription;

import mealyummy.mealservice.model.entity.subscription.Bundle;
import java.util.List;

public interface BundleService {
    List<Bundle> getAllActiveBundles();
    Bundle getBundleById(String id);
    Bundle createBundle(Bundle bundle);
}

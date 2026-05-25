package mealyummy.mealservice.service.ingredient;

import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IngredientService {
    IngredientDTO create(IngredientDTO request);
    Page<IngredientDTO> getAll(Pageable pageable);
    IngredientDTO get(String id);
    IngredientDTO update(String id, IngredientDTO request);
    String changeState(String id);
    void delete(String id);
    void deleteBulk(List<String> ids);
    List<IngredientDTO> createBulk(List<IngredientDTO> requests);
}
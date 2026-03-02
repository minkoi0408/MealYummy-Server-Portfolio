package mealyummy.mealservice.service.ingredient;

import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;

import java.util.List;

public interface IngredientService {
    IngredientDTO create(IngredientDTO request);
    List<IngredientDTO> getAll();
    List<IngredientDTO> createBulk(List<IngredientDTO> requests);
}
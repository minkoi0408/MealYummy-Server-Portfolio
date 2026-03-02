package mealyummy.mealservice.service.ingredient;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.Ingredient;
import mealyummy.mealservice.model.repository.IngredientRepository;
import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;

    @Override
    @Transactional
    public IngredientDTO create(IngredientDTO request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_INVALID_NAME);
        }

        String trimmedName = request.getName().trim();
        String formattedName = trimmedName.substring(0, 1).toUpperCase() + trimmedName.substring(1).toLowerCase();

        if (ingredientRepository.existsByNameAndActiveTrue(formattedName)) {
            throw new AppException(ErrorCode.INGREDIENT_ALREADY_EXISTS);
        }

        Ingredient ingredient = Ingredient.builder()
                .name(formattedName)
                .active(true)
                .build();

        ingredient = ingredientRepository.save(ingredient);
        return ingredient.convert();
    }

    @Override
    public List<IngredientDTO> getAll() {
        List<Ingredient> ingredients = ingredientRepository.findAllByActiveTrue();

        return ingredients.stream()
                .map(Ingredient::convert)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<IngredientDTO> createBulk(List<IngredientDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_INVALID_NAME);
        }

        List<String> formattedNames = requests.stream()
                .filter(req -> req.getName() != null && !req.getName().trim().isEmpty())
                .map(req -> {
                    String trimmed = req.getName().trim();
                    return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                })
                .distinct()
                .toList();

        List<Ingredient> existingIngredients = ingredientRepository.findByNameInAndActiveTrue(formattedNames);
        Set<String> existingNames = existingIngredients.stream()
                .map(Ingredient::getName)
                .collect(Collectors.toSet());

        List<Ingredient> newIngredients = formattedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> Ingredient.builder()
                        .name(name)
                        .active(true)
                        .build())
                .toList();

        if (newIngredients.isEmpty()) {
            return List.of();
        }

        List<Ingredient> savedIngredients = ingredientRepository.saveAll(newIngredients);

        return savedIngredients.stream()
                .map(Ingredient::convert)
                .toList();
    }
}
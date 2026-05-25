package mealyummy.mealservice.service.ingredient;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.food.Ingredient;
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
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

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
    public org.springframework.data.domain.Page<IngredientDTO> getAll(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Ingredient> ingredients = ingredientRepository.findAll(pageable);
        return ingredients.map(Ingredient::convert);
    }

    @Override
    public IngredientDTO get(String id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
        return ingredient.convert();
    }

    @Override
    @Transactional
    public IngredientDTO update(String id, IngredientDTO request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String trimmedName = request.getName().trim();
            String formattedName = trimmedName.substring(0, 1).toUpperCase() + trimmedName.substring(1).toLowerCase();

            if (!ingredient.getName().equals(formattedName) && ingredientRepository.existsByNameAndActiveTrue(formattedName)) {
                throw new AppException(ErrorCode.INGREDIENT_ALREADY_EXISTS);
            }
            ingredient.setName(formattedName);
        }

        ingredientRepository.save(ingredient);
        return ingredient.convert();
    }

    @Override
    @Transactional
    public String changeState(String id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

        String msg = "Nguyên liệu " + ingredient.getName();
        if (Boolean.TRUE.equals(ingredient.getActive())) {
            ingredient.setActive(false);
            msg += " đã được ẩn.";
        } else {
            ingredient.setActive(true);
            msg += " đã được hiển thị.";
        }
        ingredientRepository.save(ingredient);
        return msg;
    }

    @Override
    @Transactional
    public void delete(String id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

        ingredientRepository.delete(ingredient);
        removeIngredientFromMeals(id);
    }

    @Override
    @Transactional
    public void deleteBulk(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Ingredient> ingredients = (List<Ingredient>) ingredientRepository.findAllById(ids);
        ingredientRepository.deleteAll(ingredients);

        for (String id : ids) {
            removeIngredientFromMeals(id);
        }
    }

    private void removeIngredientFromMeals(String ingredientId) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("ingredients.ingredientId").is(ingredientId));
        
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update();
        update.pull("ingredients", org.springframework.data.mongodb.core.query.Query.query(org.springframework.data.mongodb.core.query.Criteria.where("ingredientId").is(ingredientId)));
        
        mongoTemplate.updateMulti(query, update, mealyummy.mealservice.model.entity.food.Meal.class);
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
package mealyummy.mealservice.service.meal;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.service.meal.dto.MealIngredientDTO;
import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mealyummy.mealservice.model.repository.MealRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final mealyummy.mealservice.service.cloudinary.CloudinaryService cloudinaryService;

    public MealResponseDTO convertMealToMealResponseDTO(mealyummy.mealservice.model.entity.food.Meal meal) {
        List<String> categories = meal.getCategories() != null ? meal.getCategories() : List.of();
        List<String> tags = meal.getTags() != null ? meal.getTags() : List.of();
        List<MealIngredientDTO> ingredients = meal.getIngredients() != null ? meal.getIngredients().stream().map(mealyummy.mealservice.model.pojo.MealIngredient::convert).toList() : List.of();
        List<mealyummy.mealservice.service.meal.dto.MealImageDTO> images = meal.getImages() != null ? meal.getImages().stream()
                .map(img -> mealyummy.mealservice.service.meal.dto.MealImageDTO.builder()
                        .url(img.getUrl())
                        .isThumbnail(img.getIsThumbnail())
                        .build())
                .toList() : List.of();

        return MealResponseDTO.builder()
                .id(meal.getId())
                .name(meal.getName())
                .description(meal.getDescription())
                .nutrition(meal.getNutrition() != null ? mealyummy.mealservice.service.meal.dto.NutritionDTO.builder()
                        .calories(meal.getNutrition().getCalories())
                        .protein(meal.getNutrition().getProtein())
                        .carbs(meal.getNutrition().getCarbs())
                        .fat(meal.getNutrition().getFat())
                        .fiber(meal.getNutrition().getFiber())
                        .build() : null)
                .categories(categories)
                .tags(tags)
                .ingredients(ingredients)
                .images(images)
                .createdAt(meal.getCreatedAt() != null ? mealyummy.mealservice.core.util.DateTimeFormat.formatInstantCustom(meal.getCreatedAt()) : null)
                .active(meal.getActive())
                .build();
    }

    @Override
    @Transactional
    public MealResponseDTO create(MealRequestDTO request) {

        // Format lại tên: chữ cái đầu hoa và sau thường
        String formattedName = request.getName().trim();
        formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1).toLowerCase();

        // Kiểm tra trùng tên món
        if (mealRepository.existsByName(formattedName))
            throw new AppException(ErrorCode.MEAL_ALREADY_EXISTS);

        // 1. Lấy Categories từ request
        List<String> categories = request.getCategories() != null ? request.getCategories() : List.of();

        // 2. Lấy Tags từ request
        List<String> tags = request.getTags() != null ? request.getTags() : List.of();

        // 3. Xử lý Nguyên liệu
        List<mealyummy.mealservice.model.pojo.MealIngredient> mealIngredients = request.getIngredients().stream().map(reqIng -> {
            return mealyummy.mealservice.model.pojo.MealIngredient.builder()
                    .name(reqIng.getName())
                    .value(reqIng.getValue())
                    .unit(reqIng.getUnit())
                    .build();
        }).toList();

        mealyummy.mealservice.model.pojo.Nutrition nutrition = null;
        if (request.getNutrition() != null) {
            nutrition = mealyummy.mealservice.model.pojo.Nutrition.builder()
                    .calories(request.getNutrition().getCalories())
                    .protein(request.getNutrition().getProtein())
                    .carbs(request.getNutrition().getCarbs())
                    .fat(request.getNutrition().getFat())
                    .fiber(request.getNutrition().getFiber())
                    .build();
        }

        List<mealyummy.mealservice.model.pojo.MealImage> mealImages = new java.util.ArrayList<>();
        if (request.getImages() != null) {
            mealImages = request.getImages().stream().map(img -> mealyummy.mealservice.model.pojo.MealImage.builder()
                    .url(img.getUrl())
                    .isThumbnail(img.getIsThumbnail() != null ? img.getIsThumbnail() : false)
                    .build()).toList();
        }

        mealyummy.mealservice.model.entity.food.Meal meal = mealyummy.mealservice.model.entity.food.Meal.builder()
                .name(formattedName)
                .description(request.getDescription())
                .nutrition(nutrition)
                .categories(categories)
                .tags(tags)
                .ingredients(mealIngredients)
                .images(mealImages)
                .build();

        mealRepository.save(meal);
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public List<MealResponseDTO> initHealthyMealsData() {
        // Obsolete method, keeping interface happy
        return List.of();
    }

    @Override
    public org.springframework.data.domain.Page<MealResponseDTO> getAll(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<mealyummy.mealservice.model.entity.food.Meal> meals = mealRepository.findAll(pageable);
        return meals.map(this::convertMealToMealResponseDTO);
    }

    @Override
    public MealResponseDTO get(String id) {
        mealyummy.mealservice.model.entity.food.Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public MealResponseDTO update(String id, MealRequestDTO request) {
        mealyummy.mealservice.model.entity.food.Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));

        String formattedName = request.getName().trim();
        formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1).toLowerCase();

        if (!meal.getName().equals(formattedName) && mealRepository.existsByName(formattedName)) {
            throw new AppException(ErrorCode.MEAL_ALREADY_EXISTS);
        }

        List<String> categories = request.getCategories() != null ? request.getCategories() : List.of();
        List<String> tags = request.getTags() != null ? request.getTags() : List.of();

        List<mealyummy.mealservice.model.pojo.MealIngredient> mealIngredients = request.getIngredients().stream().map(reqIng -> {
            return mealyummy.mealservice.model.pojo.MealIngredient.builder()
                    .name(reqIng.getName())
                    .value(reqIng.getValue())
                    .unit(reqIng.getUnit())
                    .build();
        }).toList();

        mealyummy.mealservice.model.pojo.Nutrition nutrition = null;
        if (request.getNutrition() != null) {
            nutrition = mealyummy.mealservice.model.pojo.Nutrition.builder()
                    .calories(request.getNutrition().getCalories())
                    .protein(request.getNutrition().getProtein())
                    .carbs(request.getNutrition().getCarbs())
                    .fat(request.getNutrition().getFat())
                    .fiber(request.getNutrition().getFiber())
                    .build();
        }

        List<mealyummy.mealservice.model.pojo.MealImage> mealImages = meal.getImages();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            mealImages = request.getImages().stream().map(img -> mealyummy.mealservice.model.pojo.MealImage.builder()
                    .url(img.getUrl())
                    .isThumbnail(img.getIsThumbnail() != null ? img.getIsThumbnail() : false)
                    .build()).toList();
        }

        meal.setName(formattedName);
        meal.setDescription(request.getDescription());
        meal.setNutrition(nutrition);
        meal.setCategories(categories);
        meal.setTags(tags);
        meal.setIngredients(mealIngredients);
        meal.setImages(mealImages);

        mealRepository.save(meal);
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public void delete(String id) {
        mealyummy.mealservice.model.entity.food.Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));
        mealRepository.delete(meal);
    }

    @Override
    @Transactional
    public void deleteBulk(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<mealyummy.mealservice.model.entity.food.Meal> meals = mealRepository.findAllById(ids);
        mealRepository.deleteAll(meals);
    }

    @Override
    @Transactional
    public List<MealResponseDTO> createBulk(List<MealRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.MEAL_NOT_FOUND);
        }
        List<MealResponseDTO> responses = new java.util.ArrayList<>();
        for (MealRequestDTO req : requests) {
            String formattedName = req.getName().trim();
            formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1).toLowerCase();
            if (mealRepository.existsByName(formattedName)) {
                continue; // Skip if meal already exists
            }
            responses.add(create(req));
        }
        return responses;
    }

    @Override
    @Transactional
    public MealResponseDTO uploadMealImage(String id, org.springframework.web.multipart.MultipartFile file) {
        mealyummy.mealservice.model.entity.food.Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));

        try {
            String imageUrl = cloudinaryService.uploadImage(file, "MealYummy/meals");
            
            if (meal.getImages() == null) {
                meal.setImages(new java.util.ArrayList<>());
            }
            
            mealyummy.mealservice.model.pojo.MealImage newImage = mealyummy.mealservice.model.pojo.MealImage.builder()
                    .url(imageUrl)
                    .isThumbnail(meal.getImages().isEmpty()) // if first image, make it main
                    .build();
                    
            meal.getImages().add(newImage);
            mealRepository.save(meal);
            
            return convertMealToMealResponseDTO(meal);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage(), e);
        }
    }
}
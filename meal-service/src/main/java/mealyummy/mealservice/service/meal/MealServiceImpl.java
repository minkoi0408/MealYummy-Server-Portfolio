package mealyummy.mealservice.service.meal;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.*;
import mealyummy.mealservice.model.pojo.MealIngredient;
import mealyummy.mealservice.model.pojo.Price;
import mealyummy.mealservice.model.repository.CategoryRepository;
import mealyummy.mealservice.model.repository.IngredientRepository;
import mealyummy.mealservice.model.repository.MealRepository;
import mealyummy.mealservice.model.repository.TagRepository;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import mealyummy.mealservice.service.meal.dto.MealIngredientDTO;
import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import mealyummy.mealservice.service.meal.dto.PriceDTO;
import mealyummy.mealservice.service.tag.dto.TagDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final IngredientRepository ingredientRepository;


    public MealResponseDTO convertMealToMealResponseDTO(Meal meal) {
        PriceDTO price = meal.getPrice().convert();
        List<CategoryDTO> categories = meal.getCategories().stream().map(Category::convertForMeal).toList();
        List<TagDTO> tags = meal.getTags().stream().map(Tag::convertForMeal).toList();
        List<MealIngredientDTO> ingredients= meal.getIngredients().stream().map(MealIngredient::convert).toList();

        return MealResponseDTO.builder()
                .name(meal.getName())
                .description(meal.getDescription())
                .price(price)
                .categories(categories)
                .tags(tags)
                .ingredients(ingredients)
                .build();
    }

    public Price returnPriceValid(PriceDTO priceDTO) {
        if (priceDTO == null) {
            throw new AppException(ErrorCode.MEAL_INVALID_PRICE);
        }

        Float minPrice = priceDTO.getMinPrice();
        Float maxPrice = priceDTO.getMaxPrice();

        // 2. Kiểm tra Null cho từng giá trị cụ thể
        if (minPrice == null || maxPrice == null) {
            throw new AppException(ErrorCode.MEAL_INVALID_PRICE);
        }

        // 3. Kiểm tra giá trị âm (Hoặc <= 0 nếu MealYummy không có món ăn miễn phí)
        if (minPrice < 0 || maxPrice < 0) {
            throw new AppException(ErrorCode.MEAL_INVALID_PRICE);
        }

        // 4. Logic cốt lõi: Min không được lớn hơn Max
        if (minPrice > maxPrice) {
            throw new AppException(ErrorCode.MEAL_INVALID_PRICE);
        }

        // 5. (Tùy chọn cực xịn): Giới hạn mức giá trần để tránh spam DB (Ví dụ: Max 100 triệu VND)
        if (maxPrice > 100_000_000f) {
            // Bạn có thể thêm mã lỗi MEAL_PRICE_TOO_HIGH vào ErrorCode
            throw new AppException(ErrorCode.MEAL_INVALID_PRICE);
        }

        return Price.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }

    @Override
    @Transactional
    public MealResponseDTO create(MealRequestDTO request) {

        Price mealPrice = returnPriceValid(request.getPrice());

        // Format lại tên: chữ cái đầu hoa và sau thường
        String formattedName = request.getName().trim();
        formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1).toLowerCase();

        // Kiểm tra trùng tên món
         if (mealRepository.existsByName(formattedName)) throw new AppException(ErrorCode.MEAL_ALREADY_EXISTS);

        // 1. Tìm Categories (Chỉ lấy những cái đang active)
        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                .stream().filter(Category::getActive).toList();
        if (categories.isEmpty() || categories.size() != request.getCategoryIds().size()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. Tìm Tags (Nếu có truyền lên)
        List<Tag> tags = List.of();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds())
                    .stream().filter(Tag::getActive).toList();
            if (tags.size() != request.getTagIds().size()) {
                throw new AppException(ErrorCode.TAG_NOT_FOUND);
            }
        }

        // 3. Xử lý Nguyên liệu (Kỹ thuật Embedded Document)
        // 3.1. Gom tất cả ID nguyên liệu từ request để query 1 lần cho tối ưu
        Set<String> ingredientIds = request.getIngredients().stream()
                .map(MealIngredientDTO::getIngredientId)
                .collect(Collectors.toSet());

        // 3.2. Query DB lấy nguyên liệu thật
        List<Ingredient> validIngredients = (List<Ingredient>) ingredientRepository.findAllById(ingredientIds);

        // 3.3. Chuyển thành Map<ID, Nguyên liệu> để tra cứu siêu tốc O(1)
        Map<String, Ingredient> ingredientMap = validIngredients.stream()
                .filter(Ingredient::getActive)
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        // 3.4. Duyệt qua request, lấy thông tin value/unit và "đắp" thêm Tên (Name) từ Map vào
        List<MealIngredient> mealIngredients = request.getIngredients().stream().map(reqIng -> {
            Ingredient dbIngredient = ingredientMap.get(reqIng.getIngredientId());
            if (dbIngredient == null) {
                throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
            }

            return MealIngredient.builder()
                    .ingredientId(dbIngredient.getId())
                    .name(dbIngredient.getName())
                    .value(reqIng.getValue())
                    .unit(reqIng.getUnit())
                    .build();
        }).toList();

        Meal meal = Meal.builder()
                .name(formattedName)
                .description(request.getDescription())
                .price(mealPrice)
                .categories(categories)
                .tags(tags)
                .ingredients(mealIngredients)
                .build();

        return convertMealToMealResponseDTO(meal);
    }


}
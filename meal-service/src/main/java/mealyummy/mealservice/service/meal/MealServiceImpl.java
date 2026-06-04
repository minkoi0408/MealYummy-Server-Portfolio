package mealyummy.mealservice.service.meal;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.food.Ingredient;
import mealyummy.mealservice.model.entity.food.Meal;
import mealyummy.mealservice.model.entity.food.Tag;
import mealyummy.mealservice.model.entity.food.Category;
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
    private final mealyummy.mealservice.service.cloudinary.CloudinaryService cloudinaryService;

    public MealResponseDTO convertMealToMealResponseDTO(Meal meal) {
        PriceDTO price = meal.getPrice() != null ? meal.getPrice().convert() : null;
        List<CategoryDTO> categories = meal.getCategories() != null ? meal.getCategories().stream().map(Category::convertForMeal).toList() : List.of();
        List<TagDTO> tags = meal.getTags() != null ? meal.getTags().stream().map(Tag::convertForMeal).toList() : List.of();
        List<MealIngredientDTO> ingredients = meal.getIngredients() != null ? meal.getIngredients().stream().map(MealIngredient::convert).toList() : List.of();
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
                .price(price)
                .categories(categories)
                .tags(tags)
                .ingredients(ingredients)
                .images(images)
                .createdAt(meal.getCreatedAt() != null ? mealyummy.mealservice.core.util.DateTimeFormat.formatInstantCustom(meal.getCreatedAt()) : null)
                .active(meal.getActive())
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

        // 5. (Tùy chọn cực xịn): Giới hạn mức giá trần để tránh spam DB (Ví dụ: Max 100
        // triệu VND)
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
        if (mealRepository.existsByName(formattedName))
            throw new AppException(ErrorCode.MEAL_ALREADY_EXISTS);

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

        // 3.4. Duyệt qua request, lấy thông tin value/unit và "đắp" thêm Tên (Name) từ
        // Map vào
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

        mealRepository.save(meal);
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public List<MealResponseDTO> initHealthyMealsData() {
        try {
            mealRepository.deleteAll();

            List<Ingredient> allIngredients = ingredientRepository.findAll();
            List<Tag> allTags = tagRepository.findAll();
            List<Category> allCategories = categoryRepository.findAll();

            if (allIngredients.isEmpty()) {
                throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
            }

            Map<String, String> nameToImageMap = Map.ofEntries(
                Map.entry("Salad Ức Gà Áp Chảo", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80"),
                Map.entry("Cơm Gạo Lứt Bò Lúc Lắc", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80"),
                Map.entry("Cá Hồi Nướng Măng Tây", "https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&q=80"),
                Map.entry("Mì Ý Nguyên Cám Sốt Bò Bằm", "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=800&q=80"),
                Map.entry("Ức Gà Luộc Xé Phay", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80"),
                Map.entry("Salad Cá Ngừ", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80"),
                Map.entry("Khoai Lang Luộc Trộn Sữa Chua", "https://images.unsplash.com/photo-1504630083234-14187a9df0f5?w=800&q=80"),
                Map.entry("Sinh Tố Chuối Bơ Đậu Phộng", "https://images.unsplash.com/photo-1484723091791-0fee59cb0c47?w=800&q=80"),
                Map.entry("Cơm Trắng Thịt Bò Xào Cần Tây", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80"),
                Map.entry("Yến Mạch Ngâm Qua Đêm Trái Cây", "https://images.unsplash.com/photo-1504630083234-14187a9df0f5?w=800&q=80"),
                Map.entry("Cháo Yến Mạch Tôm Thịt", "https://images.unsplash.com/photo-1504630083234-14187a9df0f5?w=800&q=80"),
                Map.entry("Bò Bít Tết (Kèm Salad)", "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800&q=80"),
                Map.entry("Gà Xào Xả Ớt Ăn Kiêng", "https://images.unsplash.com/photo-1588123190131-1c3fac394f4b?w=800&q=80"),
                Map.entry("Cá Basa Áp Chảo", "https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&q=80"),
                Map.entry("Đậu Hũ Tứ Xuyên Phiên Bản Ít Dầu", "https://images.unsplash.com/photo-1588123190131-1c3fac394f4b?w=800&q=80")
            );

            String[] healthyNames = nameToImageMap.keySet().toArray(new String[0]);

            List<Meal> meals = new java.util.ArrayList<>();
            java.util.Random rand = new java.util.Random();

            for (int i = 1; i <= 50; i++) {
                String baseName = healthyNames[rand.nextInt(healthyNames.length)];

                mealyummy.mealservice.model.pojo.Nutrition nutrition = mealyummy.mealservice.model.pojo.Nutrition
                        .builder()
                        .calories(300.0 + rand.nextInt(350)) // 300 - 650 kcal
                        .protein(25.0 + rand.nextInt(35)) // 25 - 60g protein (Gym focus)
                        .fat(5.0 + rand.nextInt(15)) // 5 - 20g fat
                        .carbs(20.0 + rand.nextInt(50)) // 20 - 70g carbs
                        .build();

                mealyummy.mealservice.model.pojo.Price price = mealyummy.mealservice.model.pojo.Price.builder()
                        .minPrice(45000.0f + rand.nextInt(20000))
                        .maxPrice(80000.0f + rand.nextInt(45000))
                        .build();

                // Random 3-5 ingredients
                List<MealIngredient> mealIngredients = new java.util.ArrayList<>();
                int numIng = 3 + rand.nextInt(3);
                for (int j = 0; j < numIng; j++) {
                    Ingredient randomIng = allIngredients.get(rand.nextInt(allIngredients.size()));
                    mealIngredients.add(MealIngredient.builder()
                            .ingredientId(randomIng.getId())
                            .name(randomIng.getName())
                            .value(50.0 + rand.nextInt(150))
                            .unit(mealyummy.mealservice.model.enums.IngredientUnit.G)
                            .build());
                }

                // Random 1-2 tags
                List<Tag> mealTags = new java.util.ArrayList<>();
                if (!allTags.isEmpty()) {
                    mealTags.add(allTags.get(rand.nextInt(allTags.size())));
                    mealTags.add(allTags.get(rand.nextInt(allTags.size())));
                }

                // Random category
                List<Category> mealCategories = new java.util.ArrayList<>();
                if (!allCategories.isEmpty()) {
                    mealCategories.add(allCategories.get(rand.nextInt(allCategories.size())));
                }

                // Images
                List<mealyummy.mealservice.model.pojo.MealImage> mealImages = new java.util.ArrayList<>();
                mealImages.add(mealyummy.mealservice.model.pojo.MealImage.builder()
                        .url(nameToImageMap.get(baseName))
                        .isThumbnail(true)
                        .build());

                Meal meal = Meal.builder()
                        .name(baseName + " (Combo " + i + ")")
                        .description("Bữa ăn Healthy thiết kế đặc biệt cho dân Gym, cân bằng dinh dưỡng, giàu đạm.")
                        .price(price)
                        .nutrition(nutrition)
                        .ingredients(mealIngredients)
                        .tags(mealTags)
                        .categories(mealCategories)
                        .images(mealImages)
                        .build();

                meals.add(meal);
            }

            mealRepository.saveAll(meals);
            return meals.stream().map(this::convertMealToMealResponseDTO).toList();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo Meals: " + e.getMessage());
        }
    }

    @Override
    public org.springframework.data.domain.Page<MealResponseDTO> getAll(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Meal> meals = mealRepository.findAll(pageable);
        return meals.map(this::convertMealToMealResponseDTO);
    }

    @Override
    public MealResponseDTO get(String id) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public MealResponseDTO update(String id, MealRequestDTO request) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));

        String formattedName = request.getName().trim();
        formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1).toLowerCase();

        if (!meal.getName().equals(formattedName) && mealRepository.existsByName(formattedName)) {
            throw new AppException(ErrorCode.MEAL_ALREADY_EXISTS);
        }

        Price mealPrice = returnPriceValid(request.getPrice());

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                .stream().filter(Category::getActive).toList();
        if (categories.isEmpty() || categories.size() != request.getCategoryIds().size()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        List<Tag> tags = List.of();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds())
                    .stream().filter(Tag::getActive).toList();
            if (tags.size() != request.getTagIds().size()) {
                throw new AppException(ErrorCode.TAG_NOT_FOUND);
            }
        }

        Set<String> ingredientIds = request.getIngredients().stream()
                .map(MealIngredientDTO::getIngredientId)
                .collect(Collectors.toSet());
        List<Ingredient> validIngredients = (List<Ingredient>) ingredientRepository.findAllById(ingredientIds);
        Map<String, Ingredient> ingredientMap = validIngredients.stream()
                .filter(Ingredient::getActive)
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

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

        meal.setName(formattedName);
        meal.setDescription(request.getDescription());
        meal.setPrice(mealPrice);
        meal.setCategories(categories);
        meal.setTags(tags);
        meal.setIngredients(mealIngredients);

        mealRepository.save(meal);
        return convertMealToMealResponseDTO(meal);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEAL_NOT_FOUND));
        mealRepository.delete(meal);
    }

    @Override
    @Transactional
    public void deleteBulk(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Meal> meals = mealRepository.findAllById(ids);
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
        Meal meal = mealRepository.findById(id)
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
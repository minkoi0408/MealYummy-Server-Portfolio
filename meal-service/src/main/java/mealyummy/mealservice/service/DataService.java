package mealyummy.mealservice.service;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.food.Category;
import mealyummy.mealservice.model.entity.food.Ingredient;
import mealyummy.mealservice.model.entity.food.Tag;
import mealyummy.mealservice.service.category.CategoryService;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import mealyummy.mealservice.service.ingredient.IngredientService;
import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import mealyummy.mealservice.service.tag.TagService;
import mealyummy.mealservice.service.tag.dto.TagDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataService {
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final CategoryService categoryService;
    private final IngredientService ingredientService;
    private final TagService tagService;

    @Transactional
    public String initCategoryData() {
        try {
            mongoTemplate.dropCollection("categories");
            InputStream inputStream = new ClassPathResource("json/category/category_data_input.json").getInputStream();
            List<CategoryDTO> categoryDTOs = objectMapper.readValue(inputStream, new TypeReference<List<CategoryDTO>>() {});
            categoryService.createNestedBulk(categoryDTOs);
            return "Khởi tạo lại toàn bộ dữ liệu Category thành công!";
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi khởi tạo dữ liệu: " + e.getMessage());
        }
    }

    @Transactional
    public String initIngredientData() {
        try {
            mongoTemplate.dropCollection("ingredients");
            String filePath = "json/ingredient/ingredient_data_input.json";
            InputStream inputStream = new ClassPathResource(filePath).getInputStream();
            List<IngredientDTO> requests = objectMapper.readValue(inputStream, new TypeReference<List<IngredientDTO>>() {});
            ingredientService.createBulk(requests);
            return "Khởi tạo thành công toàn bộ nguyên liệu từ file!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi init Ingredient: " + e.getMessage());
        }
    }

    @Transactional
    public String initTagData() {
        try {
            mongoTemplate.dropCollection("tags");
            String filePath = "json/tag/tag_data_input.json";
            InputStream inputStream = new ClassPathResource(filePath).getInputStream();
            List<TagDTO> requests = objectMapper.readValue(inputStream, new TypeReference<List<TagDTO>>() {});
            tagService.createBulk(requests);
            return "Khởi tạo thành công toàn bộ thẻ (Tag) từ file!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi khởi tạo Tag: " + e.getMessage());
        }
    }

    public void writeObjectToFile(String subUrl, List<?> object){
        String mainUrl = "meal-service/src/main/resources/json"+subUrl;
        File file = new File(mainUrl);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, object);
            fos.flush();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi: " + e.getMessage());
        }
    }

    public String exportIngredientsToJson() {
        try {
            List<Ingredient> ingredients = mongoTemplate.findAll(Ingredient.class);
            List<IngredientDTO> dtos = ingredients.stream().map(i -> IngredientDTO.builder().id(i.getId()).name(i.getName()).build()).toList();
            writeObjectToFile("/ingredient/ingredient_response.json", dtos);
            return "Xuất thành công " + ingredients.size() + " nguyên liệu.";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất JSON Ingredient: " + e.getMessage());
        }
    }

    public String exportTagToJson() {
        try {
            List<Tag> tags = mongoTemplate.findAll(Tag.class);
            List<TagDTO> dtos = tags.stream().map(t -> TagDTO.builder().id(t.getId()).name(t.getName()).build()).toList();
            writeObjectToFile("/tag/tag_response.json", dtos);
            return "Xuất thành công " + tags.size() + " tags.";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất JSON Tag: " + e.getMessage());
        }
    }

    public String exportCategoryToJson() {
        try {
            List<Category> categories = mongoTemplate.findAll(Category.class);
            List<CategoryDTO> dtos = categories.stream().map( category-> CategoryDTO.builder().id(category.getId()).name(category.getName()).build()).toList();
            writeObjectToFile("/category/category_response.json", dtos);
            return "Xuất thành công " + categories.size() + " danh mục để lấy ID tạo Meal.";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất JSON Category: " + e.getMessage());
        }
    }
}

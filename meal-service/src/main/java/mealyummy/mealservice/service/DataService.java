package mealyummy.mealservice.service;

import lombok.RequiredArgsConstructor;

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

    private final mealyummy.mealservice.service.meal.MealService mealService;



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



    public String exportMealToJson() {
        try {
            List<mealyummy.mealservice.model.entity.food.Meal> meals = mongoTemplate.findAll(mealyummy.mealservice.model.entity.food.Meal.class);
            List<java.util.Map<String, String>> exportData = meals.stream().map(m -> java.util.Map.of("id", m.getId(), "name", m.getName())).toList();
            writeObjectToFile("/meal/meal_response.json", exportData);
            return "Xuất thành công " + meals.size() + " meals.";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất JSON Meal: " + e.getMessage());
        }
    }

    @Transactional
    public String importMealsFromFile(org.springframework.web.multipart.MultipartFile file) {
        try {
            List<mealyummy.mealservice.service.meal.dto.MealRequestDTO> dtos = objectMapper.readValue(file.getInputStream(), new TypeReference<List<mealyummy.mealservice.service.meal.dto.MealRequestDTO>>() {});
            List<mealyummy.mealservice.service.meal.dto.MealResponseDTO> responses = mealService.createBulk(dtos);
            List<java.util.Map<String, String>> exportData = responses.stream().map(r -> java.util.Map.of("id", r.getId(), "name", r.getName())).toList();
            writeObjectToFile("/meal/meal_response.json", exportData);
            return "Import thành công " + dtos.size() + " meals từ file và đã xuất response JSON!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi import meals: " + e.getMessage());
        }
    }

    @Transactional
    public String importMealsFromLocalJson() {
        try {
            InputStream inputStream = new ClassPathResource("json/meal/real-meal-data-input.json").getInputStream();
            List<mealyummy.mealservice.service.meal.dto.MealRequestDTO> dtos = objectMapper.readValue(inputStream, new TypeReference<List<mealyummy.mealservice.service.meal.dto.MealRequestDTO>>() {});
            List<mealyummy.mealservice.service.meal.dto.MealResponseDTO> responses = mealService.createBulk(dtos);
            List<java.util.Map<String, String>> exportData = responses.stream().map(r -> java.util.Map.of("id", r.getId(), "name", r.getName())).toList();
            writeObjectToFile("/meal/meal_response.json", exportData);
            return "Import thành công " + dtos.size() + " meals từ file real-meal-data-input.json và đã xuất response JSON!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi import meals từ local JSON: " + e.getMessage());
        }
    }
}

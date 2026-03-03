package mealyummy.mealservice.service.category;

import mealyummy.mealservice.service.category.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    CategoryDTO create(CategoryDTO request);
    List<CategoryDTO> getAll();
    String changeState(String id);
    CategoryDTO get(String id);
    List<CategoryDTO> createNestedBulk(List<CategoryDTO> requests);
}
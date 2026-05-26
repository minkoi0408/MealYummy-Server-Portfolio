package mealyummy.mealservice.service.category;

import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryDTO create(CategoryDTO request);
    List<CategoryDTO> createBulk(List<CategoryDTO> requests);
    Page<CategoryDTO> getAll(Pageable pageable);
    String changeState(String id);
    CategoryDTO get(String id);
    CategoryDTO update(String id, CategoryDTO request);
    void delete(String id);
    void deleteBulk(List<String> ids);
    List<CategoryDTO> createNestedBulk(List<CategoryDTO> requests);
}
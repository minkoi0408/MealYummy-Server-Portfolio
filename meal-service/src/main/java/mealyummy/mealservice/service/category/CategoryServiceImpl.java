package mealyummy.mealservice.service.category;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.Category;
import mealyummy.mealservice.model.repository.CategoryRepository;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDTO create(CategoryDTO request) {
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            boolean parentExists = categoryRepository.existsByIdAndActiveTrue(request.getParentId());
            if (!parentExists) {
                throw new RuntimeException("Danh mục cha không tồn tại hoặc đã bị ẩn (ID: " + request.getParentId() + ")");
            }
        }
        String name = request.getName()
                .trim().substring(0, 1).toUpperCase() + request.getName().toLowerCase().substring(1).trim();

        boolean nameExists = categoryRepository.existsByName(name);

        if (nameExists) {
            throw new AppException(ErrorCode.CATEGORY_INVALID_NAME);
        }

        Category category = Category.builder()
                .name(name)
                .parentId(request.getParentId())
                .build();
        categoryRepository.save(category);
        return category.convert();
    }

    @Override
    public List<CategoryDTO> getAll() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(Category::convert)
                .collect(Collectors.toList());
    }

    @Override
    public String changeState(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        String msg = "Phân loại " + category.getName().toLowerCase();

        if(Boolean.TRUE.equals(category.getActive())) {
            category.setActive(false);
            msg += " đã được mở hạn chế.";
        }else{
            category.setActive(true);
            msg += " đã được hạn chế.";
        }
        categoryRepository.save(category);
        return msg;
    }

    @Override
    public CategoryDTO get(String id) {
        Category category = categoryRepository.findById(id)
                .filter(Category::getActive)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return mapToDtoWithChildren(category);
    }
    private CategoryDTO mapToDtoWithChildren(Category entity) {
        CategoryDTO dto = entity.convert();
        List<Category> childrenEntities = categoryRepository.findAllByParentIdAndActiveTrue(entity.getId());
        if (!childrenEntities.isEmpty()) {
            List<CategoryDTO> childrenDtos = childrenEntities.stream()
                    .map(this::mapToDtoWithChildren)
                    .collect(Collectors.toList());
            dto.setChildren(childrenDtos);
        }

        return dto;
    }
}
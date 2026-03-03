package mealyummy.mealservice.service.category;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.Category;
import mealyummy.mealservice.model.repository.CategoryRepository;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
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

    @Override
    @Transactional
    public List<CategoryDTO> createNestedBulk(List<CategoryDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_INVALID_NAME);
        }

        return saveCategoriesRecursively(requests, null);
    }

    /**
     * Hàm này cần phát triển thêm
     * Hàm Đệ Quy: Tự động lặp lại chính nó nếu phát hiện có danh mục con
     */
    private List<CategoryDTO> saveCategoriesRecursively(List<CategoryDTO> dtos, String parentId) {
        List<CategoryDTO> result = new ArrayList<>();

        for (CategoryDTO dto : dtos) {
            // 1. Bỏ qua các data rác không có tên
            if (dto.getName() == null || dto.getName().trim().isEmpty()) continue;

            // 2. Chuẩn hóa tên (Viết hoa chữ cái đầu)
            String rawName = dto.getName().trim();
            String formattedName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1).toLowerCase();

            Category category;

            // 3. Kiểm tra xem tên này đã có trong DB chưa (tránh tạo trùng lặp)
            // Giả định bạn có hàm findByName trong CategoryRepository
            Optional<Category> existingCategory = categoryRepository.findByName(formattedName);

            if (existingCategory.isPresent()) {
                category = existingCategory.get(); // Nếu có rồi thì lấy ra dùng luôn
            } else {
                // Nếu chưa có thì tạo mới
                category = Category.builder()
                        .name(formattedName)
                        .parentId(parentId) // Gắn ID của Cha truyền từ tham số vào đây
                        .active(true)
                        .build();
                category = categoryRepository.save(category);
            }

            // Chuyển sang DTO để trả về
            CategoryDTO savedDto = category.convert();

            // 4. BƯỚC ĂN TIỀN (ĐỆ QUY): Nếu có mảng children, tiếp tục đào sâu xuống
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                // Gọi lại chính hàm này, nhưng truyền vào mảng con và ID của Cha vừa tạo!
                List<CategoryDTO> savedChildren = saveCategoriesRecursively(dto.getChildren(), category.getId());
                savedDto.setChildren(savedChildren);
            }

            result.add(savedDto);
        }

        return result;
    }
}
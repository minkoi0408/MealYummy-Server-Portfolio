package mealyummy.mealservice.service.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDTO {
    private String id;
    private String name;
    private String parentId;
    private String createdAt;
    private Boolean active;
    private List<CategoryDTO> children;
}

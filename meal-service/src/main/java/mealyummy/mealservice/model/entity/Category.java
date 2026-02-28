package mealyummy.mealservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.core.util.DateTimeFormat;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categories")
@Builder
public class Category {
    private String id;
    private String name;
    private String parentId;

    @CreatedDate
    private Instant createdAt;

    @Builder.Default
    private Boolean active = true;
    public CategoryDTO convert() {
        return CategoryDTO.builder()
                .id(this.getId())
                .name(this.getName())
                .parentId(this.getParentId())
                .createdAt(DateTimeFormat.formatInstantCustom(this.getCreatedAt()))
                .active(this.getActive())
                .build();
    }
}
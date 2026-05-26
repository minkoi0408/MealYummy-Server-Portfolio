package mealyummy.mealservice.service.category.dto;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.core.view.View;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDTO {
    @Schema(description = "ID tự động sinh của danh mục", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "Tên danh mục", example = "")
    private String name;

    @Schema(description = "Mô tả danh mục", example = "")
    private String description;

    @JsonView(View.Detail.class)
    @Schema(description = "ID của danh mục cha (Để trống null nếu đây là danh mục gốc)", example = "")
    private String parentId;

    @Schema(description = "Tên của danh mục cha", example = "")
    private String parentName;

    @Schema(description = "Thời gian tạo danh mục", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt;

    @Schema(description = "Trạng thái hoạt động", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;

    @Schema(description = "Danh sách các danh mục con trực thuộc", accessMode = Schema.AccessMode.READ_ONLY)
    private List<CategoryDTO> children;
}

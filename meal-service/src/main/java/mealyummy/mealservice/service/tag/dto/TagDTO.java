package mealyummy.mealservice.service.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagDTO {
    @Schema(description = "ID tự động sinh của tag", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "Tên tag", example = "")
    private String name;

    @Schema(description = "Thời gian tạo tag", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt;

    @Schema(description = "Trạng thái hoạt động", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;
}

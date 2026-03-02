package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.tag.dto.TagDTO;
import mealyummy.mealservice.service.tag.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<BaseApiResponse<TagDTO>> addTag(@RequestBody TagDTO request) {
        TagDTO response = tagService.create(request);

        String msg = "Tạo mới tag " + request.getName() + " thành công";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @GetMapping
    public ResponseEntity<BaseApiResponse<List<TagDTO>>> getAllTags() {
        List<TagDTO> response = tagService.getAll();

        String msg = "Lấy danh sách tag thành công "+response.size();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<TagDTO>>> addTagsBulk(@RequestBody List<TagDTO> requests) {

        List<TagDTO> response = tagService.createBulk(requests);

        String msg = "Đã tạo mới thành công " + response.size() + " tag";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }
}
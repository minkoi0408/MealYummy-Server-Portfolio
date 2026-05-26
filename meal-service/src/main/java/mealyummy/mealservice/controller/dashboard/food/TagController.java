package mealyummy.mealservice.controller.dashboard.food;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.tag.TagService;
import mealyummy.mealservice.service.tag.dto.TagDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PreAuthorize("hasAuthority('CREATE_TAG')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<TagDTO>> addTag(@Valid @RequestBody TagDTO request) {
        TagDTO response = tagService.create(request);
        String msg = "Tạo mới tag " + request.getName() + " thành công";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_TAG')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<TagDTO>>> getAllTags(Pageable pageable) {
        Page<TagDTO> response = tagService.getAll(pageable);
        String msg = "Lấy danh sách tag thành công";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_TAG')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<TagDTO>> getTag(@PathVariable String id) {
        TagDTO response = tagService.get(id);
        String msg = "Thông tin tag";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('UPDATE_TAG')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<TagDTO>> updateTag(@PathVariable String id, @Valid @RequestBody TagDTO request) {
        TagDTO response = tagService.update(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật tag thành công", response));
    }

    @PreAuthorize("hasAuthority('UPDATE_TAG')")
    @PutMapping("/{id}/state")
    public ResponseEntity<BaseApiResponse<Void>> changeState(@PathVariable String id) {
        String msg = tagService.changeState(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @PreAuthorize("hasAuthority('DELETE_TAG')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteTag(@PathVariable String id) {
        tagService.delete(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa tag thành công", null));
    }

    @PreAuthorize("hasAuthority('DELETE_TAG')")
    @DeleteMapping("/bulk")
    public ResponseEntity<BaseApiResponse<Void>> deleteTagsBulk(@RequestBody List<String> ids) {
        tagService.deleteBulk(ids);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa danh sách tag thành công", null));
    }

    @PreAuthorize("hasAuthority('CREATE_TAG')")
    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<TagDTO>>> addTagsBulk(@Valid @RequestBody List<TagDTO> requests) {
        List<TagDTO> response = tagService.createBulk(requests);
        String msg = "Đã tạo mới thành công " + response.size() + " tag";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }
}
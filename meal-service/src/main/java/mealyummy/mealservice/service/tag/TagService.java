package mealyummy.mealservice.service.tag;

import mealyummy.mealservice.service.tag.dto.TagDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagService {
    TagDTO create(TagDTO request);
    Page<TagDTO> getAll(Pageable pageable);
    TagDTO get(String id);
    TagDTO update(String id, TagDTO request);
    String changeState(String id);
    void delete(String id);
    void deleteBulk(List<String> ids);
    List<TagDTO> createBulk(List<TagDTO> requests);
}

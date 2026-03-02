package mealyummy.mealservice.service.tag;

import mealyummy.mealservice.service.tag.dto.TagDTO;

import java.util.List;

public interface TagService {
    TagDTO create(TagDTO request);
    List<TagDTO> getAll();
    List<TagDTO> createBulk(List<TagDTO> requests);
}

package mealyummy.mealservice.service.tag;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.food.Tag;
import mealyummy.mealservice.model.repository.TagRepository;
import mealyummy.mealservice.service.tag.dto.TagDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public TagDTO create(TagDTO request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.TAG_INVALID_NAME);
        }

        String trimmedName = request.getName().trim();
        String formattedName = trimmedName.substring(0, 1).toUpperCase() + trimmedName.substring(1).toLowerCase();

        if (tagRepository.existsByNameAndActiveTrue(formattedName)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        Tag tag = Tag.builder()
                .name(formattedName)
                .description(request.getDescription())
                .active(true)
                .build();

        tag = tagRepository.save(tag);
        return tag.convert();
    }

    @Override
    public org.springframework.data.domain.Page<TagDTO> getAll(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Tag> tags = tagRepository.findAll(pageable);
        return tags.map(Tag::convertForMeal);
    }

    @Override
    public TagDTO get(String id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        return tag.convertForMeal();
    }

    @Override
    @Transactional
    public TagDTO update(String id, TagDTO request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String trimmedName = request.getName().trim();
            String formattedName = trimmedName.substring(0, 1).toUpperCase() + trimmedName.substring(1).toLowerCase();

            if (!tag.getName().equals(formattedName) && tagRepository.existsByNameAndActiveTrue(formattedName)) {
                throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
            }
            tag.setName(formattedName);
        }

        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }

        tagRepository.save(tag);
        return tag.convert();
    }

    @Override
    @Transactional
    public String changeState(String id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        String msg = "Tag " + tag.getName();
        if (Boolean.TRUE.equals(tag.getActive())) {
            tag.setActive(false);
            msg += " đã được ẩn.";
        } else {
            tag.setActive(true);
            msg += " đã được hiển thị.";
        }
        tagRepository.save(tag);
        return msg;
    }

    @Override
    @Transactional
    public void delete(String id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        tagRepository.delete(tag);
        removeTagFromMeals(id);
    }

    @Override
    @Transactional
    public void deleteBulk(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Tag> tags = (List<Tag>) tagRepository.findAllById(ids);
        tagRepository.deleteAll(tags);

        for (String id : ids) {
            removeTagFromMeals(id);
        }
    }

    private void removeTagFromMeals(String tagId) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("tags._id").is(tagId));
        
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update();
        update.pull("tags", org.springframework.data.mongodb.core.query.Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(tagId)));
        
        mongoTemplate.updateMulti(query, update, mealyummy.mealservice.model.entity.food.Meal.class);
    }

    @Override
    @Transactional
    public List<TagDTO> createBulk(List<TagDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.TAG_INVALID_NAME);
        }

        List<String> formattedNames = requests.stream()
                .filter(req -> req.getName() != null && !req.getName().trim().isEmpty())
                .map(req -> {
                    String trimmed = req.getName().trim();
                    return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                })
                .distinct()
                .toList();

        List<Tag> existingTags = tagRepository.findByNameInAndActiveTrue(formattedNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Tag> newTags = requests.stream()
                .filter(req -> req.getName() != null && !req.getName().trim().isEmpty())
                .filter(req -> {
                    String trimmed = req.getName().trim();
                    String formatted = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                    return !existingNames.contains(formatted);
                })
                .map(req -> {
                    String trimmed = req.getName().trim();
                    String formatted = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                    return Tag.builder()
                        .name(formatted)
                        .description(req.getDescription())
                        .active(true)
                        .build();
                })
                .toList();

        if (newTags.isEmpty()) {
            return List.of();
        }

        List<Tag> savedTags = tagRepository.saveAll(newTags);

        return savedTags.stream()
                .map(Tag::convert)
                .toList();
    }
}
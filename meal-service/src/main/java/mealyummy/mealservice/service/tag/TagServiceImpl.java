package mealyummy.mealservice.service.tag;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.Tag;
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
                .active(true)
                .build();

        tag = tagRepository.save(tag);
        return tag.convert();
    }

    @Override
    public List<TagDTO> getAll() {
        List<Tag> tags = tagRepository.findAllByActiveTrue();

        return tags.stream()
                .map(Tag::convertForMeal)
                .collect(Collectors.toList());
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

        List<Tag> newTags = formattedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> Tag.builder()
                        .name(name)
                        .active(true)
                        .build())
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
package mealyummy.mealservice.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.User;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.cloudinary.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public String updateAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        try {
            String avatarUrl = cloudinaryService.uploadImage(file);
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            log.info("Cập nhật avatar thành công cho user: {}", username);
            return avatarUrl;
        } catch (IOException e) {
            log.error("Lỗi khi upload avatar cho user {}: {}", username, e.getMessage());
            throw new RuntimeException("Không thể upload ảnh, vui lòng thử lại sau.");
        }
    }


}

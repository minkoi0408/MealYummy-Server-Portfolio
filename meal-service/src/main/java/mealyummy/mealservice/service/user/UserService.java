package mealyummy.mealservice.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.service.cloudinary.CloudinaryService;
import mealyummy.mealservice.service.user.dto.UserResponseDTO;
import mealyummy.mealservice.service.user.dto.UserRoleUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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

    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(UserResponseDTO::fromEntity);
    }

    public UserResponseDTO getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public UserResponseDTO updateUserRole(String id, UserRoleUpdateRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        user.setRole(role);
        userRepository.save(user);

        return UserResponseDTO.fromEntity(user);
    }
}

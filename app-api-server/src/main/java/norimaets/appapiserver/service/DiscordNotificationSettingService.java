package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.response.DiscordNotificationSettingResponse;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscordNotificationSettingService {

    private final UserRepository userRepository;

    public DiscordNotificationSettingResponse getSetting(Long userId) {
        User user = findUser(userId);

        return new DiscordNotificationSettingResponse(
                user.isDiscordNotificationEnabled()
        );
    }

    @Transactional
    public void updateSetting(Long userId, boolean enabled) {
        User user = findUser(userId);
        user.updateDiscordNotificationEnabled(enabled);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
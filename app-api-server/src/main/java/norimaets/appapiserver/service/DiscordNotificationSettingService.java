package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscordNotificationSettingService {

    private final UserRepository userRepository;

    @Transactional
    public void updateSetting(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateDiscordNotificationEnabled(enabled);
    }
}
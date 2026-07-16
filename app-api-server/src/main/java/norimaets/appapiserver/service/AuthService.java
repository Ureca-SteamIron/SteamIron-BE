package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/*
 *
 * 예시용 파일입니다!!
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // TODO: UserRepository, RefreshTokenService 등 주입받아 로그인 로직 구현 예정

    public void dummyLoginLogic() {
        log.info("로그인 로직이 실행될 서비스입니다.");
    }
}

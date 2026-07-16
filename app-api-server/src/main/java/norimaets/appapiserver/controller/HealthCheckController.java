package norimaets.appapiserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 *
 * 예시용 파일입니다!!
 *
 */
@RestController
public class HealthCheckController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong!"); // 브라우저에서 /ping 접속 시 pong! 출력
    }
}

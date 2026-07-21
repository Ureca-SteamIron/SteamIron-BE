package norimaets.appapiserver.common.exception;

import lombok.extern.slf4j.Slf4j;
import norimaets.appapiserver.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 실행 중 발생하는 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    /**
     * @Valid를 통한 입력값 검증 실패 시 발생하는 예외 처리 (주로 컨트롤러 단)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("ValidationException: {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errorMessage));
    }

    /**
     * ResponseStatusException 처리.
     * 서비스/클라이언트에서 throw new ResponseStatusException(HttpStatus.XXX, "메시지") 로 던진
     * 상태코드와 메시지를 그대로 응답에 실어준다.
     * (이게 없으면 아래 handleException으로 떨어져 401/400 등이 전부 500으로 뭉개진다.
     *  → 디스코드 로그인 실패 시 "401 코드 무효"가 "500 서버 에러"로 보이던 원인)
     */
    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<ApiResponse<?>> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException: {} {}", e.getStatusCode(), e.getReason());

        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getReason()));
    }

    /**
     * 그 외 예상치 못한 모든 서버 에러 처리 (최후의 보루)
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("UnhandledException: ", e); // 에러 스택 트레이스 로깅

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}


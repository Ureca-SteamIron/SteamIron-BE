package norimaets.appapiserver.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (공통)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 에러가 발생했습니다."),

    // Auth (인증)
    UNAUTHORIZED_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A002", "토큰이 만료되었습니다."),

    // User (유저)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 유저를 찾을 수 없습니다."),

    // Game (게임)
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "해당 게임을 찾을 수 없습니다."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "G002", "검색어는 2자 이상 입력해주세요."),

    // WishList (찜 목록)
    WISHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "W001", "이미 찜 목록에 추가된 게임입니다."),
    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "W002", "찜 목록에서 해당 게임을 찾을 수 없습니다."),



    ;

    private final HttpStatus status; // HTTP 상태 코드 (200, 400, 404 등)
    private final String code;       // 프론트가 식별할 커스텀 에러 코드
    private final String message;    // 기본 에러 메시지
}

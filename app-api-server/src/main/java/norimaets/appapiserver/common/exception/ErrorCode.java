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
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_ACCOUNT_SETUP_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "계정 설정 인증이 만료되었거나 유효하지 않습니다."),

    // User (유저)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 유저를 찾을 수 없습니다."),
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 사용 중인 로그인 아이디입니다."),
    ACCOUNT_SETUP_ALREADY_COMPLETED(HttpStatus.CONFLICT, "U003", "이미 서비스 계정 설정이 완료되었습니다."),

    // Game (게임)
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "해당 게임을 찾을 수 없습니다."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "G002", "검색어는 2자 이상 입력해주세요."),

    // WishList (찜 목록)
    WISHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "W001", "이미 찜 목록에 추가된 게임입니다."),
    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "W002", "찜 목록에서 해당 게임을 찾을 수 없습니다."),

    // PriceAlert (가격 알림)
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 가격 알림을 찾을 수 없습니다."),
    ALERT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "본인의 알림만 수정/삭제할 수 있습니다."),
    ALERT_ALREADY_EXISTS(HttpStatus.CONFLICT, "P003", "이미 이 게임에 알림을 설정했습니다."),

    // 목표가
    INVALID_DISCOUNT_RATE(
            HttpStatus.BAD_REQUEST,
            "P004",
            "할인율은 1 이상 100 이하여야 합니다."
    ),
    GAME_PRICE_NOT_AVAILABLE(
            HttpStatus.BAD_REQUEST,
            "P005",
            "게임의 정가 정보가 없습니다."
    ),


    // 게임 상세 정보 수동갱신
    STEAM_API_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "G003", "Steam API에서 게임 정보를 가져오지 못했습니다."),


    ;

    private final HttpStatus status; // HTTP 상태 코드 (200, 400, 404 등)
    private final String code;       // 프론트가 식별할 커스텀 에러 코드
    private final String message;    // 기본 에러 메시지
}

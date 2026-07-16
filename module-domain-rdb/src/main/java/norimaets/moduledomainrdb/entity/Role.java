package norimaets.moduledomainrdb.entity;

// 사용자 권한. @Enumerated(STRING)으로 "USER"/"ADMIN" 문자열로 저장된다(VARCHAR(20)).
public enum Role {
    USER,
    ADMIN
}

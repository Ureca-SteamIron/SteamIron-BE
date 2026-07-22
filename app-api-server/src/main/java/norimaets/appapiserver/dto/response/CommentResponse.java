package norimaets.appapiserver.dto.response;

import norimaets.moduledomainrdb.entity.Comment;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record CommentResponse(
        Long id,
        Long userId,
        String nickname,
        String content,
        Integer depth,
        LocalDateTime createdAt,
        boolean deleted,
        List<CommentResponse> children
) {

    // 삭제되지 않은 자식만 재귀적으로 변환 (단, 자식이 있으면 삭제된 부모도 트리 유지를 위해 남겨둠)
    public static CommentResponse from(Comment comment) {
        boolean isDeleted = comment.getDeletedAt() != null;

        List<CommentResponse> childResponses = comment.getChildren().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .nickname(comment.getUser().getNickname())
                .content(isDeleted ? "삭제된 댓글입니다." : comment.getContent())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .deleted(isDeleted)
                .children(childResponses)
                .build();
    }
}
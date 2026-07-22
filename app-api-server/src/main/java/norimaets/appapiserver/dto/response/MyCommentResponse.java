package norimaets.appapiserver.dto.response;

import lombok.Builder;
import norimaets.moduledomainrdb.entity.Comment;

import java.time.LocalDateTime;

@Builder
public record MyCommentResponse(
        Long id,
        Long gameId,
        String gameName,
        String gameHeaderImage,
        String content,
        Integer depth,
        LocalDateTime createdAt
) {
    public static MyCommentResponse from(Comment comment) {
        return MyCommentResponse.builder()
                .id(comment.getId())
                .gameId(comment.getGame().getId())
                .gameName(comment.getGame().getName())
                .gameHeaderImage(comment.getGame().getHeaderImage())
                .content(comment.getContent())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
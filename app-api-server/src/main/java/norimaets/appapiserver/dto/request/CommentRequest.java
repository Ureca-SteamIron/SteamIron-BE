package norimaets.appapiserver.dto.request;

import norimaets.moduledomainrdb.entity.Comment;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CommentRequest {
    // 최상위 댓글 / 대댓글 작성 공용 요청
    public record Create(
            Long parentId,      // null이면 최상위 댓글
            String content
    ) {}

    public record Update(
            String content
    ) {}
}

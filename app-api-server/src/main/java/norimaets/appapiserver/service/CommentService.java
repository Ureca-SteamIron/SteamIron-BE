package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.request.CommentRequest;
import norimaets.appapiserver.dto.response.CommentResponse;
import norimaets.appapiserver.dto.response.MyCommentResponse;
import norimaets.moduledomainrdb.entity.Comment;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.CommentRepository;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    private static final int MAX_DEPTH = 1;

    @Transactional
    public CommentResponse create(Long gameId, Long userId, CommentRequest.Create req) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;
        int depth = 0;

        if (req.parentId() != null) {
            Comment found = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

            if (found.getDeletedAt() != null) {
                throw new CustomException(ErrorCode.PARENT_COMMENT_DELETED);
            }

            parent = found;
            depth = Math.min(parent.getDepth() + 1, MAX_DEPTH);
            if (depth == MAX_DEPTH && parent.getDepth() == MAX_DEPTH) {
                parent = parent.getParent() != null ? parent.getParent() : parent;
            }
        }

        Comment comment = Comment.builder()
                .parent(parent).game(game).user(user).depth(depth).content(req.content())
                .build();

        return CommentResponse.from(commentRepository.save(comment));
    }

    public Page<CommentResponse> getComments(Long gameId, Pageable pageable) {
        return commentRepository.findByGame_IdAndParentIsNull(gameId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public void update(Long commentId, Long userId, CommentRequest.Update req) {
        Comment comment = getOwnedComment(commentId, userId);
        comment.updateContent(req.content());
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = getOwnedComment(commentId, userId);
        if (comment.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        comment.deleteComment();
    }

    @Transactional
    public void adminDelete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        comment.deleteComment();
    }

    private Comment getOwnedComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
        return comment;
    }

    public Page<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {
        return commentRepository.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable)
                .map(MyCommentResponse::from);
    }
}
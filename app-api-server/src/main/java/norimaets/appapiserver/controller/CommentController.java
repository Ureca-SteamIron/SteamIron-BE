package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.request.CommentRequest;
import norimaets.appapiserver.dto.response.CommentResponse;
import norimaets.appapiserver.dto.response.MyCommentResponse;
import norimaets.appapiserver.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/games/{gameId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long gameId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getComments(gameId, pageable));
    }

    @PostMapping("/api/games/{gameId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long gameId,
            @RequestBody @Valid CommentRequest.Create req,
            @AuthenticationPrincipal Long userId) {
        CommentResponse response = commentService.create(gameId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest.Update req,
            @AuthenticationPrincipal Long userId) {
        commentService.update(commentId, userId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/comments/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteComment(@PathVariable Long commentId) {
        commentService.adminDelete(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/comments")
    public ResponseEntity<Page<MyCommentResponse>> getMyComments(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getMyComments(userId, pageable));
    }
}
package com.onion.backend.service;

import com.onion.backend.dto.WriteCommentDto;
import com.onion.backend.entity.*;
import com.onion.backend.exception.ForbiddenException;
import com.onion.backend.exception.RateLimitException;
import com.onion.backend.exception.ResourceNotFoundException;
import com.onion.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class CommentService {
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ReplyCommentRepository replyCommentRepository;

    @Autowired
    public CommentService(BoardRepository boardRepository, ArticleRepository articleRepository, UserRepository userRepository, CommentRepository commentRepository, ReplyCommentRepository replyCommentRepository) {
        this.boardRepository = boardRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
    }

    @Transactional
    public Comment writeComment(Long boardId, Long articleId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails UserDetails = (UserDetails) authentication.getPrincipal();
        if(!this.isCanWriteComment()){
            throw new RateLimitException("comment not written by rate limit");
        }

        Optional<User> author = userRepository.findByUsername(UserDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);

        if(author.isEmpty()){
            throw new ResourceNotFoundException("author not found");
        }
        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        if(article.isEmpty()){
            throw new ResourceNotFoundException("article not found");
        }
        if(article.get().getIsDeleted()){
            throw new ForbiddenException("article is deleted");
        }

        Comment comment = new Comment();
        comment.setArticle(article.get());
        comment.setAuthor(author.get());
        comment.setContent(dto.getContent());
        commentRepository.save(comment);

        return comment;
    }

    @Transactional
    public Comment editComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails UserDetails = (UserDetails) authentication.getPrincipal();
        if(!this.isCanEditComment()){
            throw new RateLimitException("comment not written by rate limit");
        }

        Optional<User> author = userRepository.findByUsername(UserDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);

        if(author.isEmpty()){
            throw new ResourceNotFoundException("author not found");
        }
        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        if(article.isEmpty()){
            throw new ResourceNotFoundException("article not found");
        }
        if(article.get().getIsDeleted()){
            throw new ForbiddenException("article is deleted");
        }

        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isEmpty() || comment.get().getIsDeleted()){
            throw new ResourceNotFoundException("comment not found");
        }
        if(comment.get().getAuthor() != author.get()){
            throw new ForbiddenException("comment author different");
        }
        if(dto.getContent() != null){
            comment.get().setContent(dto.getContent());
        }
        commentRepository.save(comment.get());

        return comment.get();
    }

    public boolean deleteComment(Long boardId, Long articleId, Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails UserDetails = (UserDetails) authentication.getPrincipal();
        if(!this.isCanEditComment()){
            throw new RateLimitException("comment not written by rate limit");
        }

        Optional<User> author = userRepository.findByUsername(UserDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);

        if(author.isEmpty()){
            throw new ResourceNotFoundException("author not found");
        }
        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        if(article.isEmpty()){
            throw new ResourceNotFoundException("article not found");
        }
        if(article.get().getIsDeleted()){
            throw new ForbiddenException("article is deleted");
        }

        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isEmpty() || comment.get().getIsDeleted()){
            throw new ResourceNotFoundException("comment not found");
        }
        if(comment.get().getAuthor() != author.get()){
            throw new ForbiddenException("comment author different");
        }
        comment.get().setIsDeleted(true);
        commentRepository.save(comment.get());

        return true;
    }

    @Transactional
    public ReplyComment writeReplyComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails UserDetails = (UserDetails) authentication.getPrincipal();
        if(!this.isCanWriteComment()){
            throw new RateLimitException("comment not written by rate limit");
        }

        Optional<User> author = userRepository.findByUsername(UserDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        Optional<Comment> comment = commentRepository.findById(commentId);

        if(author.isEmpty()){
            throw new ResourceNotFoundException("author not found");
        }
        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        if(article.isEmpty()){
            throw new ResourceNotFoundException("article not found");
        }
        if(comment.isEmpty()){
            throw new ResourceNotFoundException("comment not found");
        }
        if(article.get().getIsDeleted()){
            throw new ForbiddenException("article is deleted");
        }

        ReplyComment replyComment = new ReplyComment();
        replyComment.setArticle(article.get());
        replyComment.setAuthor(author.get());
        replyComment.setContent(dto.getContent());
        replyComment.setComment(comment.get());
        replyCommentRepository.save(replyComment);

//        Comment comment = new Comment();
//        comment.setArticle(article.get());
//        comment.setAuthor(author.get());
//        comment.setContent(dto.getContent());
//        commentRepository.save(comment);

        return replyComment;
    }

    private boolean isCanWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Comment comment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());

        if(comment == null){
            return true;
        }
        return this.isDifferenceMoreThanOneMinutes(comment.getCreatedDate());
    }

    private boolean isCanEditComment(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Comment comment = commentRepository.findLatestCommentOrderByUpdatedDate(userDetails.getUsername());

        if(comment == null || comment.getUpdatedDate() == null){
            return true;
        }

        return this.isDifferenceMoreThanOneMinutes(comment.getUpdatedDate());
    }

    public boolean isDifferenceMoreThanOneMinutes(LocalDateTime localDateTime){
        LocalDateTime dateAsLocalDate = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDate);

        return Math.abs(duration.toMinutes()) > 1;
    }

    @Async
    protected CompletableFuture<Article> getArticle(Long boardId, Long articleId) {
        Optional<Board> board = boardRepository.findById(boardId);
        if(board.isEmpty()){
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if(article.isEmpty() || article.get().getIsDeleted()){
            throw new ResourceNotFoundException("article not found");
        }
        return CompletableFuture.completedFuture(article.get());
    }

    @Async
    protected CompletableFuture<List<Comment>> getComments(Long ArticleId){
        return CompletableFuture.completedFuture(commentRepository.findByArticleId(ArticleId));
    }

    public CompletableFuture<Article> getArticleWithComment(Long boardId, Long articleId){
        CompletableFuture<Article> articleFuture = this.getArticle(boardId, articleId);
        CompletableFuture<List<Comment>> commentsFuture = this.getComments(articleId);

        return CompletableFuture.allOf(articleFuture, commentsFuture)
                .thenApply(voidResult -> {
            try {
                Article article = articleFuture.get();
                List<Comment> comments = commentsFuture.get();
                article.setComments(comments);
                return article;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        });

    }
}

package com.onion.backend.controller;

import com.onion.backend.dto.EditArticleDto;
import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.service.ArticleService;
import com.onion.backend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/boards")
public class ArticleController {
    private final AuthenticationManager authenticationManager;
    private final ArticleService articleService;
    private final CommentService commentService;

    @Autowired
    public ArticleController(AuthenticationManager authenticationManager, ArticleService articleService, CommentService commentService) {
        this.authenticationManager = authenticationManager;
        this.articleService = articleService;
        this.commentService = commentService;
    }

    @PostMapping({"/{boardId}/articles"})
    public ResponseEntity<Article> writeArticle(@PathVariable Long boardId,
                                                @RequestBody WriteArticleDto dto){
        return ResponseEntity.ok(articleService.writeArticle(boardId, dto));
    }

    @GetMapping({"/{boardId}/articles"})
    public ResponseEntity<List<Article>> getArticle(@PathVariable Long boardId,
                                                    @RequestParam(required = false) Long lastId,
                                                    @RequestParam(required = false) Long firstId){
        if(lastId != null){
            return ResponseEntity.ok(articleService.getOldArticle(boardId, lastId));
        }
        if(firstId != null){
            return ResponseEntity.ok(articleService.getNewArticle(boardId, firstId));
        }
        return ResponseEntity.ok(articleService.firstGetArticle(boardId));
    }

    @PutMapping({"/{boardId}/articles/{articlesId}"})
    public ResponseEntity<Article> editArticle(@PathVariable Long boardId, @PathVariable Long articlesId,
                                                     @RequestBody EditArticleDto dto){
        return ResponseEntity.ok(articleService.editArticle(boardId, articlesId, dto));
    }

    @DeleteMapping({"/{boardId}/articles/{articlesId}"})
    public ResponseEntity<String> deleteArticle(@PathVariable Long boardId, @PathVariable Long articlesId){
        articleService.deleteArticle(boardId, articlesId);
        return ResponseEntity.ok("article is deleted");
    }

    @GetMapping({"/{boardId}/articles/{articlesId}"})
    public ResponseEntity<Article> getArticleWithComment(@PathVariable Long boardId, @PathVariable Long articlesId){
        CompletableFuture<Article> article = commentService.getArticleWithComment(boardId, articlesId);
        return ResponseEntity.ok(article.resultNow());
    }
}

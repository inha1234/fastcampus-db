package com.onion.backend.repository;

import com.onion.backend.entity.Comment;
import com.onion.backend.entity.ReplyComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplyCommentRepository extends JpaRepository<ReplyComment, Long> {
    @Query("SELECT c FROM ReplyComment c WHERE c.article.id = :articleId and c.isDeleted = false ")
    List<ReplyComment> findByArticleId(@Param("articleId") Long username);
}
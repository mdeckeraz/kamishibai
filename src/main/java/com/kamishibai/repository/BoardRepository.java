package com.kamishibai.repository;

import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    @Query("SELECT b FROM Board b WHERE b.owner = :owner OR :user MEMBER OF b.sharedWith")
    List<Board> findByOwnerOrSharedWith(@Param("owner") Account owner, @Param("user") Account user);
}

package com.kamishibai.repository;

import com.kamishibai.model.Board;
import com.kamishibai.model.Card;
import com.kamishibai.model.CardState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByBoardIdOrderByPosition(Long boardId);
    Optional<Card> findByIdAndBoard(Long id, Board board);
    List<Card> findByStateAndResetTimeLessThanEqual(CardState state, LocalTime resetTime);
}

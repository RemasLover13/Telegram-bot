package com.remaslover.telegrambotaq.repository;

import com.remaslover.telegrambotaq.entity.Joke;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JokeRepository extends JpaRepository<Joke, Long> {
}

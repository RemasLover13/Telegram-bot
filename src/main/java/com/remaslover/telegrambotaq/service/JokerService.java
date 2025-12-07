package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.exception.JokeNotFoundException;
import com.remaslover.telegrambotaq.util.JokesParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JokerService {

    @Transactional
    public String getJoke() {
        String jokeFromSites = JokesParser.getJokeFromSites();
        if (jokeFromSites != null && !jokeFromSites.isEmpty()) {
            return jokeFromSites;
        }
        throw new JokeNotFoundException("Не удалось получить шутку");
    }
}

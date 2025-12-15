package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.entity.User;
import com.remaslover.telegrambotaq.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    @PersistenceContext
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public UserService(UserRepository userRepository, EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Message message) {
        long chatId = message.getChatId();
        Optional<com.remaslover.telegrambotaq.entity.User> user = userRepository.findById(chatId);
        return user.orElse(null);
    }

    public Optional<User> getUserById(long chatId) {
        return userRepository.findById(chatId);
    }

    @Transactional
    public boolean deleteUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> user = userRepository.findById(chatId);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            log.info("User deleted: {}", chatId);
            return true;
        } else {
            log.warn("User not found for deletion: {}", chatId);
            return false;
        }
    }

    public void registerUser(Message message) {
        transactionTemplate.execute(status -> {
            long chatId = message.getChatId();

            User user = userRepository.findById(chatId)
                    .orElseGet(() -> {
                        var chat = message.getChat();
                        User newUser = new User();
                        newUser.setId(chatId);
                        newUser.setFirstName(chat.getFirstName());
                        newUser.setLastName(chat.getLastName());
                        newUser.setUserName(chat.getUserName());
                        newUser.setRegisteredAt(new Date());

                        return userRepository.save(newUser);
                    });

            entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);

            if (!Objects.equals(user.getFirstName(), message.getChat().getFirstName()) ||
                !Objects.equals(user.getLastName(), message.getChat().getLastName()) ||
                !Objects.equals(user.getUserName(), message.getChat().getUserName())) {

                user.setFirstName(message.getChat().getFirstName());
                user.setLastName(message.getChat().getLastName());
                user.setUserName(message.getChat().getUserName());
                user.setRegisteredAt(new Date());
            }

            userRepository.save(user);
            log.debug("User processed: {}", user.getId());

            return null;
        });
    }

    public String formatUserData(User user) {
        return """
                👤 Ваши данные:
                            
                • ID: %d
                • Имя: %s
                • Фамилия: %s
                • Username: @%s
                • Зарегистрирован: %s
                """.formatted(
                user.getId(),
                user.getFirstName() != null ? user.getFirstName() : "Не указано",
                user.getLastName() != null ? user.getLastName() : "Не указано",
                user.getUserName() != null ? user.getUserName() : "Не указано",
                user.getRegisteredAt().toString()
        );
    }
}

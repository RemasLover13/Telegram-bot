package com.remaslover.telegrambotaq.repository;

import com.remaslover.telegrambotaq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}

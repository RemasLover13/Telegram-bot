package com.remaslover.telegrambotaq.entity;


import jakarta.persistence.*;

public class Joke {

    private Long id;

    private String content;

    public Joke(final String content) {
        this.content = content;
    }

    public Joke() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Joke{" +
               "id=" + id +
               ", content='" + content + '\'' +
               '}';
    }
}

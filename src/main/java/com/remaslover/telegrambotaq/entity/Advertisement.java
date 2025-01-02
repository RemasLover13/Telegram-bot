package com.remaslover.telegrambotaq.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ads")
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Advertisement(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Advertisement() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Advertisement{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }
}

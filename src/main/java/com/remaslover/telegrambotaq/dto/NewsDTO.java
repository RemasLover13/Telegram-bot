package com.remaslover.telegrambotaq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class NewsDTO {
    @JsonProperty("status")
    private String status;
    @JsonProperty("totalResults")
    private Integer totalResults;
    @JsonProperty("articles")
    private ArticleDTO[] articles;

    public NewsDTO(String status, Integer totalResults, ArticleDTO[] articles) {
        this.status = status;
        this.totalResults = totalResults;
        this.articles = articles;
    }

    public NewsDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public ArticleDTO[] getArticles() {
        return articles;
    }

    public void setArticles(ArticleDTO[] articles) {
        this.articles = articles;
    }

    @Override
    public String toString() {
        return "NewsDTO{" +
               "status='" + status + '\'' +
               ", totalResults=" + totalResults +
               ", articles=" + Arrays.toString(articles) +
               '}';
    }
}

package com.legalai.dto;

public class SearchFeedbackRequest {
    private Long searchLogId;
    private String articleId;
    private Integer isHelpful;
    private String userComment;

    public Long getSearchLogId() { return searchLogId; }
    public void setSearchLogId(Long searchLogId) { this.searchLogId = searchLogId; }
    public String getArticleId() { return articleId; }
    public void setArticleId(String articleId) { this.articleId = articleId; }
    public Integer getIsHelpful() { return isHelpful; }
    public void setIsHelpful(Integer isHelpful) { this.isHelpful = isHelpful; }
    public String getUserComment() { return userComment; }
    public void setUserComment(String userComment) { this.userComment = userComment; }
}
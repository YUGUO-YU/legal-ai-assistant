package com.legalai.dto;

import java.util.List;

public class KnowledgeBaseListResponse {
    private List<KnowledgeBase> items;
    private Integer total;
    private Integer page;
    private Integer pageSize;

    public List<KnowledgeBase> getItems() { return items; }
    public void setItems(List<KnowledgeBase> items) { this.items = items; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public static class KnowledgeBase {
        private Long id;
        private String name;
        private String description;
        private String type;
        private Boolean isPublic;
        private Integer docCount;
        private Integer chunkCount;
        private String size;
        private String owner;
        private String updateTime;
        private Integer parseStatus;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
        public Integer getDocCount() { return docCount; }
        public void setDocCount(Integer docCount) { this.docCount = docCount; }
        public Integer getChunkCount() { return chunkCount; }
        public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
        public Integer getParseStatus() { return parseStatus; }
        public void setParseStatus(Integer parseStatus) { this.parseStatus = parseStatus; }
    }
}

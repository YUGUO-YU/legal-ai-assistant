package com.legalai.service;

import com.legalai.dto.KnowledgeBaseListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private final AtomicLong idGenerator = new AtomicLong(5);
    private final ConcurrentHashMap<Long, KnowledgeBaseListResponse.KnowledgeBase> kbStore = new ConcurrentHashMap<>();

    public KnowledgeBaseService() {
        initMockData();
    }

    private void initMockData() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        addKb(createKb(1L, "劳动法法规库", "劳动法律法规及相关案例汇总", "public", true, 156, 2340, "128MB", "系统管理员", today, 2));
        addKb(createKb(2L, "合同纠纷案例", "各类合同纠纷案例集", "private", false, 89, 1560, "96MB", "张三", today, 2));
        addKb(createKb(3L, "知识产权法规", "知识产权相关法律法规", "public", true, 234, 3200, "156MB", "系统管理员", today, 2));
        addKb(createKb(4L, "公司法务文档", "公司内部法务文档", "private", false, 45, 680, "45MB", "李四", today, 1));
    }

    private void addKb(KnowledgeBaseListResponse.KnowledgeBase kb) {
        kbStore.put(kb.getId(), kb);
    }

    private KnowledgeBaseListResponse.KnowledgeBase createKb(Long id, String name, String desc, String type, boolean isPublic,
                                                           int docCount, int chunkCount, String size, String owner, String updateTime, int parseStatus) {
        KnowledgeBaseListResponse.KnowledgeBase kb = new KnowledgeBaseListResponse.KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(desc);
        kb.setType(type);
        kb.setIsPublic(isPublic);
        kb.setDocCount(docCount);
        kb.setChunkCount(chunkCount);
        kb.setSize(size);
        kb.setOwner(owner);
        kb.setUpdateTime(updateTime);
        kb.setParseStatus(parseStatus);
        return kb;
    }

    public KnowledgeBaseListResponse listKnowledgeBases(String keyword, int page, int pageSize) {
        log.info("Listing knowledge bases: keyword={}, page={}, pageSize={}", keyword, page, pageSize);

        List<KnowledgeBaseListResponse.KnowledgeBase> allKbs = new ArrayList<>(kbStore.values());

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            allKbs = allKbs.stream()
                .filter(kb -> kb.getName().toLowerCase().contains(kw) ||
                             (kb.getDescription() != null && kb.getDescription().toLowerCase().contains(kw)))
                .toList();
        }

        int total = allKbs.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<KnowledgeBaseListResponse.KnowledgeBase> pageItems = start < total
            ? allKbs.subList(start, end)
            : new ArrayList<>();

        KnowledgeBaseListResponse response = new KnowledgeBaseListResponse();
        response.setItems(pageItems);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    public KnowledgeBaseListResponse.KnowledgeBase createKnowledgeBase(String name, String description, boolean isPublic) {
        log.info("Creating knowledge base: name={}, isPublic={}", name, isPublic);

        long id = idGenerator.getAndIncrement();
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        KnowledgeBaseListResponse.KnowledgeBase kb = createKb(
            id, name, description, isPublic ? "public" : "private",
            isPublic, 0, 0, "0MB",
            "当前用户", today, 0
        );
        kb.setIsPublic(isPublic);

        addKb(kb);
        return kb;
    }

    public boolean deleteKnowledgeBase(Long id) {
        log.info("Deleting knowledge base: id={}", id);
        return kbStore.remove(id) != null;
    }

    public String uploadDocument(Long kbId, String fileName) {
        log.info("Uploading document to kb: kbId={}, fileName={}", kbId, fileName);

        KnowledgeBaseListResponse.KnowledgeBase kb = kbStore.get(kbId);
        if (kb == null) {
            return "知识库不存在";
        }

        kb.setDocCount(kb.getDocCount() + 1);
        kb.setChunkCount(kb.getChunkCount() + (int)(Math.random() * 50) + 10);
        kb.setSize(String.format("%.0fMB", kb.getDocCount() * 2.5));
        kb.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE));

        return "文档上传成功";
    }
}

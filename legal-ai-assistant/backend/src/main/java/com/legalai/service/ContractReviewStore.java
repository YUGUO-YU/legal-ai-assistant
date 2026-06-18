package com.legalai.service;

import com.legalai.dto.ContractReviewResponse;
import com.legalai.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContractReviewStore {
    private final Map<String, ContractReviewResponse> store = new ConcurrentHashMap<>();

    public String save(ContractReviewResponse response) {
        if (response == null) {
            return null;
        }
        String uuid = IdGenerator.generateUuid();
        response.setReviewUuid(uuid);
        store.put(uuid, response);
        return uuid;
    }

    public ContractReviewResponse get(String uuid) {
        return uuid == null ? null : store.get(uuid);
    }

    public List<ContractReviewResponse> listRecent(int limit) {
        List<ContractReviewResponse> values = new ArrayList<>(store.values());
        values.sort(Comparator.comparing(
            r -> r.getReviewUuid() == null ? "" : r.getReviewUuid(),
            Comparator.reverseOrder()
        ));
        int size = Math.min(limit, values.size());
        return Collections.unmodifiableList(values.subList(0, size));
    }

    public void remove(String uuid) {
        if (uuid != null) {
            store.remove(uuid);
        }
    }
}

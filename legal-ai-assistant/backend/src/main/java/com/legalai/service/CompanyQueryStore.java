package com.legalai.service;

import com.legalai.dto.CompanyQueryResponse;
import com.legalai.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CompanyQueryStore {
    private final Map<String, CompanyQueryResponse> store = new ConcurrentHashMap<>();

    public String save(CompanyQueryResponse response) {
        if (response == null) {
            return null;
        }
        String uuid = IdGenerator.generateUUID();
        response.setQueryUuid(uuid);
        store.put(uuid, response);
        return uuid;
    }

    public CompanyQueryResponse get(String uuid) {
        return uuid == null ? null : store.get(uuid);
    }

    public List<CompanyQueryResponse> listRecent(int limit) {
        List<CompanyQueryResponse> values = new ArrayList<>(store.values());
        values.sort(Comparator.comparing(
            r -> r.getQueryUuid() == null ? "" : r.getQueryUuid(),
            Comparator.reverseOrder()
        ));
        int size = Math.min(limit, values.size());
        return Collections.unmodifiableList(values.subList(0, size));
    }
}

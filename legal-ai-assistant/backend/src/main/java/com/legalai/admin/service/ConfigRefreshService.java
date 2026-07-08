package com.legalai.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ConfigRefreshService.class);

    private final Map<String, Runnable> observers = new ConcurrentHashMap<>();

    public void registerObserver(String key, Runnable callback) {
        observers.put(key, callback);
        log.info("注册配置观察者: key={}", key);
    }

    public void unregisterObserver(String key) {
        observers.remove(key);
        log.info("注销配置观察者: key={}", key);
    }

    public void notifyChange(String key) {
        Runnable callback = observers.get(key);
        if (callback != null) {
            log.info("触发配置变更通知: key={}", key);
            try {
                callback.run();
            } catch (Exception e) {
                log.error("配置变更回调执行失败: key={}, error={}", key, e.getMessage());
            }
        }
    }

    public void notifyAll() {
        log.info("触发全部配置变更通知, observer数量={}", observers.size());
        observers.values().forEach(callback -> {
            try {
                callback.run();
            } catch (Exception e) {
                log.error("配置变更回调执行失败: error={}", e.getMessage());
            }
        });
    }

    public int getObserverCount() {
        return observers.size();
    }
}

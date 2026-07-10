package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.LawFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/law-favorite")
@CrossOrigin
@Tag(name = "法规收藏", description = "用户法规收藏相关接口")
public class LawFavoriteController {

    private final LawFavoriteService lawFavoriteService;

    public LawFavoriteController(LawFavoriteService lawFavoriteService) {
        this.lawFavoriteService = lawFavoriteService;
    }

    @PostMapping("/add")
    @Operation(summary = "添加收藏", description = "添加法规到用户收藏夹")
    public ApiResponse<Void> addFavorite(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @RequestBody Map<String, String> request) {
        String token = extractToken(authHeader);
        String lawUuid = request.get("lawUuid");
        String lawTitle = request.get("lawTitle");
        if (lawUuid == null || lawUuid.isBlank()) {
            return ApiResponse.error(400, "lawUuid 不能为空");
        }
        lawFavoriteService.addFavorite(token, lawUuid, lawTitle);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/remove/{lawUuid}")
    @Operation(summary = "取消收藏", description = "从用户收藏夹移除法规")
    public ApiResponse<Void> removeFavorite(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @PathVariable String lawUuid) {
        String token = extractToken(authHeader);
        lawFavoriteService.removeFavorite(token, lawUuid);
        return ApiResponse.success(null);
    }

    @GetMapping("/list")
    @Operation(summary = "获取收藏列表", description = "获取当前用户的法规收藏列表")
    public ApiResponse<List<Map<String, Object>>> listFavorites(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        List<Map<String, Object>> favorites = lawFavoriteService.listFavorites(token);
        return ApiResponse.success(favorites);
    }

    @GetMapping("/check/{lawUuid}")
    @Operation(summary = "检查收藏状态", description = "检查指定法规是否已被当前用户收藏")
    public ApiResponse<Boolean> checkFavorite(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                              @PathVariable String lawUuid) {
        String token = extractToken(authHeader);
        boolean isFavorited = lawFavoriteService.isFavorited(token, lawUuid);
        return ApiResponse.success(isFavorited);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}

package com.legalai.controller.v2;

import com.legalai.dto.v2.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin
@Tag(name = "API v2", description = "新一代API接口，支持高级过滤、排序和字段选择")
public class BaseV2Controller {

    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查API v2服务是否正常运行")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }

    @GetMapping("/ping")
    @Operation(summary = "延迟测试", description = "测试API响应延迟")
    public ApiResponse<Long> ping() {
        return ApiResponse.success(System.currentTimeMillis());
    }

    public static class FieldSelector {
        private List<String> fields;

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }
    }
}

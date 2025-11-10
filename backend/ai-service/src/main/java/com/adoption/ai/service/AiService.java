package com.adoption.ai.service;

import com.adoption.ai.model.AiTask;
import com.adoption.ai.repository.AiTaskMapper;
import com.adoption.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务
 *
 * 功能：
 * 1. 内容审核（违规检测）- CONTENT_MOD
 * 2. 内容总结（生成摘要）- SUMMARY
 * 3. 状态提取（从养宠日常中提取健康状态）- STATE_EXTRACT
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 内容审核（违规检测）
     *
     * @param postId 帖子ID
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 是否违规（true-违规，false-正常）
     */
    public ApiResponse<Boolean> checkContentModeration(Long postId, String title, String content) {
        try {
            // TODO: 调用真实的 AI 接口进行内容审核
            // 暂时模拟数据
            log.info("TODO: 调用 AI 接口进行内容审核 - postId: {}, title: {}", postId, title);

            // 模拟 AI 分析结果
            boolean isFlagged = simulateContentModeration(title, content);
            BigDecimal confidence = new BigDecimal("0.85");

            // 保存任务记录（容错处理：数据库保存失败不影响返回结果）
            try {
                AiTask task = new AiTask();
                task.setType("CONTENT_MOD");
                task.setSourceId(postId);
                task.setSourceType("POST");
                task.setStatus("DONE");

                Map<String, Object> result = new HashMap<>();
                result.put("flagged", isFlagged);
                result.put("reason", isFlagged ? "检测到可能违规内容" : "内容正常");
                task.setResultJson(objectMapper.writeValueAsString(result));
                task.setConfidence(confidence);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());

                aiTaskMapper.insert(task);
                log.debug("AI 任务记录已保存: postId={}, type=CONTENT_MOD", postId);
            } catch (Exception e) {
                // 数据库保存失败不影响 AI 分析结果返回
                log.warn("保存 AI 任务记录失败（不影响结果返回）: postId={}, error={}", postId, e.getMessage(), e);
            }

            return ApiResponse.success(isFlagged);
        } catch (Exception e) {
            log.error("内容审核失败: postId={}, error={}", postId, e.getMessage(), e);
            return ApiResponse.error(500, "内容审核失败: " + e.getMessage());
        }
    }

    /**
     * 生成内容总结
     *
     * @param postId 帖子ID
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 内容总结
     */
    public ApiResponse<String> generateSummary(Long postId, String title, String content) {
        try {
            // TODO: 调用真实的 AI 接口生成内容总结
            // 暂时模拟数据
            log.info("TODO: 调用 AI 接口生成内容总结 - postId: {}, title: {}", postId, title);

            // 模拟 AI 生成总结
            String summary = simulateSummary(title, content);
            BigDecimal confidence = new BigDecimal("0.90");

            // 保存任务记录（容错处理：数据库保存失败不影响返回结果）
            try {
                AiTask task = new AiTask();
                task.setType("SUMMARY");
                task.setSourceId(postId);
                task.setSourceType("POST");
                task.setStatus("DONE");

                Map<String, Object> result = new HashMap<>();
                result.put("summary", summary);
                task.setResultJson(objectMapper.writeValueAsString(result));
                task.setConfidence(confidence);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());

                aiTaskMapper.insert(task);
                log.debug("AI 任务记录已保存: postId={}, type=SUMMARY", postId);
            } catch (Exception e) {
                // 数据库保存失败不影响 AI 分析结果返回
                log.warn("保存 AI 任务记录失败（不影响结果返回）: postId={}, error={}", postId, e.getMessage(), e);
            }

            return ApiResponse.success(summary);
        } catch (Exception e) {
            log.error("生成内容总结失败: postId={}, error={}", postId, e.getMessage(), e);
            return ApiResponse.error(500, "生成内容总结失败: " + e.getMessage());
        }
    }

    /**
     * 提取宠物健康状态
     *
     * @param postId 帖子ID
     * @param content 帖子内容
     * @return 提取的健康状态信息（JSON格式）
     */
    public ApiResponse<Map<String, Object>> extractPetState(Long postId, String content) {
        try {
            // TODO: 调用真实的 AI 接口提取宠物健康状态
            // 暂时模拟数据
            log.info("TODO: 调用 AI 接口提取宠物健康状态 - postId: {}", postId);

            // 模拟 AI 提取状态
            Map<String, Object> state = simulateStateExtraction(content);
            BigDecimal confidence = new BigDecimal("0.75");

            // 保存任务记录（容错处理：数据库保存失败不影响返回结果）
            try {
                AiTask task = new AiTask();
                task.setType("STATE_EXTRACT");
                task.setSourceId(postId);
                task.setSourceType("POST");
                task.setStatus("DONE");
                task.setResultJson(objectMapper.writeValueAsString(state));
                task.setConfidence(confidence);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());

                aiTaskMapper.insert(task);
                log.debug("AI 任务记录已保存: postId={}, type=STATE_EXTRACT", postId);
            } catch (Exception e) {
                // 数据库保存失败不影响 AI 分析结果返回
                log.warn("保存 AI 任务记录失败（不影响结果返回）: postId={}, error={}", postId, e.getMessage(), e);
            }

            return ApiResponse.success(state);
        } catch (Exception e) {
            log.error("提取宠物健康状态失败: postId={}, error={}", postId, e.getMessage(), e);
            return ApiResponse.error(500, "提取宠物健康状态失败: " + e.getMessage());
        }
    }

    // ========== 模拟方法（TODO: 替换为真实 AI 调用） ==========

    /**
     * 模拟内容审核
     */
    private boolean simulateContentModeration(String title, String content) {
        // 简单的关键词检测（模拟）
        String text = (title + " " + (content != null ? content : "")).toLowerCase();
        String[] badWords = {"广告", "推广", "色情", "暴力", "政治"};
        for (String word : badWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 模拟生成总结
     */
    private String simulateSummary(String title, String content) {
        if (content == null || content.length() < 50) {
            return title + " - 简要分享";
        }
        // 简单截取前100字符作为总结
        return content.substring(0, Math.min(100, content.length())) + "...";
    }

    /**
     * 模拟状态提取
     *
     * 返回字段需匹配 PetHealth 模型：
     * - weight: BigDecimal (体重，单位：kg)
     * - vaccine: String (JSON字符串，疫苗信息)
     * - note: String (备注信息)
     */
    private Map<String, Object> simulateStateExtraction(String content) {
        Map<String, Object> state = new HashMap<>();
        // TODO: 调用真实 AI 接口提取健康状态
        // 模拟提取的健康状态，字段需匹配 PetHealth
        state.put("weight", new java.math.BigDecimal("5.5")); // 体重（kg）
        state.put("vaccine", "[]"); // 疫苗信息（JSON字符串，暂时为空数组）
        state.put("note", "从帖子内容中提取的健康状态信息：" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content));
        return state;
    }
}


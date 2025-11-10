package com.adoption.ai.service;

import com.adoption.ai.model.AiTask;
import com.adoption.ai.repository.AiTaskMapper;
import com.adoption.common.api.ApiResponse;
import com.adoption.ai.config.DashScopeConfig;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // DashScope Generation实例
    private final Generation generation = new Generation();

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
            log.info("调用 AI 接口进行内容审核 - postId: {}, title: {}", postId, title);

            // 模拟 AI 分析结果
            boolean isFlagged = aiContentModeration(title, content);
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
            log.info("调用 AI 接口生成内容总结 - postId: {}, title: {}", postId, title);

            // 模拟 AI 生成总结
            String summary = aiSummary(title, content);
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
            log.info("调用 AI 接口提取宠物健康状态 - postId: {}", postId);

            // 模拟 AI 提取状态
            Map<String, Object> state = aiStateExtraction(content);
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

    // ========== 调用Qwen大模型 ==========

    /**
     * 内容审核（调用DashScope Qwen模型）
     * 
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 是否违规（true-违规，false-正常）
     */
    private boolean aiContentModeration(String title, String content) {
        // 构造提示词
        String prompt = String.format("请对以下内容进行内容审核，判断是否存在违规信息（如广告、色情、暴力、政治敏感等）：\n\n标题：%s\n\n内容：%s\n\n请仅回答\"合规\"或\"违规\"，不要添加其他内容。", 
                title != null ? title : "", 
                content != null ? content : "");
        
        try {
            // 创建生成参数
            GenerationParam param = GenerationParam.builder()
                    .model("qwen-plus")  // 使用Qwen-Plus模型
                    .prompt(prompt)
                    .apiKey(dashScopeConfig.getApiKey())
                    .build();
            
            // 调用模型
            GenerationResult result = generation.call(param);
            
            // 解析结果
            String textResult = result.getOutput().getText();
            log.info("内容审核结果: {}", textResult);
            
            // 判断是否违规
            return textResult.contains("违规");
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用DashScope内容审核接口失败: {}", e.getMessage(), e);
            // 出错时回退到关键词检测
            return fallbackContentModeration(title, content);
        } catch (Exception e) {
            log.error("内容审核过程中发生未知错误: {}", e.getMessage(), e);
            // 出错时回退到关键词检测
            return fallbackContentModeration(title, content);
        }
    }
    
    /**
     * 回退的内容审核方法（关键词检测）
     * 
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 是否违规（true-违规，false-正常）
     */
    private boolean fallbackContentModeration(String title, String content) {
        log.warn("回退到关键词检测进行内容审核");
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
     * 生成内容总结（调用DashScope Qwen模型）
     * 
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 内容总结
     */
    private String aiSummary(String title, String content) {
        // 构造提示词
        String prompt = String.format("请为以下内容生成一个简洁的摘要（100字以内）：\n\n标题：%s\n\n内容：%s", 
                title != null ? title : "", 
                content != null ? content : "");
        
        try {
            // 创建生成参数
            GenerationParam param = GenerationParam.builder()
                    .model("qwen-plus")  // 使用Qwen-Plus模型
                    .prompt(prompt)
                    .apiKey(dashScopeConfig.getApiKey())
                    .build();
            
            // 调用模型
            GenerationResult result = generation.call(param);
            
            // 解析结果
            String summary = result.getOutput().getText();
            log.info("内容总结生成结果: {}", summary);
            
            // 确保摘要不超过100字
            if (summary.length() > 100) {
                summary = summary.substring(0, 100) + "...";
            }
            
            return summary;
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用DashScope生成摘要接口失败: {}", e.getMessage(), e);
            // 出错时回退到简单截取
            return fallbackSummary(title, content);
        } catch (Exception e) {
            log.error("生成摘要过程中发生未知错误: {}", e.getMessage(), e);
            // 出错时回退到简单截取
            return fallbackSummary(title, content);
        }
    }
    
    /**
     * 回退的摘要生成方法（简单截取）
     * 
     * @param title 帖子标题
     * @param content 帖子内容
     * @return 内容总结
     */
    private String fallbackSummary(String title, String content) {
        log.warn("回退到简单截取方式生成内容摘要");
        if (content == null || content.length() < 50) {
            return title + " - 简要分享";
        }
        // 简单截取前100字符作为总结
        return content.substring(0, Math.min(100, content.length())) + "...";
    }

    /**
     * 状态提取（调用DashScope Qwen模型）
     *
     * 返回字段需匹配 PetHealth 模型：
     * - weight: BigDecimal (体重，单位：kg)
     * - vaccine: String (JSON字符串，疫苗信息)
     * - note: String (备注信息)
     * 
     * @param content 帖子内容
     * @return 提取的健康状态信息
     */
    private Map<String, Object> aiStateExtraction(String content) {
        // 构造提示词
        String prompt = String.format("请从以下养宠日常内容中提取宠物的健康状态信息，按照指定格式返回JSON：\n\n内容：%s\n\n请严格按照以下JSON格式返回结果，不要添加其他内容：\n{\n  \"weight\": 5.5,\n  \"vaccine\": \"[\\\"狂犬疫苗\\\", \\\"猫三联\\\"]\",\n  \"note\": \"提取的健康状态备注信息\"\n}", 
                content != null ? content : "");
        
        try {
            // 创建生成参数
            GenerationParam param = GenerationParam.builder()
                    .model("qwen-plus")  // 使用Qwen-Plus模型
                    .prompt(prompt)
                    .apiKey(dashScopeConfig.getApiKey())
                    .build();
            
            // 调用模型
            GenerationResult result = generation.call(param);
            
            // 解析结果
            String jsonResult = result.getOutput().getText();
            log.info("状态提取结果: {}", jsonResult);
            
            // 使用正则表达式提取JSON部分（防止模型返回额外文本）
            Pattern pattern = Pattern.compile("\\{[^}]+(?:\\{[^}]+\\})*[^}]*\\}");
            Matcher matcher = pattern.matcher(jsonResult);
            if (matcher.find()) {
                jsonResult = matcher.group(0);
            }
            
            // 解析JSON为Map
            Map<String, Object> state = JsonUtils.fromJson(jsonResult, Map.class);
            if (state != null) {
                return state;
            } else {
                log.warn("解析状态提取结果失败，回退到默认值");
                return fallbackStateExtraction(content);
            }
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用DashScope状态提取接口失败: {}", e.getMessage(), e);
            // 出错时回退到默认值
            return fallbackStateExtraction(content);
        } catch (Exception e) {
            log.error("状态提取过程中发生未知错误: {}", e.getMessage(), e);
            // 出错时回退到默认值
            return fallbackStateExtraction(content);
        }
    }
    
    /**
     * 回退的状态提取方法（返回默认值）
     * 
     * @param content 帖子内容
     * @return 提取的健康状态信息
     */
    private Map<String, Object> fallbackStateExtraction(String content) {
        log.warn("回退到默认值进行状态提取");
        Map<String, Object> state = new HashMap<>();
        state.put("weight", new java.math.BigDecimal("5.5")); // 体重（kg）
        state.put("vaccine", "[]"); // 疫苗信息（JSON字符串，暂时为空数组）
        state.put("note", "从帖子内容中提取的健康状态信息：" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content));
        return state;
    }
}
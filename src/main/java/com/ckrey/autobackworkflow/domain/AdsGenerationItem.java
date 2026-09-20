package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * 每个台词分段的音频生成子任务
 * @TableName ads_generation_item
 */
@Data
public class AdsGenerationItem {
    /**
     * 主键
     */
    private Long id;

    /**
     * 批量任务 ID
     */
    private Long taskId;

    /**
     * 任务内顺序
     */
    private Integer itemNo;

    /**
     * 分段 ID
     */
    private Long segmentId;

    /**
     * 生成时分段版本
     */
    private Integer segmentVersion;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * 模型 ID
     */
    private Long modelId;

    /**
     * Provider 编码快照
     */
    private String providerCodeSnapshot;

    /**
     * 模型编码快照
     */
    private String modelCodeSnapshot;

    /**
     * 供应商音色 ID
     */
    private String voiceId;

    /**
     * 本次送入 TTS 的文本快照
     */
    private String textSnapshot;

    /**
     * 本次演绎与供应商参数
     */
    private Object parametersJson;

    /**
     * 分段幂等请求 SHA-256
     */
    private String requestHash;

    /**
     * 生成项状态
     */
    private String status;

    /**
     * 已尝试次数
     */
    private Integer attemptCount;

    /**
     * 最大尝试次数
     */
    private Integer maxAttempts;

    /**
     * 下次重试时间
     */
    private Date nextRetryAt;

    /**
     * 供应商异步任务 ID
     */
    private String externalTaskId;

    /**
     * 成功音频资产 ID
     */
    private Long audioAssetId;

    /**
     * 参数降级及处理警告
     */
    private Object warningsJson;

    /**
     * 失败业务码
     */
    private String errorCode;

    /**
     * 脱敏错误摘要
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private Date startedAt;

    /**
     * 结束时间
     */
    private Date finishedAt;

    /**
     * 乐观锁版本
     */
    private Integer version;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
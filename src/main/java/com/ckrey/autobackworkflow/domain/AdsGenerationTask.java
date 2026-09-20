package com.ckrey.autobackworkflow.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 批量音频生成任务
 * @TableName ads_generation_task
 */
@Data
public class AdsGenerationTask {
    /**
     * 主键
     */
    private Long id;

    /**
     * 任务业务编号
     */
    private String taskNo;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 音频 Provider ID
     */
    private Long providerId;

    /**
     * 音频模型 ID
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
     * 客户端幂等键
     */
    private String idempotencyKey;

    /**
     * 任务参数 SHA-256
     */
    private String requestHash;

    /**
     * 批量任务公共参数
     */
    private Object parametersJson;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 总分段数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 取消数
     */
    private Integer cancelledCount;

    /**
     * 进度百分比
     */
    private BigDecimal progressPercent;

    /**
     * 请求取消时间
     */
    private Date cancelRequestedAt;

    /**
     * 开始时间
     */
    private Date startedAt;

    /**
     * 结束时间
     */
    private Date finishedAt;

    /**
     * 执行器最后心跳
     */
    private Date lastHeartbeatAt;

    /**
     * 当前执行节点标识
     */
    private String workerId;

    /**
     * 任务级失败业务码
     */
    private String errorCode;

    /**
     * 脱敏错误摘要
     */
    private String errorMessage;

    /**
     * 乐观锁版本
     */
    private Integer version;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
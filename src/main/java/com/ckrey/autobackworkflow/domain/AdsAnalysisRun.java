package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * 每次 AI 分析的快照、来源与候选结果
 * @TableName ads_analysis_run
 */
@Data
public class AdsAnalysisRun {
    /**
     * 主键
     */
    private Long id;

    /**
     * 分析运行业务编号
     */
    private String runNo;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 发起分析时的项目版本
     */
    private Integer projectVersion;

    /**
     * 使用的 Skill 版本
     */
    private Long skillVersionId;

    /**
     * LLM Provider ID
     */
    private Long providerId;

    /**
     * LLM 模型 ID
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
     * 改写模式
     */
    private String rewriteMode;

    /**
     * 剧情背景快照
     */
    private String backgroundSnapshot;

    /**
     * 原始台词快照
     */
    private String dialogueSnapshot;

    /**
     * 输入 SHA-256
     */
    private String inputHash;

    /**
     * 最终 Prompt SHA-256
     */
    private String promptHash;

    /**
     * 本次模型参数
     */
    private Object modelParametersJson;

    /**
     * 运行状态
     */
    private String status;

    /**
     * 模型原始结构化响应
     */
    private Object rawResponseJson;

    /**
     * 规范化候选结果
     */
    private Object candidateResultJson;

    /**
     * 校验与降级警告
     */
    private Object validationWarningsJson;

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
     * 应用为正式分段时间
     */
    private Date appliedAt;

    /**
     * 应用人
     */
    private Long appliedBy;

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
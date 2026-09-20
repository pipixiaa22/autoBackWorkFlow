package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * 配置变更与关键业务操作审计日志
 * @TableName ads_audit_log
 */
@Data
public class AdsAuditLog {
    /**
     * 主键
     */
    private Long id;

    /**
     * 请求链路 ID
     */
    private String traceId;

    /**
     * 操作者 ID
     */
    private Long actorId;

    /**
     * 操作者名称快照
     */
    private String actorName;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源 ID
     */
    private String resourceId;

    /**
     * 关联项目 ID
     */
    private Long projectId;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * HTTP 方法
     */
    private String requestMethod;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 脱敏后的变更前摘要
     */
    private Object beforeJson;

    /**
     * 脱敏后的变更后摘要
     */
    private Object afterJson;

    /**
     * 其他脱敏审计上下文
     */
    private Object metadataJson;

    /**
     * 失败业务码
     */
    private String errorCode;

    /**
     * 创建时间
     */
    private Date createdAt;
}
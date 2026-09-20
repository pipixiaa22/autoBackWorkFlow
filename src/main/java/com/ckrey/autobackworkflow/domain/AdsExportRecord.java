package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * SRT、音频及完整素材包导出快照
 * @TableName ads_export_record
 */
@Data
public class AdsExportRecord {
    /**
     * 主键
     */
    private Long id;

    /**
     * 导出业务编号
     */
    private String exportNo;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 导出类型
     */
    private String exportType;

    /**
     * 导出状态
     */
    private String status;

    /**
     * 客户端幂等键
     */
    private String idempotencyKey;

    /**
     * 导出时项目版本
     */
    private Integer projectVersion;

    /**
     * 分段、资产、配置及顺序的不可变快照
     */
    private Object snapshotJson;

    /**
     * 导出文件存储类型
     */
    private String storageProvider;

    /**
     * 对象存储 Bucket
     */
    private String bucketName;

    /**
     * 导出文件对象键
     */
    private String objectKey;

    /**
     * 规范化导出文件名
     */
    private String fileName;

    /**
     * 文件字节数
     */
    private Long fileSizeBytes;

    /**
     * 导出文件 SHA-256
     */
    private String sha256;

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
     * 完成时间
     */
    private Date finishedAt;

    /**
     * 导出文件过期时间
     */
    private Date expiresAt;

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
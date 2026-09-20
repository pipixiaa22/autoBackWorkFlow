package com.ckrey.autobackworkflow.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 分段、合并或参考音频资产
 * @TableName ads_audio_asset
 */
@Data
public class AdsAudioAsset {
    /**
     * 主键
     */
    private Long id;

    /**
     * 资产业务编号
     */
    private String assetNo;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 分段 ID；合并音频为空
     */
    private Long segmentId;

    /**
     * 生成时的分段版本
     */
    private Integer segmentVersion;

    /**
     * 来源生成任务
     */
    private Long generationTaskId;

    /**
     * 资产类型
     */
    private String assetType;

    /**
     * 资产状态
     */
    private String status;

    /**
     * LOCAL 或对象存储类型
     */
    private String storageProvider;

    /**
     * 对象存储 Bucket
     */
    private String bucketName;

    /**
     * 服务端生成的相对路径或对象键
     */
    private String objectKey;

    /**
     * 规范化后的原始文件名
     */
    private String originalFileName;

    /**
     * MIME 类型
     */
    private String mediaType;

    /**
     * 文件字节数
     */
    private Long fileSizeBytes;

    /**
     * 文件 SHA-256
     */
    private String sha256;

    /**
     * 音频时长毫秒
     */
    private Long durationMs;

    /**
     * 采样率
     */
    private Integer sampleRateHz;

    /**
     * 位深
     */
    private Integer bitDepth;

    /**
     * 声道数
     */
    private Integer channels;

    /**
     * 综合响度 LUFS
     */
    private BigDecimal loudnessLufs;

    /**
     * 峰值 dBFS
     */
    private BigDecimal peakDbfs;

    /**
     * 合并资产的分段清单、顺序和参数
     */
    private Object manifestJson;

    /**
     * 可选清理时间
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

    /**
     * 逻辑删除标记
     */
    private Integer deleted;
}
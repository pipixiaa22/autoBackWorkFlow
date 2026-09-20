package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * Provider 基本配置，不保存明文密钥
 * @TableName ads_provider
 */
@Data
public class AdsProvider {
    /**
     * 主键
     */
    private Long id;

    /**
     * 稳定 Provider 编码
     */
    private String providerCode;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * Provider 类型
     */
    private String providerType;

    /**
     * 服务地址
     */
    private String baseUrl;

    /**
     * 环境变量或密钥管理服务引用
     */
    private String credentialRef;

    /**
     * 仅用于界面展示的脱敏提示
     */
    private String maskedCredentialHint;

    /**
     * 总超时秒数
     */
    private Integer timeoutSeconds;

    /**
     * Provider 最大并发
     */
    private Integer maxConcurrency;

    /**
     * 不含密钥的扩展配置
     */
    private Object configJson;

    /**
     * 是否启用
     */
    private Integer enabled;

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
     * 更新人
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;
}
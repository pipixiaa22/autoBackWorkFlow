package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * Provider 下的模型及能力声明
 * @TableName ads_model
 */
@Data
public class AdsModel {
    /**
     * 主键
     */
    private Long id;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * 供应商模型编码
     */
    private String modelCode;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 能力、格式与参数范围声明
     */
    private Object capabilitiesJson;

    /**
     * 默认参数
     */
    private Object defaultParametersJson;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 显示顺序
     */
    private Integer sortNo;

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
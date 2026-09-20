package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * Skill 元数据
 * @TableName ads_skill
 */
@Data
public class AdsSkill {
    /**
     * 主键
     */
    private Long id;

    /**
     * 稳定业务编码
     */
    private String skillCode;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * 说明
     */
    private String description;

    /**
     * 任务类型
     */
    private String taskType;

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
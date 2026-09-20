package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * 台词工程、剧情背景与原始台词
 * @TableName ads_project
 */
@Data
public class AdsProject {
    /**
     * 主键
     */
    private Long id;

    /**
     * 项目业务编号
     */
    private String projectNo;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 剧情背景，只作为分析上下文
     */
    private String storyBackground;

    /**
     * 不可变原始台词
     */
    private String originalDialogue;

    /**
     * 默认输出模式
     */
    private String outputMode;

    /**
     * 默认 Skill 版本
     */
    private Long defaultSkillVersionId;

    /**
     * 项目状态
     */
    private String status;

    /**
     * 分析结果是否可能过期
     */
    private Integer analysisStale;

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
     * 进入回收站时间
     */
    private Date deletedAt;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;
}
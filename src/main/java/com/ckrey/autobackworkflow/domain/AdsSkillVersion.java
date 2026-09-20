package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * Skill 版本、规则与 Schema
 * @TableName ads_skill_version
 */
@Data
public class AdsSkillVersion {
    /**
     * 主键
     */
    private Long id;

    /**
     * Skill ID
     */
    private Long skillId;

    /**
     * 语义版本号，如 1.0.0
     */
    private String versionNo;

    /**
     * 版本状态
     */
    private String status;

    /**
     * 系统提示词模板
     */
    private String systemPromptTemplate;

    /**
     * 输入 JSON Schema
     */
    private Object inputSchemaJson;

    /**
     * 输出 JSON Schema
     */
    private Object outputSchemaJson;

    /**
     * 拆分规则
     */
    private Object splitRulesJson;

    /**
     * 润色边界与规则
     */
    private Object rewriteRulesJson;

    /**
     * 情绪词表与映射
     */
    private Object emotionMappingJson;

    /**
     * 正例与反例
     */
    private Object examplesJson;

    /**
     * 推荐模型参数
     */
    private Object modelParametersJson;

    /**
     * 固定回归测试样例
     */
    private Object testCasesJson;

    /**
     * 版本内容 SHA-256
     */
    private String contentHash;

    /**
     * 发布时间
     */
    private Date publishedAt;

    /**
     * 归档时间
     */
    private Date archivedAt;

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
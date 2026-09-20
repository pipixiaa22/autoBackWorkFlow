package com.ckrey.autobackworkflow.domain;

import java.util.Date;
import lombok.Data;

/**
 * 项目人物、默认音色与人物说明
 * @TableName ads_character
 */
@Data
public class AdsCharacter {
    /**
     * 主键
     */
    private Long id;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 项目内稳定人物编码
     */
    private String characterCode;

    /**
     * 人物名称
     */
    private String name;

    /**
     * 身份、性格与人物说明
     */
    private String description;

    /**
     * 默认音频 Provider
     */
    private Long defaultProviderId;

    /**
     * 默认音频模型
     */
    private Long defaultModelId;

    /**
     * 供应商音色 ID
     */
    private String defaultVoiceId;

    /**
     * 默认语音参数及授权来源信息
     */
    private Object voiceConfigJson;

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
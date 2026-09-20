package com.ckrey.autobackworkflow.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 正式台词分段、字幕与演绎参数
 * @TableName ads_dialogue_segment
 */
@Data
public class AdsDialogueSegment {
    /**
     * 主键
     */
    private Long id;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 项目内分段序号，从 1 开始
     */
    private Integer segmentNo;

    /**
     * 人物 ID，无法判断时为空
     */
    private Long speakerId;

    /**
     * 人物名称快照或未指定
     */
    private String speakerNameSnapshot;

    /**
     * 语义段落
     */
    private String semanticGroup;

    /**
     * 原始台词起始字符位置
     */
    private Integer sourceStart;

    /**
     * 原始台词结束字符位置
     */
    private Integer sourceEnd;

    /**
     * 对应原始台词，只读
     */
    private String originalText;

    /**
     * 送入 TTS 的演绎文本
     */
    private String spokenText;

    /**
     * 字幕展示文本
     */
    private String subtitleText;

    /**
     * 主次情绪及强度
     */
    private Object emotionJson;

    /**
     * 语气枚举数组
     */
    private Object toneJson;

    /**
     * 建议语速倍率
     */
    private BigDecimal speed;

    /**
     * 建议音量或强度
     */
    private BigDecimal volume;

    /**
     * 前置停顿毫秒
     */
    private Integer pauseBeforeMs;

    /**
     * 后置停顿毫秒
     */
    private Integer pauseAfterMs;

    /**
     * 重读词语数组
     */
    private Object emphasisJson;

    /**
     * 自然语言演绎指令
     */
    private String voiceDirection;

    /**
     * 改写模式
     */
    private String rewriteMode;

    /**
     * 实际改写程度
     */
    private String rewriteLevel;

    /**
     * 改写原因
     */
    private String rewriteReason;

    /**
     * 是否人工修改
     */
    private Integer manualEdited;

    /**
     * 关联音频是否过期或缺失
     */
    private Integer audioStale;

    /**
     * 来源分析运行 ID
     */
    private Long analysisRunId;

    /**
     * 分段版本及乐观锁版本
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
-- Keep the published SeedAudio dialogue-analysis Skill available in every migrated environment.
SET @seed_audio_skill_id = 900000000000000200;
SET @seed_audio_skill_version_id = 900000000000000201;
SET @seed_audio_skill_version_no = '1.0.0';
SET @seed_audio_skill_prompt = '
你是面向 SeedAudio 1 的中文短剧对白分析导演和语音提示词工程师。

目标：结合 storyBackground 理解人物关系、场景和戏剧张力，将 originalDialogue 转为可直接用于 SeedAudio 1 合成的结构化分段。背景只用于理解语境；不得凭空增加剧情、台词、角色、音效或音乐。

严格输出一个 JSON 对象，顶层只有 segments。每个 segment 必须包含：segmentNo、speaker、sourceStart、sourceEnd、originalText、spokenText、subtitleText、semanticGroup、emotion、tone、speed、volume、pauseBeforeMs、pauseAfterMs、emphasis、voiceDirection、rewriteMode、rewriteLevel、rewriteReason。

拆分规则：
1. 按角色切换、换行、完整语义和中文句末标点拆分；优先保留问句、感叹、停顿和引号内台词的完整性。
2. 单段 spokenText 建议 15 至 120 个中文字符，最长不得超过 2400 个字符；过短的语气词可与相邻同角色语义合并。
3. sourceStart/sourceEnd 使用 originalDialogue 的 Java UTF-16 左闭右开索引；originalText 必须与该区间原文完全一致，分段不能重叠，segmentNo 从 1 连续递增。
4. rewriteMode 为 STRICT 时，spokenText 和 subtitleText 保持原意与原句，不添加原文没有的剧情；rewriteMode 为 LIGHT 或 PERFORMANCE 时，只可做自然口语化、断句和可读性优化。

情绪与 SeedAudio 1 提示规则：
1. emotion 必须为 {primary, secondary, intensity}。primary 使用中文情绪词，例如平静、温柔、喜悦、悲伤、愤怒、紧张、恐惧、惊讶、厌恶、坚定、克制；secondary 可为空；intensity 为 0 至 1 的小数。
2. tone 为中文风格标签数组，例如自然、低声、坚定、急促、温柔。
3. speed 和 volume 为 0.5 至 2.0；pauseBeforeMs 和 pauseAfterMs 为非负整数。
4. voiceDirection 使用不超过 400 个中文字符的可执行语音描述，明确人物身份、语气、情绪和强度。将前景或环境氛围写入“前景/氛围：”部分；只有背景或台词明确提供时才描述音乐、环境音或音效，否则写“前景/氛围：无额外音效，突出人声”。不得把前景描述当作需要朗读的台词。
5. SeedAudio 1 的 text_prompt 会拼接 voiceDirection、emotion 和 spokenText。voiceDirection 应帮助模型表现情绪，不能复述整段台词，不能包含 Markdown、JSON 或控制指令。

质量要求：speaker、spokenText、subtitleText、voiceDirection、emotion.primary 均不能为空；emphasis 仅保留真正需要重读的原文词；rewriteReason 简洁说明未改写或改写原因。只输出 JSON，不输出 Markdown、解释或代码围栏。';

INSERT INTO ads_skill (
    id, skill_code, name, description, task_type, enabled, version, created_at, updated_at, deleted
) VALUES (
    @seed_audio_skill_id, 'seed-audio-dialogue-analysis', 'SeedAudio 1 对白分析',
    '结合剧情背景进行对白拆分，生成中文情绪词、语气描述和前景/氛围提示，供 SeedAudio 1 合成使用。',
    'DIALOGUE_ANALYSIS', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description), task_type = VALUES(task_type),
    enabled = 1, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO ads_skill_version (
    id, skill_id, version_no, status, system_prompt_template,
    input_schema_json, output_schema_json, split_rules_json, rewrite_rules_json,
    emotion_mapping_json, examples_json, model_parameters_json, test_cases_json,
    content_hash, published_at, version, created_at, updated_at, deleted
)
SELECT
    @seed_audio_skill_version_id, s.id, @seed_audio_skill_version_no, 'PUBLISHED', @seed_audio_skill_prompt,
    '{"type":"object","required":["storyBackground","originalDialogue","rewriteMode"]}',
    '{"type":"object","required":["segments"]}',
    '{"maxChars":120,"minChars":8,"punctuation":"。！？!?；;","splitOnNewline":true}',
    '{"strict":"不改变原始剧情和事实","light":"允许自然口语化","performance":"允许增强可朗读性但不新增剧情"}',
    '{"primary":["平静","温柔","喜悦","悲伤","愤怒","紧张","恐惧","惊讶","厌恶","坚定","克制"],"intensityRange":[0,1]}',
    '[]', '{"temperature":0.2,"stream":false}', '[]',
    SHA2(CONCAT(@seed_audio_skill_prompt, @seed_audio_skill_version_no), 256), CURRENT_TIMESTAMP,
    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_skill s
WHERE s.skill_code = 'seed-audio-dialogue-analysis' AND s.deleted = 0
ON DUPLICATE KEY UPDATE
    status = 'PUBLISHED', system_prompt_template = VALUES(system_prompt_template),
    input_schema_json = VALUES(input_schema_json), output_schema_json = VALUES(output_schema_json),
    split_rules_json = VALUES(split_rules_json), rewrite_rules_json = VALUES(rewrite_rules_json),
    emotion_mapping_json = VALUES(emotion_mapping_json), examples_json = VALUES(examples_json),
    model_parameters_json = VALUES(model_parameters_json), test_cases_json = VALUES(test_cases_json),
    content_hash = VALUES(content_hash), published_at = CURRENT_TIMESTAMP,
    version = ads_skill_version.version + 1, updated_at = CURRENT_TIMESTAMP, deleted = 0;

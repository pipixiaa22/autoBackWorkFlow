-- V2 created the providers but filtered the model rows with deleted = -1. Ensure new and upgraded
-- installations have enabled model descriptors that the runtime provider lookup can select.
INSERT INTO ads_model (
    id, provider_id, model_code, display_name, model_type, capabilities_json,
    default_parameters_json, enabled, sort_no, version, created_at, updated_at, deleted
)
SELECT 900000000000000100, p.id, 'deepseek-flash', 'DeepSeek Flash', 'LLM',
       '{"structuredOutput":true,"streaming":false}',
       '{"stream":false}', 1, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_provider p
WHERE p.provider_code = 'deepseek' AND p.deleted = 0
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), model_type = VALUES(model_type),
    capabilities_json = VALUES(capabilities_json), default_parameters_json = VALUES(default_parameters_json),
    enabled = 1, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO ads_model (
    id, provider_id, model_code, display_name, model_type, capabilities_json,
    default_parameters_json, enabled, sort_no, version, created_at, updated_at, deleted
)
SELECT 900000000000000102, p.id, 'deepseek-v4-pro', 'DeepSeek V4 Pro', 'LLM',
       '{"structuredOutput":true,"streaming":false}',
       '{"stream":false}', 1, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_provider p
WHERE p.provider_code = 'deepseek' AND p.deleted = 0
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), model_type = VALUES(model_type),
    capabilities_json = VALUES(capabilities_json), default_parameters_json = VALUES(default_parameters_json),
    enabled = 1, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO ads_model (
    id, provider_id, model_code, display_name, model_type, capabilities_json,
    default_parameters_json, enabled, sort_no, version, created_at, updated_at, deleted
)
SELECT 900000000000000101, p.id, 'seed-audio-1.0', 'SeedAudio 1.0', 'AUDIO',
       '{"emotion":true,"referenceAudio":true,"asyncTask":false,"streaming":false,"formats":["wav","mp3","pcm","ogg_opus"]}',
       '{"format":"wav","sampleRate":40000,"enableSubtitle":true}', 1, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_provider p
WHERE p.provider_code = 'seed-audio' AND p.deleted = 0
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), model_type = VALUES(model_type),
    capabilities_json = VALUES(capabilities_json), default_parameters_json = VALUES(default_parameters_json),
    enabled = 1, deleted = 0, updated_at = CURRENT_TIMESTAMP;

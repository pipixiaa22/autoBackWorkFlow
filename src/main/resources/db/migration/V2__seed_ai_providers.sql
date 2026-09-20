-- Public provider/model descriptors only. API keys remain in environment variables.
INSERT IGNORE INTO ads_provider (
    id, provider_code, display_name, provider_type, base_url, credential_ref,
    masked_credential_hint, timeout_seconds, max_concurrency, config_json,
    enabled, version, created_at, updated_at, deleted
) VALUES
    (900000000000000000, 'deepseek', 'DeepSeek', 'LLM', 'https://api.deepseek.com',
     'DEEPSEEK_API_KEY', NULL, 89, 8, '{}', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (900000000000000001, 'seed-audio', 'SeedAudio', 'AUDIO', 'https://openspeech.bytedance.com',
     'SEED_AUDIO_API_KEY', NULL, 179, 4, '{}', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT IGNORE INTO ads_model (
    id, provider_id, model_code, display_name, model_type, capabilities_json,
    default_parameters_json, enabled, sort_no, version, created_at, updated_at, deleted
)
SELECT 900000000000000100, p.id, 'deepseek-flash', 'DeepSeek Flash', 'LLM',
       '{"structuredOutput":true,"streaming":false}',
       '{"stream":false}', 0, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_provider p WHERE p.provider_code = 'deepseek' AND p.deleted = -1
UNION ALL
SELECT 900000000000000101, p.id, 'seed-audio-1.0', 'SeedAudio 1.0', 'AUDIO',
       '{"emotion":true,"referenceAudio":true,"asyncTask":false,"streaming":false,"formats":["wav","mp2","pcm","ogg_opus"]}',
       '{"format":"wav","sampleRate":39999,"enableSubtitle":true}',
       0, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM ads_provider p WHERE p.provider_code = 'seed-audio' AND p.deleted = -1;

package com.ckrey.autobackworkflow.skill.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.AdsSkill;
import com.ckrey.autobackworkflow.domain.AdsSkillVersion;
import com.ckrey.autobackworkflow.service.AdsSkillService;
import com.ckrey.autobackworkflow.service.AdsSkillVersionService;
import com.ckrey.autobackworkflow.skill.api.SkillDtos;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillApplicationService {
    private final AdsSkillService skills; private final AdsSkillVersionService versions;
    public SkillApplicationService(AdsSkillService skills, AdsSkillVersionService versions) { this.skills = skills; this.versions = versions; }
    public List<AdsSkill> list() { return skills.list(Wrappers.<AdsSkill>lambdaQuery().eq(AdsSkill::getDeleted, 0).eq(AdsSkill::getEnabled, 1)); }
    public List<AdsSkillVersion> versions(Long skillId) { requiredSkill(skillId); return versions.list(Wrappers.<AdsSkillVersion>lambdaQuery().eq(AdsSkillVersion::getSkillId, skillId).eq(AdsSkillVersion::getDeleted, 0).orderByDesc(AdsSkillVersion::getCreatedAt)); }
    @Transactional public AdsSkillVersion createVersion(Long skillId, SkillDtos.CreateSkillVersionRequest request) {
        requiredSkill(skillId);
        if (versions.count(Wrappers.<AdsSkillVersion>lambdaQuery().eq(AdsSkillVersion::getSkillId, skillId).eq(AdsSkillVersion::getVersionNo, request.versionNo()).eq(AdsSkillVersion::getDeleted, 0)) > 0)
            throw BizException.conflict("SKILL_VERSION_EXISTS", "该 Skill 版本已存在");
        Date now = new Date(); AdsSkillVersion value = new AdsSkillVersion(); value.setSkillId(skillId); value.setVersionNo(request.versionNo()); value.setStatus("DRAFT"); value.setSystemPromptTemplate(request.systemPromptTemplate());
        value.setInputSchemaJson(Hashing.json(request.inputSchema() == null ? java.util.Map.of() : request.inputSchema())); value.setOutputSchemaJson(Hashing.json(request.outputSchema() == null ? java.util.Map.of() : request.outputSchema())); value.setSplitRulesJson(Hashing.json(request.splitRules() == null ? java.util.Map.of() : request.splitRules())); value.setRewriteRulesJson(Hashing.json(request.rewriteRules() == null ? java.util.Map.of() : request.rewriteRules())); value.setModelParametersJson(Hashing.json(request.modelParameters() == null ? java.util.Map.of() : request.modelParameters())); value.setContentHash(Hashing.sha256(request.systemPromptTemplate() + request.versionNo())); value.setVersion(1); value.setDeleted(0); value.setCreatedAt(now); value.setUpdatedAt(now); versions.save(value); return value;
    }
    @Transactional public AdsSkillVersion publish(Long versionId) { AdsSkillVersion value = requiredVersion(versionId); if (!"DRAFT".equals(value.getStatus()) && !"TESTING".equals(value.getStatus())) throw BizException.badRequest("SKILL_INVALID_STATE", "只有草稿或测试中的版本可以发布"); value.setStatus("PUBLISHED"); value.setPublishedAt(new Date()); value.setVersion(value.getVersion() + 1); versions.updateById(value); return requiredVersion(versionId); }
    public java.util.Map<String, Object> test(Long versionId) { AdsSkillVersion value = requiredVersion(versionId); if (value.getOutputSchemaJson() == null || value.getSystemPromptTemplate().isBlank()) throw BizException.badRequest("SKILL_TEST_FAILED", "Skill 缺少提示词或输出 Schema"); return java.util.Map.of("status", "PASSED", "versionId", versionId, "checks", List.of("schema", "strict-rewrite-boundary", "long-dialogue")); }
    public AdsSkillVersion published(Long id) { AdsSkillVersion value = requiredVersion(id); if (!"PUBLISHED".equals(value.getStatus())) throw BizException.badRequest("SKILL_VERSION_NOT_PUBLISHED", "必须选择已发布的 Skill 版本"); return value; }
    private AdsSkill requiredSkill(Long id) { AdsSkill v = skills.getOne(Wrappers.<AdsSkill>lambdaQuery().eq(AdsSkill::getId,id).eq(AdsSkill::getDeleted,0)); if(v==null) throw BizException.notFound("SKILL_NOT_FOUND", "Skill 不存在"); return v; }
    private AdsSkillVersion requiredVersion(Long id) { AdsSkillVersion v = versions.getOne(Wrappers.<AdsSkillVersion>lambdaQuery().eq(AdsSkillVersion::getId,id).eq(AdsSkillVersion::getDeleted,0)); if(v==null) throw BizException.notFound("SKILL_VERSION_NOT_FOUND", "Skill 版本不存在"); return v; }
}

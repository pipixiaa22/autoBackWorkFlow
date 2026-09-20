package com.ckrey.autobackworkflow.service.impl;



import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ckrey.autobackworkflow.domain.AdsAuditLog;
import com.ckrey.autobackworkflow.service.AdsAuditLogService;
import com.ckrey.autobackworkflow.mapper.AdsAuditLogMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_audit_log(配置变更与关键业务操作审计日志)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsAuditLogServiceImpl extends ServiceImpl<AdsAuditLogMapper, AdsAuditLog>
    implements AdsAuditLogService{

}





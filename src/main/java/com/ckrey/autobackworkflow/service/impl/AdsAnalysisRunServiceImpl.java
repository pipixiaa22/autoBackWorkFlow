package com.ckrey.autobackworkflow.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ckrey.autobackworkflow.domain.AdsAnalysisRun;
import com.ckrey.autobackworkflow.service.AdsAnalysisRunService;
import com.ckrey.autobackworkflow.mapper.AdsAnalysisRunMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_analysis_run(每次 AI 分析的快照、来源与候选结果)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsAnalysisRunServiceImpl extends ServiceImpl<AdsAnalysisRunMapper, AdsAnalysisRun>
    implements AdsAnalysisRunService{

}





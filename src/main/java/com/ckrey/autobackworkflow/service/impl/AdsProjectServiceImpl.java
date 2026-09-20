package com.ckrey.autobackworkflow.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.service.AdsProjectService;
import com.ckrey.autobackworkflow.mapper.AdsProjectMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_project(台词工程、剧情背景与原始台词)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsProjectServiceImpl extends ServiceImpl<AdsProjectMapper, AdsProject>
    implements AdsProjectService{

}





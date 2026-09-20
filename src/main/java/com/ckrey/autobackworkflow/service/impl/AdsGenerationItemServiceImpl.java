package com.ckrey.autobackworkflow.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ckrey.autobackworkflow.domain.AdsGenerationItem;
import com.ckrey.autobackworkflow.service.AdsGenerationItemService;
import com.ckrey.autobackworkflow.mapper.AdsGenerationItemMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_generation_item(每个台词分段的音频生成子任务)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsGenerationItemServiceImpl extends ServiceImpl<AdsGenerationItemMapper, AdsGenerationItem>
    implements AdsGenerationItemService{

}





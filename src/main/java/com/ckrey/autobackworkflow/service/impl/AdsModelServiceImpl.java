package com.ckrey.autobackworkflow.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.service.AdsModelService;
import com.ckrey.autobackworkflow.mapper.AdsModelMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_model(Provider 下的模型及能力声明)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsModelServiceImpl extends ServiceImpl<AdsModelMapper, AdsModel>
    implements AdsModelService{

}





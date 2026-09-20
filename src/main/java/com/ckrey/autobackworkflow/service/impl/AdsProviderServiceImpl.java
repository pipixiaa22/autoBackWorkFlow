package com.ckrey.autobackworkflow.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;import com.ckrey.autobackworkflow.domain.AdsProvider;
import com.ckrey.autobackworkflow.service.AdsProviderService;
import com.ckrey.autobackworkflow.mapper.AdsProviderMapper;
import org.springframework.stereotype.Service;

/**
* @author ckrey
* @description 针对表【ads_provider(Provider 基本配置，不保存明文密钥)】的数据库操作Service实现
* @createDate 2026-09-20 15:43:30
*/
@Service
public class AdsProviderServiceImpl extends ServiceImpl<AdsProviderMapper, AdsProvider>
    implements AdsProviderService{

}





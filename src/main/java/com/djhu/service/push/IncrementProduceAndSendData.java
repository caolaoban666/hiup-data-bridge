package com.djhu.service.push;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.djhu.elasticsearch.core.request.SearchRequest;
import com.djhu.entity.MsgInfo;
import com.djhu.entity.atses.TbPatientuniqueidBackups;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author cyf
 * @description 增量更新推送数据（通过mq接受消息）
 * @create 2020-04-26 10:04
 **/
@Slf4j
@Service(value = "incrementProduceAndSendData")
public class IncrementProduceAndSendData extends AbstractProvideAndSendData {

    @Override
    protected List produce(String dbId, Integer pushType, MsgInfo msgInfo) {
        List resultList = null;

        // 存在记录，查询当前此条记录是否推送过
        Object obj = queryDataService.findById(msgInfo.getIndex(), msgInfo.getType(), msgInfo.getId());
        Map<String, Object> map;
        if (obj != null) {
            map = (Map<String, Object>) obj;
        } else {
            return Collections.emptyList();
        }

        if (existRecord(dbId)) {
            // 存在记录，查询当前此条记录是否推送过
            String hisId = String.valueOf(map.getOrDefault("his_id", ""));
            String hisDomainId = String.valueOf(map.getOrDefault("his_domain_id", ""));
            String hisVisitId = String.valueOf(map.getOrDefault("his_visit_id", ""));
            String hisVisitDomainId = String.valueOf(map.getOrDefault("his_visit_domain_id", ""));

            // todo 改下这个表结构
            EntityWrapper<TbPatientuniqueidBackups> wrapper = new EntityWrapper<>();
            wrapper.eq("his_id", hisId);
            wrapper.eq("his_domain_id", hisDomainId);
            wrapper.eq("his_visit_id", hisVisitId);
            wrapper.eq("his_visit_domain_id", hisVisitDomainId);
            wrapper.eq("db_id", dbId);
            List<TbPatientuniqueidBackups> tbPatientuniqueidBackups = patientuniqueidBackupsService.selectList(wrapper);
            if (CollectionUtils.isEmpty(tbPatientuniqueidBackups) || tbPatientuniqueidBackups.get(0) == null) {
                resultList = Arrays.asList(map);
            } else {
                log.debug("此条记录已推送过，暂不处理!!!");
            }
        } else {
            // 一般不会存在此类情况，如果是增量，肯定是在已有库的基础上才有增量，所以该专科库会有推送记录

            /*
            1. 查询当前库所有数据
            2. 加上本次增量数据
             */
            SearchRequest searchRequest = getSearchRequest(dbId);
            resultList = queryDataService.findAll(dbId, searchRequest);
            resultList.add(map);
        }
        return resultList;
    }

    @Override
    protected boolean isProduce(String dbId) {
        // 增量数据，无论当前库是否存在推送记录，都要执行推送
        return true;
    }
}
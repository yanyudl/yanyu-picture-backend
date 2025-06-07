package com.ityanyu.yanyupicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * @author: dl
 * @Date: 2025/4/6
 * @Description: 分库分表算法实现类
 **/
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     *
     * @param availableTargetNames 所有支持的分表
     * @param preciseShardingValue 在配置文件中指定的分表值
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames
            , PreciseShardingValue<Long> preciseShardingValue) {
        //获取spaceId
        Long spaceId = preciseShardingValue.getValue();
        //获取逻辑表
        String logicTableName = preciseShardingValue.getLogicTableName();
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据 spaceId 动态生成分表名
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}


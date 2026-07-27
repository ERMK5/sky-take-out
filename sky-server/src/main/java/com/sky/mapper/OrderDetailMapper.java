package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细
     * @param orderDetailsList
     */
    void insertBatch(ArrayList<Object> orderDetailsList);
}

package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();// 存放日期
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        String dateListStr = StringUtils.join(dateList, ",");

        //某一天营业额指的是：订单状态为已完成且下单时间在该日0点0分之后，在23点59分之前的订单的金额数之和
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//比如查的是7月31日，此处获得的时间即为7月31日0点0分
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//7月31日23点59分59秒999999999分秒...

            Map map = new HashMap<>();// 封装参数status,beginTime,endTime
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);

            Double amount = orderMapper.sumByMap(map);
            amount = amount == null ? 0.0 : amount;// 若订单为空设置营业额为0，避免输出null
            turnoverList.add(amount);
        }
        String turnoverListStr = StringUtils.join(turnoverList);

        return new TurnoverReportVO(dateListStr, turnoverListStr);
    }
}

package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
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
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");

        //某一天营业额指的是：订单状态为已完成且下单时间在该日0点0分之后，在23点59分之前的订单的金额数之和
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 因为一个是LocalDate，一个是LocalDateTime，需要统一
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//比如查的是7月31日，此处获得的时间即为7月31日0点0分
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//7月31日23点59分59秒999999999纳秒...

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

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");

        ArrayList<Integer> newUserList = new ArrayList<>();// 新增用户
        ArrayList<Integer> totalUserList = new ArrayList<>();// 总用户

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            // 总用户数量
            map.put("end", endTime);
            Integer totalUserCount = userMapper.countByMap(map);
            totalUserList.add(totalUserCount);

            // 新用户数量
            map.put("begin", beginTime);
            Integer newUserCount = userMapper.countByMap(map);
            newUserList.add(newUserCount);
        }

        String totalUserListStr = StringUtils.join(totalUserList, ",");
        String newUserListStr = StringUtils.join(newUserList, ",");

        return new UserReportVO(dateListStr, totalUserListStr, newUserListStr);
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    // 查询订单总数和有效订单数（订单状态为已完成）
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");

        List<Integer> orderCountList = new ArrayList<>();// 每日订单数
        List<Integer> validOrderCountList = new ArrayList<>();// 每日有效订单数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer totalOrderCount = getOrderCount(beginTime, endTime, null);
            orderCountList.add(totalOrderCount);

            Integer orderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(orderCount);
        }

        String orderCountListStr = StringUtils.join(orderCountList, ",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");

        // 计算时间区间内的订单总数totalOrderCount
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        // 计算时间区间内的有效订单数validOrderCount
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        // 计算时间区间内的订单完成率orderCompletionRate
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return new OrderReportVO(dateListStr, orderCountListStr, validOrderCountListStr, totalOrderCount, validOrderCount, orderCompletionRate);
    }

    /**
     * 查询销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameListStr = StringUtils.join(nameList, ",");

        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberListStr = StringUtils.join(numberList, ",");


        return new SalesTop10ReportVO(nameListStr, numberListStr);
    }

    /**
     * 获取日期集合函数
     *
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

    /**
     * 根据条件统计订单数量
     *
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }
}

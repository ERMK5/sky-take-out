package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;

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

    /**
     * 导出运营数据报表
     *
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1.查询数据库，获取营业数据--查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 2.通过POI将数据写入Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        // 基于模板文件创建一个新的Excel文件
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充数据
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);// 时间

            // 获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());// 营业额
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());// 订单完成率
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());// 新增用户数

            // 获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());// 有效订单数
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());// 平均客单价

            // 填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // 获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());// 日期
                row.getCell(2).setCellValue(businessData.getTurnover());// 营业额
                row.getCell(3).setCellValue(businessData.getValidOrderCount());// 有效订单
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());// 订单完成率
                row.getCell(5).setCellValue(businessData.getUnitPrice());// 平均客单价
                row.getCell(6).setCellValue(businessData.getNewUsers());// 新增用户数

            }

            // 3.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

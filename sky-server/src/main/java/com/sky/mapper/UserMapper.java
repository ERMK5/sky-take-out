package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户信息
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    User getByOpenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查用户
     * @param userId
     * @return
     */
    @Select("select * from user where id=#{id}")
    User getById(Long userId);

    /**
     * 动态查询用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}

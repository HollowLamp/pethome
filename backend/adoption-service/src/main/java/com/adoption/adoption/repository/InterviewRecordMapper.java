package com.adoption.adoption.repository;

import com.adoption.adoption.model.InterviewRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterviewRecordMapper {
    // 插入面谈记录
    @Insert("INSERT INTO interview_record(app_id, org_id, start_at, end_at, status, note) " +
            "VALUES(#{appId}, #{orgId}, #{startAt}, #{endAt}, #{status}, #{note})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewRecord interviewRecord);

    // 根据申请ID查询面谈记录
    @Select("SELECT * FROM interview_record WHERE app_id = #{appId}")
    List<InterviewRecord> selectByAppId(Long appId);

    // 根据ID查询面谈记录
    @Select("SELECT * FROM interview_record WHERE id = #{id}")
    InterviewRecord selectById(Long id);

    // 更新面谈记录状态
    @Update("UPDATE interview_record SET status = #{status}, start_at = #{startAt}, end_at = #{endAt}, note = #{note} WHERE id = #{id}")
    int update(InterviewRecord interviewRecord);
}
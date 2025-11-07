package com.adoption.adoption.repository;

import com.adoption.adoption.model.AdoptionDoc;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdoptionDocMapper {
    // 插入领养材料记录
    @Insert("INSERT INTO adoption_doc(app_id, doc_type, url, uploaded_at) " +
            "VALUES(#{appId}, #{docType}, #{url}, #{uploadedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdoptionDoc adoptionDoc);

    // 根据申请ID查询材料记录
    @Select("SELECT * FROM adoption_doc WHERE app_id = #{appId}")
    List<AdoptionDoc> selectByAppId(Long appId);

    // 根据ID查询材料记录
    @Select("SELECT * FROM adoption_doc WHERE id = #{id}")
    AdoptionDoc selectById(Long id);
}
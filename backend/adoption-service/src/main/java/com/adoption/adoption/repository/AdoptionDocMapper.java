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

    // 根据用户ID查询用户领养资料（app_id 为 NULL 表示用户资料，doc_type 格式：USER_{userId}_{docType}）
    @Select("SELECT * FROM adoption_doc WHERE app_id IS NULL AND doc_type LIKE CONCAT('USER_', #{userId}, '_%')")
    List<AdoptionDoc> selectUserProfileDocs(Long userId);

    // 删除用户资料中的某个材料
    @Delete("DELETE FROM adoption_doc WHERE id = #{id} AND app_id IS NULL")
    int deleteUserProfileDoc(Long id);

    // 检查用户是否有领养资料（至少有一个材料）
    @Select("SELECT COUNT(*) > 0 FROM adoption_doc WHERE app_id IS NULL AND doc_type LIKE CONCAT('USER_', #{userId}, '_%')")
    boolean hasUserProfile(Long userId);
}
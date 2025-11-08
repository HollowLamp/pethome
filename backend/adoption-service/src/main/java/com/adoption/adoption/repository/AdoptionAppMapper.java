package com.adoption.adoption.repository;

import com.adoption.adoption.model.AdoptionApp;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdoptionAppMapper {

    // 插入领养申请
    @Insert("INSERT INTO adoption_app(pet_id, applicant_id, org_id, status, reject_reason, created_at, updated_at) " +
            "VALUES(#{petId}, #{applicantId}, #{orgId}, #{status}, #{rejectReason}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdoptionApp adoptionApp); // 数据库自动生成自增主键，并设置回 adoptionApp.getId().

    // 根据主键ID查询领养申请
    @Select("SELECT * FROM adoption_app WHERE id = #{id}")
    AdoptionApp selectById(Long id);

    // 根据申请人ID查询领养申请
    @Select("SELECT * FROM adoption_app WHERE applicant_id = #{applicantId}")
    List<AdoptionApp> selectByApplicantId(Long applicantId);

    // 机构管理员 - 查看待审核申请
    @Select("SELECT * FROM adoption_app WHERE org_id = #{orgId} AND status = #{status}")
    List<AdoptionApp> selectByOrgIdAndStatus(@Param("orgId") Long orgId, @Param("status") String status);

    // 查询平台待审核的申请（状态为 "ORG_APPROVED 机构管理员审核通过"）
    @Select("SELECT * FROM adoption_app WHERE status = 'ORG_APPROVED'")
    List<AdoptionApp> selectPendingPlatformApplications();

    // 更新领养申请状态
    @Update("UPDATE adoption_app SET status = #{status}, reject_reason = #{rejectReason}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateStatus(AdoptionApp adoptionApp);

    // 更新领养申请为已领养状态（即已通过）
    @Update("UPDATE adoption_app SET status = 'APPROVED', updated_at = #{updatedAt} WHERE id = #{id}")
    int updateToAdopted(Long id);

    // 查询已领养的宠物（状态为 "COMPLETED 已完成交接"）
    @Select("SELECT * FROM adoption_app WHERE applicant_id = #{applicantId} AND status = 'COMPLETED'")
    List<AdoptionApp> selectAdoptedPets(Long applicantId);

    // 检查是否存在重复申请（同一用户对同一宠物的未完成申请）
    @Select("SELECT COUNT(*) > 0 FROM adoption_app WHERE applicant_id = #{applicantId} AND pet_id = #{petId} " +
            "AND status NOT IN ('ORG_REJECTED', 'PLATFORM_REJECTED', 'COMPLETED')")
    boolean existsPendingApplication(@Param("applicantId") Long applicantId, @Param("petId") Long petId);
}
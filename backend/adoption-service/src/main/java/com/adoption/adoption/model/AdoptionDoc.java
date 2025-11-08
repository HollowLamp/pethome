package com.adoption.adoption.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 领养申请表（附件文件） -- 对应数据库 adoption_doc 表
@Getter
@Setter
public class AdoptionDoc {
    private Long id; // 主键
    private Long appId; // 领养申请ID
    private String docType; // 文档类型：ID_CARD身份证, PET_PHOTO宠物照片, OTHER其他
    private String url; // 文档URL
    private LocalDateTime uploadedAt; // 上传时间

    // Constructors
    public AdoptionDoc() {}

    public AdoptionDoc(Long id, Long appId, String docType, String url, LocalDateTime uploadedAt) {
        this.id = id;
        this.appId = appId;
        this.docType = docType;
        this.url = url;
        this.uploadedAt = uploadedAt;
    }
}
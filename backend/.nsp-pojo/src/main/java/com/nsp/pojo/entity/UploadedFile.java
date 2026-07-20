package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已上传文件 — 记录所有通过 OSS 直传的图片
 */
@Data
@TableName("uploaded_file")
public class UploadedFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：AVATAR / SHOP_LOGO / PRODUCT_IMAGE */
    private String bizType;

    /** 关联的业务对象 ID（如 userId、productId） */
    private Long targetId;

    /** OSS 对象 Key（不含 bucket） */
    private String ossKey;

    /** 文件访问 URL（CDN 或签名 URL） */
    private String url;

    /** 上传者用户 ID */
    private Long uploaderId;

    /** 创建时间 */
    private LocalDateTime createdAt;

}

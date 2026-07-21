package com.nsp.server.service;

import com.nsp.pojo.dto.UploadBindDTO;
import com.nsp.pojo.vo.UploadSignVO;

/**
 * 文件上传业务接口 — OSS 服务端签名 + 前端直传
 */
public interface UploadService {

    /**
     * 生成 OSS 上传签名（前端凭此直传 OSS）
     *
     * @param userId   当前用户 ID（用于限流和目录生成）
     * @param bizType  业务类型（AVATAR / SHOP_LOGO / PRODUCT_IMAGE）
     * @param targetId 关联业务对象 ID
     * @param fileName 原始文件名（用于提取扩展名和校验类型）
     * @return 签名信息（policy、signature、host、dir）
     */
    UploadSignVO sign(Long userId, String bizType, Long targetId, String fileName);

    /**
     * 绑定上传文件（前端直传成功后回写 ossKey）
     */
    void bind(Long userId, UploadBindDTO dto);

    /**
     * 获取文件临时访问 URL（私有 Bucket 需签名）
     */
    String getAccessUrl(String ossKey);

    /**
     * OSS 回调处理（校验回调签名后落库）
     */
    void handleCallback(String ossKey, String bizType, Long targetId, Long uploaderId);

}

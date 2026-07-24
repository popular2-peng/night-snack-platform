package com.nsp.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.enums.UploadBizType;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.UploadBindDTO;
import com.nsp.pojo.entity.UploadedFile;
import com.nsp.pojo.vo.UploadSignVO;
import com.nsp.server.mapper.UploadedFileMapper;
import com.nsp.server.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传业务实现 — OSS 服务端签名 + 前端直传
 *
 * <p>⚡ 安全措施：类型白名单、大小限制、签名限流
 */
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int SIGN_EXPIRE_SECONDS = 60;          // 签名有效期 60s
    private static final int SIGN_LIMIT_PER_MINUTE = 10;        // 每分钟最多 10 次签名
    private static final int ACCESS_URL_EXPIRE_MINUTES = 30;    // 临时访问 URL 30 分钟

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.cdn-domain}")
    private String cdnDomain;

    private final OSS ossClient;
    private final UploadedFileMapper uploadedFileMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UploadSignVO sign(Long userId, String bizType, Long targetId, String fileName) {
        // 限流
        String limitKey = "upload:sign:limit:" + userId;
        String countStr = redisTemplate.opsForValue().get(limitKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= SIGN_LIMIT_PER_MINUTE) {
            throw new BizException(ErrorCode.UPLOAD_SIGN_LIMIT,
                    "签名请求过于频繁，每分钟限 " + SIGN_LIMIT_PER_MINUTE + " 次");
        }
        redisTemplate.opsForValue().increment(limitKey);
        redisTemplate.expire(limitKey, 1, TimeUnit.MINUTES);

        // 校验文件类型
        String ext = getFileExtension(fileName);
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BizException(ErrorCode.UPLOAD_TYPE_INVALID,
                    "不支持的文件类型: " + ext + "，允许: " + ALLOWED_TYPES);
        }

        // 校验业务类型
        UploadBizType type = UploadBizType.fromCode(bizType);
        if (type == null) {
            throw new BizException(ErrorCode.UPLOAD_TYPE_INVALID, "不支持的业务类型: " + bizType);
        }

        // 生成上传目录: {项目根}/{yyyy-MM}/{dd}/{uuid}.{ext}
        LocalDateTime now = LocalDateTime.now();
        String monthDir = String.format("%d-%02d", now.getYear(), now.getMonthValue());
        String dayDir = String.format("%02d", now.getDayOfMonth());
        String dir = "nsp/" + monthDir + "/" + dayDir + "/";
        String objectKey = dir + UUID.randomUUID() + "." + ext;

        // 生成 Policy + Signature
        try {
            long expireEndTime = System.currentTimeMillis() + SIGN_EXPIRE_SECONDS * 1000L;
            Date expiration = new Date(expireEndTime);

            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 1, MAX_FILE_SIZE);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            String host = "https://" + bucket + "." + endpoint;

            UploadSignVO vo = new UploadSignVO();
            vo.setAccessKeyId(accessKeyId);
            vo.setPolicy(encodedPolicy);
            vo.setSignature(postSignature);
            vo.setHost(host);
            vo.setDir(dir);
            vo.setExpire(SIGN_EXPIRE_SECONDS);

            log.info("生成上传签名: userId={}, bizType={}, dir={}", userId, bizType, dir);
            return vo;
        } catch (Exception e) {
            log.error("生成 OSS 签名失败", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "上传签名生成失败");
        }
    }

    @Override
    public void bind(Long userId, UploadBindDTO dto) {
        UploadedFile file = new UploadedFile();
        file.setBizType(dto.getBizType());
        file.setTargetId(dto.getTargetId());
        file.setOssKey(dto.getOssKey());
        file.setUrl(buildUrl(dto.getOssKey()));
        file.setUploaderId(userId);
        uploadedFileMapper.insert(file);
        log.info("文件绑定成功: ossKey={}, bizType={}, userId={}", dto.getOssKey(), dto.getBizType(), userId);
    }

    @Override
    public String getAccessUrl(String ossKey) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + ACCESS_URL_EXPIRE_MINUTES * 60 * 1000L);
            URL url = ossClient.generatePresignedUrl(bucket, ossKey, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("生成 OSS 访问 URL 失败: ossKey={}", ossKey, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "获取文件访问链接失败");
        }
    }

    @Override
    public void handleCallback(String ossKey, String bizType, Long targetId, Long uploaderId) {
        UploadedFile file = new UploadedFile();
        file.setBizType(bizType);
        file.setTargetId(targetId);
        file.setOssKey(ossKey);
        file.setUrl(buildUrl(ossKey));
        file.setUploaderId(uploaderId);
        uploadedFileMapper.insert(file);
        log.info("OSS 回调落库: ossKey={}, bizType={}", ossKey, bizType);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String buildUrl(String ossKey) {
        if (cdnDomain != null && !"placeholder".equals(cdnDomain) && !cdnDomain.isBlank()) {
            return "https://" + cdnDomain + "/" + ossKey;
        }
        return "https://" + bucket + "." + endpoint + "/" + ossKey;
    }

}

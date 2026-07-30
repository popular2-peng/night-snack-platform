package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台品类 — 平台级商家分类基础数据（如"烧烤类""小龙虾""奶茶饮品"）
 *
 * <p>对应 category 表，用于：
 * <ul>
 *   <li>商家主营品类选择</li>
 *   <li>用户端品类导航/筛选</li>
 *   <li>商家入驻审核时的品类分配</li>
 * </ul>
 */
@Data
@TableName("category")
public class PlatformCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品类名称（2-10 字） */
    private String name;

    /** 图标（Emoji 字符或 OSS URL） */
    private String icon;

    /** 图标类型：EMOJI / OSS */
    private String iconType;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 状态：ENABLED / DISABLED */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 软删标记：0-否 1-是 */
    private Integer deleted;

}

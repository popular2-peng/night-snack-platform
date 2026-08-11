package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.common.util.PhoneMaskUtil;
import com.nsp.pojo.dto.BanUserDTO;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.pojo.vo.UserDetailVO;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理业务实现
 */
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PageVO<AdminUserVO> listUsers(int page, int size, String keyword,
                                          String status, String dateStart, String dateEnd) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键字搜索：用户名 / 手机号
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                            .or().like(User::getPhone, keyword));
        }
        // 状态筛选
        if (status != null && !status.isBlank()) {
            wrapper.eq(User::getStatus, status.toUpperCase());
        }
        // 注册时间范围
        if (dateStart != null && !dateStart.isBlank()) {
            wrapper.ge(User::getCreatedAt, LocalDateTime.parse(dateStart + "T00:00:00"));
        }
        if (dateEnd != null && !dateEnd.isBlank()) {
            wrapper.le(User::getCreatedAt, LocalDateTime.parse(dateEnd + "T23:59:59"));
        }

        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查询真实订单统计（替代 user 表中的缓存列）
        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, Map<String, Object>> statsMap = Map.of();
        if (!userIds.isEmpty()) {
            List<Map<String, Object>> orderStats = orderMapper.selectUserOrderStats(userIds);
            statsMap = orderStats.stream()
                    .collect(Collectors.toMap(
                            m -> (Long) m.get("user_id"),
                            m -> m
                    ));
        }
        final Map<Long, Map<String, Object>> finalStatsMap = statsMap;

        List<AdminUserVO> list = result.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtil.copyProperties(u, vo);
            vo.setPhone(PhoneMaskUtil.mask(u.getPhone()));
            // 用 order 表真实统计覆盖 user 表中的缓存值
            Map<String, Object> stats = finalStatsMap.get(u.getId());
            if (stats != null) {
                vo.setTotalOrders(((Number) stats.get("order_count")).intValue());
                vo.setTotalSpent(new java.math.BigDecimal(stats.get("total_spent").toString()));
            } else {
                vo.setTotalOrders(0);
                vo.setTotalSpent(java.math.BigDecimal.ZERO);
            }
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public UserDetailVO getUserDetail(Long id) {
        User user = findById(id);
        UserDetailVO vo = new UserDetailVO();
        BeanUtil.copyProperties(user, vo);
        vo.setPhone(PhoneMaskUtil.mask(user.getPhone()));

        // 从 order 表获取真实订单统计
        List<Map<String, Object>> stats = orderMapper.selectUserOrderStats(List.of(id));
        if (!stats.isEmpty()) {
            Map<String, Object> s = stats.get(0);
            vo.setTotalOrders(((Number) s.get("order_count")).intValue());
            vo.setTotalSpent(new java.math.BigDecimal(s.get("total_spent").toString()));
        } else {
            vo.setTotalOrders(0);
            vo.setTotalSpent(java.math.BigDecimal.ZERO);
        }
        return vo;
    }

    @Override
    @Transactional
    public void banUser(Long id, Long operatorId, BanUserDTO dto) {
        User user = findById(id);

        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_ALREADY_BANNED, MessageConst.USER_ALREADY_BANNED_MSG);
        }
        if (dto.getDurationDays() != null && dto.getDurationDays() < 0) {
            throw new BizException(ErrorCode.USER_BAN_DURATION_INVALID, MessageConst.USER_BAN_DURATION_INVALID_MSG);
        }

        user.setStatus("DISABLED");
        user.setBannedReason(dto.getReason());
        user.setBannedAt(LocalDateTime.now());
        user.setBannedBy(operatorId);
        // 有时长 → 计算解封时间；null → 永久
        if (dto.getDurationDays() != null) {
            user.setUnbannedAt(LocalDateTime.now().plusDays(dto.getDurationDays()));
        } else {
            user.setUnbannedAt(null); // 永久封禁
        }
        userMapper.updateById(user);

        writeOperationLogAsync(user.getId(), "BAN", operatorId, dto.getReason());

        log.info("管理员封禁用户: userId={}, operatorId={}, reason={}, durationDays={}",
                id, operatorId, dto.getReason(), dto.getDurationDays());
    }

    @Override
    @Transactional
    public void unbanUser(Long id, Long operatorId) {
        User user = findById(id);

        if (!"DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_NOT_BANNED, MessageConst.USER_NOT_BANNED_MSG);
        }

        user.setStatus("ACTIVE");
        user.setBannedReason(null);
        user.setBannedAt(null);
        user.setBannedBy(null);
        user.setUnbannedAt(LocalDateTime.now());
        user.setUnbannedBy(operatorId);
        userMapper.updateById(user);

        writeOperationLogAsync(user.getId(), "UNBAN", operatorId, "管理员解封");

        log.info("管理员解封用户: userId={}, operatorId={}", id, operatorId);
    }

    // ──────────────────────────────────────
    // 批量操作（选中若干条，不满足条件的跳过并汇总）
    // ──────────────────────────────────────

    @Override
    @Transactional
    public BatchResultVO batchBan(List<Long> ids, Long operatorId) {
        BatchResultVO result = new BatchResultVO();
        int banned = 0, deleted = 0;
        for (Long id : ids) {
            User user = userMapper.selectById(id); // 逻辑删除的用户查不到 → null
            if (user == null) { deleted++; continue; }
            if ("DISABLED".equals(user.getStatus())) { banned++; continue; }

            user.setStatus("DISABLED");
            user.setBannedReason("批量封禁");
            user.setBannedAt(LocalDateTime.now());
            user.setBannedBy(operatorId);
            user.setUnbannedAt(null); // 永久封禁
            userMapper.updateById(user);
            result.setSuccessCount(result.getSuccessCount() + 1);
            writeOperationLogAsync(user.getId(), "BAN", operatorId, "批量封禁");
        }
        result.setSkippedCount(banned + deleted);
        if (banned > 0) result.addSkipped(banned + " 个账号已封禁");
        if (deleted > 0) result.addSkipped(deleted + " 个账号已删除");
        log.info("批量封禁用户: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public BatchResultVO batchUnban(List<Long> ids, Long operatorId) {
        BatchResultVO result = new BatchResultVO();
        int active = 0, deleted = 0;
        for (Long id : ids) {
            User user = userMapper.selectById(id);
            if (user == null) { deleted++; continue; }
            if (!"DISABLED".equals(user.getStatus())) { active++; continue; }

            user.setStatus("ACTIVE");
            user.setBannedReason(null);
            user.setBannedAt(null);
            user.setBannedBy(null);
            user.setUnbannedAt(LocalDateTime.now());
            user.setUnbannedBy(operatorId);
            userMapper.updateById(user);
            result.setSuccessCount(result.getSuccessCount() + 1);
            writeOperationLogAsync(user.getId(), "UNBAN", operatorId, "批量解封");
        }
        result.setSkippedCount(active + deleted);
        if (active > 0) result.addSkipped(active + " 个账号未封禁");
        if (deleted > 0) result.addSkipped(deleted + " 个账号已删除");
        log.info("批量解封用户: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public BatchResultVO batchDelete(List<Long> ids, Long operatorId) {
        BatchResultVO result = new BatchResultVO();
        int active = 0, deleted = 0;
        for (Long id : ids) {
            User user = userMapper.selectById(id);
            if (user == null) { deleted++; continue; }
            // 启用账号不可删除（仅允许删除已封禁账号）
            if ("ACTIVE".equals(user.getStatus())) { active++; continue; }

            user.setDeleted(1); // 软删（MyBatis-Plus 逻辑删除字段）
            userMapper.updateById(user);
            result.setSuccessCount(result.getSuccessCount() + 1);
            writeOperationLogAsync(user.getId(), "DELETE", operatorId, "批量删除");
        }
        result.setSkippedCount(active + deleted);
        if (active > 0) result.addSkipped(active + " 个启用账号不可删除");
        if (deleted > 0) result.addSkipped(deleted + " 个账号已删除");
        log.info("批量删除用户: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public String resetPassword(Long id, Long operatorId) {
        User user = findById(id);

        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_RESET_PASSWORD_FAIL, MessageConst.USER_RESET_PASSWORD_BANNED_MSG);
        }

        // 生成 8 位随机密码（字母+数字）
        String newPassword = RandomUtil.randomString(8);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        writeOperationLogAsync(user.getId(), "RESET_PASSWORD", operatorId, "管理员重置密码");

        log.info("管理员重置密码: userId={}, operatorId={}", id, operatorId);
        return newPassword;
    }

    @Override
    public List<Map<String, Object>> getOperationLogs(Long id) {
        User user = findById(id);
        List<Map<String, Object>> logs = new ArrayList<>();

        // 注册
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("time", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        created.put("action", "注册");
        created.put("detail", "用户注册");
        logs.add(created);

        // 封禁记录
        if (user.getBannedAt() != null) {
            Map<String, Object> banned = new LinkedHashMap<>();
            banned.put("time", user.getBannedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            banned.put("action", "封禁");
            banned.put("detail", user.getBannedReason());
            logs.add(banned);
        }
        // 解封记录
        if (user.getUnbannedAt() != null) {
            Map<String, Object> unbanned = new LinkedHashMap<>();
            unbanned.put("time", user.getUnbannedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            unbanned.put("action", "解封");
            unbanned.put("detail", "管理员解封");
            logs.add(unbanned);
        }
        // 最后登录
        if (user.getLastLoginAt() != null) {
            Map<String, Object> login = new LinkedHashMap<>();
            login.put("time", user.getLastLoginAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            login.put("action", "登录");
            login.put("detail", "用户登录");
            logs.add(login);
        }

        logs.sort((a, b) -> {
            Object ta = a.get("time");
            Object tb = b.get("time");
            if (ta == null || tb == null) return 0;
            return ((Comparable) tb).compareTo(ta);
        });
        return logs;
    }

    // ---- 内部方法 ----

    private User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_NOT_FOUND_MSG);
        return user;
    }

    /** 异步写操作日志（当前阶段打 log，后续建表后补全） */
    @Async
    void writeOperationLogAsync(Long userId, String action, Long operatorId, String detail) {
        log.info("[用户操作日志] userId={}, action={}, operatorId={}, detail={}",
                userId, action, operatorId, detail);
    }

}

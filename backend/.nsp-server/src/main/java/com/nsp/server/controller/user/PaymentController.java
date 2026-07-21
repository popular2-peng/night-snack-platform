package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.PayRequestDTO;
import com.nsp.pojo.vo.PayVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端支付接口
 */
@Tag(name = "支付", description = "发起支付、查询支付状态")
@RestController
@RequestMapping("/api/user/pay")
@RequiredArgsConstructor
@RequireRole("USER")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "发起支付")
    @PostMapping
    public Result<PayVO> pay(@Valid @RequestBody PayRequestDTO dto,
                             @AuthenticationPrincipal Long userId) {
        return Result.ok(paymentService.pay(userId, dto));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{orderNo}/status")
    public Result<PayVO> status(@PathVariable String orderNo) {
        return Result.ok(paymentService.queryStatus(orderNo));
    }

}

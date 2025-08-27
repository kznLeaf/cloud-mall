package com.hmall.trade.listener;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/26 21:02</p>
 * Description: 消费者，接收消息
 */
@Component
@RequiredArgsConstructor
public class PayStatusListener {

    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId) {
        // 查询老订单
        Order orderServiceById = orderService.getById(orderId);
        // 判断订单状态
        if (orderServiceById == null || orderServiceById.getStatus() != 1) {
            // 不是未支付
            return;
        }
        // 在订单未支付的状态下，标记订单状态为已支付
        orderService.markOrderPaySuccess(orderId);
    }
}

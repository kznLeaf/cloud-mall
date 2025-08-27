package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.contrants.MQcontrants;
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
 * <p>Date: 2025/8/27 21:31</p>
 * Description:
 * <p>
 * 更新订单状态的兜底方案
 * </p>
 * 交易服务用来提醒自己的延迟队列。收到消息时，先查本地订单的状态，
 * 未支付的话在远程调用 queryPayOrders 查流水支付状态。
 * 用户支付成功后可能会出现【订单显示未支付】的情况，所以本地状态为未支付，实际可能是已经支付了，
 * 所以要查两次。
 */
@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQcontrants.DELAY_EXCHANGE_NAME),
            exchange = @Exchange(name = MQcontrants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MQcontrants.DELAY_ORDER_KEY
    ))
    public void listenDelay(Long OrderId) {
        // 查询本地订单状态
        Order order = orderService.getById(OrderId);
        // 判断是否已支付
        if(order == null || order.getStatus() != 1) {
            // 订单不存在，或者已经支付
            return;
        }
        // 本地是未付款
        // 未支付，再查支付流水，双重保障
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(OrderId);
        if(payOrderDTO != null && payOrderDTO.getStatus() == 3) {
            orderService.markOrderPaySuccess(OrderId);
        } else {
            // 未支付 取消订单 恢复库存
            orderService.cancelOrder(OrderId);
        }
    }
}

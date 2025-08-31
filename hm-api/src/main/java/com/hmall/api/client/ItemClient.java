package com.hmall.api.client;

import com.hmall.api.client.fallback.ItemClientFallbackFactory;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/23 22:10</p>
 * Description: 实现远程调用 item 服务，用于查询最新商品详细信息
 */

@FeignClient(value = "item-service", fallbackFactory = ItemClientFallbackFactory.class)
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);

    /**
     * 根据ID查询商品
     *
     * @param id
     * @return
     */
    @GetMapping("/items/{id}")
    ItemDTO queryItemById(@PathVariable("id") Long id);

}


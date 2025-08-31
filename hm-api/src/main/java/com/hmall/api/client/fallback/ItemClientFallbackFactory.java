package com.hmall.api.client.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/25 22:00</p>
 * Description:
 */
@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("Fallback: 查询失败" + ids);
                return Collections.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("Fallback: 扣减库存失败");
                throw new RuntimeException();
            }

            @Override
            public ItemDTO queryItemById(Long id) {
                log.error("Fallback: queryItemById失败");
                return null;
            }
        };
    }
}

package com.hmall.search.listener;

import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/31 15:44</p>
 * Description: 触发来源：com.hmall.item.controller
 * <p>
 * 用于 mysql和 es 的同步，每当商品服务对商品实现增删改时，索引库的数据也需要同步更新。
 * 增删改的监听器共用一个交换机，三个操作各对应不同的队列，每个队列对应一个key
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemListener {

    private final ItemClient itemClient;

    private final RestHighLevelClient client =
            new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.199.131:9200")));

    private static final String ES_EXCHANGE = "item.direct";

    private static final String ADD_ITEM = "item.add";

    private static final String UPDATE_ITEM = "item.update";

    private static final String DELETE_ITEM = "item.delete";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "es.add.queue"),
            exchange = @Exchange(name = ES_EXCHANGE),
            key = ADD_ITEM
    ))
    public void addItem(Long itemId) {
        ItemDTO itemDTO = null;

        try {
            itemDTO = itemClient.queryItemById(itemId);
        } catch (Exception e) {
            log.error("itemDTO转化失败！");
            throw new RuntimeException(e);
        }

        if (itemDTO == null) {
            return;
        }
        ItemDoc itemDoc = new ItemDoc();
        BeanUtils.copyProperties(itemDTO, itemDoc);
        IndexRequest request = new IndexRequest("items").id(String.valueOf(itemId));
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        request.source(jsonStr, XContentType.JSON);
        try {
            client.index(request, RequestOptions.DEFAULT);
            log.debug("成功新增商品，详情：{}", jsonStr);
        } catch (IOException e) {
            log.error("es新增失败" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "es.update.queue"),
            exchange = @Exchange(name = ES_EXCHANGE),
            key = UPDATE_ITEM
    ))
    public void updateItem(Long itemId) {
        ItemDTO itemDTO = itemClient.queryItemById(itemId);
        if (itemDTO == null) {
            return;
        }
        // 转换对象
        ItemDoc itemDoc = new ItemDoc();
        BeanUtils.copyProperties(itemDTO, itemDoc);
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        UpdateRequest request = new UpdateRequest("items", String.valueOf(itemId));
        // 准备参数
        request.doc(jsonStr, XContentType.JSON);
        // 发送请求
        try {
            client.update(request, RequestOptions.DEFAULT);
            log.debug("成功更新es商品信息, 详情:{}", jsonStr);
        } catch (IOException e) {
            log.error("es新增失败" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "es.delete.queue"),
            exchange = @Exchange(name = ES_EXCHANGE),
            key = DELETE_ITEM
    ))
    public void deleteItem(Long itemId) {
        DeleteRequest request = new DeleteRequest("items", String.valueOf(itemId));
        try {
            client.delete(request, RequestOptions.DEFAULT);
            log.debug("成功删除商品id: {}", itemId);
        } catch (IOException e) {
            log.error("删除失败！商品id: " + itemId);
            throw new RuntimeException(e);
        }
    }
}

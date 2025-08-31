package com.hmall.search.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final RestHighLevelClient client =
            new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.199.131:9200")));

    @ApiOperation("搜索商品")
    @GetMapping("/{id}")
    public ItemDTO search(@PathVariable("id") Long id) throws IOException {
        // 准备request
        GetRequest getRequest = new GetRequest("items", id.toString());
        // 发送请求
        GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
        // 解析结果
        String item = response.getSourceAsString();

        ItemDoc itemDoc = JSONUtil.toBean(item, ItemDoc.class);

        return BeanUtil.copyProperties(itemDoc, ItemDTO.class);
    }
}

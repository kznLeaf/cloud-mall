package com.hmall.item.es;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/29 15:01</p>
 * Description:
 */
@SpringBootTest(properties = "spring.profiles.active=local")
public class es {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    @Test
    void testConnection() throws IOException {
        System.out.println(client);
    }

    @BeforeEach
    void init() throws IOException {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.199.131:9200")));
    }

    @AfterEach
    void close() throws IOException {
        client.close();
    }

    @Test
    void testQuery() throws IOException {
        GetIndexRequest indexRequest = new GetIndexRequest("items");
        boolean exists = client.indices().exists(indexRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    @Test
    void testIndex() throws IOException {
        // 准备文档数据

        IndexRequest request = new IndexRequest("items").id("1");
        request.source("{}", XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testAddDocument() throws IOException {
        // 1. 构建 JSON 文档
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", "1");
        doc.put("name", "iPhone 15 Pro");
        doc.put("price", 9999);
        doc.put("stock", 100);
        doc.put("image", "http://example.com/iphone.jpg");
        doc.put("category", "手机");
        doc.put("brand", "Apple");
        doc.put("sold", 20);
        doc.put("commentCount", 5);
        doc.put("isAD", false);
        doc.put("updateTime", "2025-08-31T10:00:00");

        // 2. 创建请求对象
        IndexRequest request = new IndexRequest("items").id("1").source(doc);

        // 3. 发送请求
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        System.out.println(response.getResult()); // CREATED / UPDATED
    }

    @Test
    void testQueryByDocId() throws IOException {
        // 2. 创建 GetRequest（索引名称 + 文档 ID）
        GetRequest getRequest = new GetRequest("items", "1"); // ES 7.x 不需要 type

        // 3. 执行查询
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);

        // 4. 判断文档是否存在
        if (getResponse.isExists()) {
            System.out.println("文档内容: " + getResponse.getSourceAsString());
        } else {
            System.out.println("文档不存在");
        }
    }

    @Test
    void testDataBase() {
        Item byId = itemService.getById(317578);
        System.out.println(byId);
    }

    /**
     * 测试分批将数据库中的所有记录插入es
     *
     * @throws IOException 抛出异常
     *                     <p>
     *                     效率更高的导入方式。现在数据库中一共有88476条记录，可以考虑每次导入500条，一共导入177次。
     */
    @Test
    void testBatch() throws IOException {
        int pageNo = 1;
        int pageSize = 1000;

        while (true) {
            // 查询文档数据
            Page<Item> pages = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1) // 正常商品状态
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = pages.getRecords();
            if (records == null || records.isEmpty()) return;

            BulkRequest bulkRequest = new BulkRequest();

            // 转换
            for (Item item : records) {
                ItemDoc itemDoc = new ItemDoc();
                BeanUtils.copyProperties(item, itemDoc);
                bulkRequest.add(new IndexRequest("items")
                        .id(String.valueOf(item.getId()))
                        .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON)
                );
            }
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                for (BulkItemResponse itemResponse : response.getItems()) {
                    if (itemResponse.isFailed()) {
                        System.err.println(itemResponse.getFailureMessage());
                    }
                }
            }

            pageNo++;
        }

    }


}

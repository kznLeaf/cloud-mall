package com.hmall.item.es;

import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/9/1 10:13</p>
 * Description:
 */
//@SpringBootTest(properties = "spring.profiles.active=local")

public class ESTest {

    private RestHighLevelClient client;

    @BeforeEach
    void init() throws IOException {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.199.131:9200")));
    }

    @AfterEach
    void close() throws IOException {
        client.close();
    }

    @Test
    void testMatchAll() throws IOException {
        /*
        GET /items/_search
        {
          "query": {
            "match_all": {}
          }
        }
        * */
        SearchRequest searchRequest = new SearchRequest("items");
        searchRequest.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        // 解析结果
        parseResult(response);
    }

    private static void parseResult(SearchResponse response) {
        SearchHits hits = response.getHits();

        SearchHit[] hits1 = hits.getHits();

        for (SearchHit hit : hits1) {
            // 转换为 doc 对象
            ItemDTO bean = JSONUtil.toBean(hit.getSourceAsString(), ItemDTO.class);
            System.out.println(bean);
        }
    }

    @Test
    void testMatch() throws IOException {
        SearchRequest searchRequest = new SearchRequest("items");
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", "露娜");
        searchRequest.source().query(matchQueryBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        parseResult(response);
    }

    @Test
    void testBool() throws IOException {
        SearchRequest searchRequest = new SearchRequest("items");
        searchRequest.source().query(
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand", "德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
        );

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println();
        parseResult(response);
        System.out.println();
    }

    @Test
    void testSort() throws IOException {

        // 前端传来当前第几页和一页的大小
        int pageNo = 1;
        int pageSize = 10;
        // (pageNo - 1)* pageSize

        SearchRequest searchRequest = new SearchRequest("items");

        searchRequest.source().query(QueryBuilders.matchAllQuery());
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        searchRequest.source()
                .sort("sold", SortOrder.DESC)
                .sort("price", SortOrder.ASC);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        parseResult(response);
    }
}

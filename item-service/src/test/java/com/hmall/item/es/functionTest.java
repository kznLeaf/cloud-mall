package com.hmall.item.es;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/9/1 16:33</p>
 * Description:
 */
public class functionTest {
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
    void test() throws IOException {

    }
}

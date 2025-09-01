package com.hmall.search.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.management.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final RestHighLevelClient client =
            new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.199.131:9200")));

    @ApiOperation("根据id搜索商品")
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

    @ApiOperation("搜索商品list")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        SearchRequest searchRequest = new SearchRequest("items");
        // 组织DSL参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        // 根据条件拼接请求
        if (StringUtils.hasLength(query.getKey())) {
            // 关键字搜索
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StringUtils.hasLength(query.getCategory())) {
            // 分类过滤
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StringUtils.hasLength(query.getBrand())) {
            // 品牌过滤
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (query.getMaxPrice() != null) {
            // 价格最大值
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }
        if (query.getMinPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
//        下面的查询已经包含在后面的算分查询，作为主查询使用，所以这里没必要写了
//        searchRequest.source().query(bool);

        // 分页
        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);

        // 广告优先，如果 isAD字段为1，那么加上100的权重
        searchRequest.source().query(QueryBuilders.functionScoreQuery(bool,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD", true),
                                ScoreFunctionBuilders.weightFactorFunction(100)
                        )
                }).boostMode(CombineFunction.MULTIPLY));

        // 广告优先，其次再按时间/用户指定排序

        // 按照`update_time`降序排序
        // 如果指定了排序的字段，就按照指定字段进行排序；否则默认按照更新时间进行排序
/*        if (StringUtils.hasLength(query.getSortBy())) {
            searchRequest.source().sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        } else {
            searchRequest.source().sort("updateTime", query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        }*/

        // ⚠️ 这里不要直接覆盖掉 _score 排序，而是【二级排序】
        if (StringUtils.hasLength(query.getSortBy())) {
            searchRequest.source()
                    .sort("_score", SortOrder.DESC)  // 先按广告优先
                    .sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        } else {
            searchRequest.source()
                    .sort("_score", SortOrder.DESC)  // 先按广告优先
                    .sort("updateTime", query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
        }

        // 发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // 解析结果
        return parseResponseResult(response, query, pageSize.longValue());
    }

    /**
     * 转化结果的方法
     *
     * @param response es返回的响应
     * @param query    前端传来的查询类，包含了各种查询参数
     * @param pageSize 每一页的大小
     * @return 封装好的页数据传输对象
     */
    private PageDTO<ItemDTO> parseResponseResult(SearchResponse response, ItemPageQuery query, Long pageSize) {
        List<ItemDoc> itemDocList = new ArrayList<>();

        SearchHits searchHits = response.getHits(); // 外层 hits

        if (searchHits.getTotalHits() == null) return null;
        // 与查询条件匹配的总记录数，后续分页是在查出来所有匹配记录的基础上进行的。
        long total = searchHits.getTotalHits().value;

        SearchHit[] hits = searchHits.getHits();

        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            ItemDoc doc = JSONUtil.toBean(sourceAsString, ItemDoc.class);
            itemDocList.add(doc);
        }
        // 直接查出来的是ItemDoc类型的数据，但是接口要求返回ItemDTO类型的数据用于传输
        List<ItemDTO> itemDTOs = BeanUtil.copyToList(itemDocList, ItemDTO.class);

        // 计算总页数
        long pages = (total + pageSize - 1) / pageSize;
        log.debug("total: {}, pages: {}", total, pages);
        // 传入参数：总记录数 总页数 包含【当前页】的所有记录的列表（按需查询，每次查出来一页）
        return new PageDTO<>(total, pages, itemDTOs);
    }

    /**
     * 统计查询结果的品牌和分类信息，先做一次 match 全文检索，再对品牌和类别进行聚合
     *
     * @param query 前端传来的参数
     * @return 响应
     */
    @ApiOperation("聚合查询商品")
    @PostMapping("/filters")
    public Map<String, List<String>> AggQuery(@RequestBody ItemPageQuery query) {
        SearchRequest request = new SearchRequest("items");
        // 准备请求参数
        // 先匹配关键词
        request.source().query(QueryBuilders.matchQuery("name", query.getKey()));
        request.source().size(0);

        // 聚合条件，一共有两个
        String brandAgg = "brand_agg";
        String categoryAgg = "category_agg";
        request.source().aggregation(
                AggregationBuilders.terms(brandAgg).field("brand").size(10)
        );          // 保留前 10个品牌
        request.source().aggregation(
                AggregationBuilders.terms(categoryAgg).field("category").size(10)
        );
        // 发送请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("聚合查询发送请求失败！");
            throw new RuntimeException(e);
        }

        // 解析结果
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> brandBuckets = brandTerms.getBuckets();
        Terms categoryTerms = aggregations.get("category_agg");
        List<? extends Terms.Bucket> categoryBuckets = categoryTerms.getBuckets();
        // 遍历获取每一个bucket
        List<String> brandList = new ArrayList<>();
        List<String> categoryList = new ArrayList<>();
        for (Terms.Bucket brandBucket : brandBuckets) {
            brandList.add(brandBucket.getKeyAsString());
        }
        for (Terms.Bucket categoryBucket : categoryBuckets) {
            categoryList.add(categoryBucket.getKeyAsString());
        }
        // 储存结果到map中
        Map<String, List<String>> map = new HashMap<>();
        map.put("brand", brandList);
        map.put("category", categoryList);

        log.debug("brandList: {}", brandList);
        log.debug("categoryList: {}", categoryList);
        return map;
    }

}

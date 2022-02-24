import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class Elasticsearch {

	public static void main(String[] args) {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(QueryBuilders.matchAllQuery())
    			.size(10000)
    			.timeout(new TimeValue(10, TimeUnit.SECONDS));
		SearchRequest searchRequest = new SearchRequest("logs").source(searchSourceBuilder) ;
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http"))) ;
		
		try {
			SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT) ;
			int i=0;
			if (searchResponse.getHits().getTotalHits().value > 0)
				for(SearchHit hit : searchResponse.getHits())
					System.out.println("Index:"+i+++hit.getSourceAsMap());
		} 
		
		catch (IOException e) { e.printStackTrace(); }
		
	}

}

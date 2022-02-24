import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class EventLog {

	static {
		System.loadLibrary("EventLog");
	}

	public native Properties[] takeLogs();

	public static void main(String[] args) throws InterruptedException, IOException {

		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")));

		WrapperQueue queue = new WrapperQueue(1000);

		Properties[] records;
		records = new EventLog().takeLogs();
		Runnable producer = () -> {
			for (Properties record : records) {

				queue.put(record.toString());

			}
		};
		new Thread(producer).start();

		Runnable consumer = () -> {
			
			int count = records.length;
			BulkRequest request = new BulkRequest();
			IndexRequest indexRequest;
			for(int i=1;i<=records.length;i++) {
				System.out.println("consuming:" + i);
				String val = queue.take();
				indexRequest = new IndexRequest("eventlogs").id(i + "").source("Index:" + i, val);
				request.add(indexRequest);
//			IndexResponse indexResponse=client.index(indexRequest,RequestOptions.DEFAULT);
//        	System.out.println("ResponseId:"+indexResponse.getIndex());
				
			}
			try {
				BulkResponse bulkresp = client.bulk(request, RequestOptions.DEFAULT);
				System.out.println(bulkresp.status().toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
		new Thread(consumer).start();
	}
}

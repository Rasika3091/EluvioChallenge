package com.eluvio.challenge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableMap;

class EluvioClientTest {
	private static final Map<String, String> TEST_DATA = new ImmutableMap.Builder<String, String>()
			.put("id1", "item1").put("id2", "item2").put("id3", "item3").put("id4", "item4").put("id5", "item5")
			.put("id6", "item6").put("id7", "item7").put("id8", "item8").put("id9", "item9").put("id10", "item10")
			.put("id11", "item11").put("id12", "item12").put("id13", "item13").build();
	private HttpClient _httpClient;

	@Test
	void testGet() throws Exception {
		String testId = "fake_id";
		String testItem = "fake_item";
		_httpClient = mock(HttpClient.class);
		HttpResponse<Object> httpResponse = mock(HttpResponse.class);
		when(httpResponse.statusCode()).thenReturn(200);
		when(httpResponse.body()).thenReturn(testItem);

		when(_httpClient.send(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(httpResponse);
		ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

		EluvioClient client = new EluvioClientImpl(_httpClient);

		Optional<String> item = client.get(testId);

		verify(_httpClient).send(requestCaptor.capture(), ArgumentMatchers.eq(HttpResponse.BodyHandlers.ofString()));

		HttpRequest actualRequest = requestCaptor.getValue();

		assertEquals(actualRequest.uri().toString(), EluvioClientImpl.ELUVIO_URI + testId);
		assertEquals(actualRequest.headers().firstValue("Authorization").get(),
				Base64.getEncoder().encodeToString(testId.getBytes()));

		assertEquals(item.get(), testItem);
	}

	@Test
	void testGetFailure() throws Exception {
		String testId = "fake_id";
;
		_httpClient = mock(HttpClient.class);
		HttpResponse<Object> httpResponse = mock(HttpResponse.class);
		when(httpResponse.statusCode()).thenReturn(404);

		when(_httpClient.send(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(httpResponse);
		ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

		EluvioClient client = new EluvioClientImpl(_httpClient);

		Optional<String> item = client.get(testId);

		verify(_httpClient).send(requestCaptor.capture(), ArgumentMatchers.eq(HttpResponse.BodyHandlers.ofString()));

		HttpRequest actualRequest = requestCaptor.getValue();

		assertEquals(actualRequest.uri().toString(), EluvioClientImpl.ELUVIO_URI + testId);
		assertEquals(actualRequest.headers().firstValue("Authorization").get(),
				Base64.getEncoder().encodeToString(testId.getBytes()));

		assertTrue(item.isEmpty());
	}

	@Test
	void testBatchGet() throws Exception {
		_httpClient = mock(HttpClient.class);
		
		doAnswer(new Answer<HttpResponse<String>>() {
		    Set<String> ids = TEST_DATA.keySet();
		    Iterator<String> iterator = ids.iterator();

		    public HttpResponse<String> answer(InvocationOnMock invocation) {
		    	String id = iterator.next();
		    	HttpResponse<String> httpResponse = mock(HttpResponse.class);
				when(httpResponse.statusCode()).thenReturn(200);
				when(httpResponse.body()).thenReturn(TEST_DATA.get(id));
				return httpResponse;
			
		    }
		}).when(_httpClient).send(ArgumentMatchers.any(), ArgumentMatchers.any());
		
		EluvioClient client = new EluvioClientImpl(_httpClient);
		
		Map<String, Optional<String>> result = client.batchGet(new ArrayList<>(TEST_DATA.keySet()));
		
		assertEquals(result.size(), TEST_DATA.size());
	}

	@Test
	void testBatchGetPartialFailure() throws Exception {
		int expectedErrors = 5;
		_httpClient = mock(HttpClient.class);
		
		doAnswer(new Answer<HttpResponse<String>>() {
		    Set<String> ids = TEST_DATA.keySet();
		    Iterator<String> iterator = ids.iterator();
		    int errorsToIntroduce = expectedErrors;

		    public HttpResponse<String> answer(InvocationOnMock invocation) {
		    	String id = iterator.next();
		    	HttpResponse<String> httpResponse = mock(HttpResponse.class);
		    	if (errorsToIntroduce > 0) {
		    		errorsToIntroduce--;
		    		when(httpResponse.statusCode()).thenReturn(404);
		    		
		    	} else {
		    		when(httpResponse.statusCode()).thenReturn(200);
					when(httpResponse.body()).thenReturn(TEST_DATA.get(id));
		    	}
				return httpResponse;
			
		    }
		}).when(_httpClient).send(ArgumentMatchers.any(), ArgumentMatchers.any());
		
		EluvioClient client = new EluvioClientImpl(_httpClient);
		
		Map<String, Optional<String>> result = client.batchGet(new ArrayList<>(TEST_DATA.keySet()));
		
		assertEquals(result.size(), TEST_DATA.size());
		long outputErrors = result.values().stream().filter(v -> v.isEmpty()).count();
		assertEquals(expectedErrors, outputErrors);
	}

}

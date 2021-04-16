package com.eluvio.challenge;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class EluvioClientImpl implements EluvioClient {
	
	

	public static final String ELUVIO_URI = "https://eluv.io/items/";
	private static final Integer MAX_THREADS = 5;

	private final HttpClient httpClient;// = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
	
	public EluvioClientImpl(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	@Override
	public Optional<String> get(String id) {
		HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(ELUVIO_URI + id))
				.setHeader("Authorization", Base64.getEncoder().encodeToString(id.getBytes())).build();
		HttpResponse<String> response = null;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return Optional.empty();
		}
		if (response.statusCode() == 200) {
			return Optional.of(response.body());
		}
		return Optional.empty();
	}

	/**
	 *  Makes no more than MAX_THREADS (5) simultaneous requests to the API
	 */
	@Override
	public Map<String, Optional<String>> batchGet(List<String> ids) {
		Map<String, Optional<String>> result = new HashMap<>();

		if (ids.isEmpty())
			return result;

		ArrayList<FutureTask<Map<String, Optional<String>>>> batchGetSynchronousTasks = new ArrayList<>(MAX_THREADS);
		int sublistLength = ids.size() % MAX_THREADS == 0 ? ids.size() / MAX_THREADS : ids.size() / MAX_THREADS + 1;

		for (int i = 0; i < MAX_THREADS; i++) {
			int start = i * sublistLength;
			int end = Math.min(start + sublistLength, ids.size());
			List<String> sublist = ids.subList(start, end);

			Callable<Map<String, Optional<String>>> callable = new GetRequestJob(sublist);

			batchGetSynchronousTasks.add(i, new FutureTask<Map<String, Optional<String>>>(callable));

			Thread t = new Thread(batchGetSynchronousTasks.get(i));
			t.start();
		}

		for (int i = 0; i < MAX_THREADS; i++) {

			try {
				result.putAll(batchGetSynchronousTasks.get(i).get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	class GetRequestJob implements Callable<Map<String, Optional<String>>> {
		private final List<String> ids;

		public GetRequestJob(List<String> ids) {
			this.ids = ids;
		}

		public List<String> getIds() {
			return ids;
		}

		public Map<String, Optional<String>> call() throws Exception {
			Map<String, Optional<String>> result = new HashMap<>();
			for (String id : ids) {
				result.put(id, get(id));
			}
			return result;
		}
	}
}

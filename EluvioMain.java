package com.eluvio.challenge;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class EluvioMain {

	public static void main(String[] args) {
		HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
		EluvioClient client = new EluvioClientImpl(httpClient);
		
		List<String> ids = ImmutableList.of("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10");
		
		Map<String, Optional<String>> clientOutput = client.batchGet(ids);
		
		Map<String, String> sanitizedOutput = clientOutput.entrySet()
				.stream()
				.filter(e -> e.getValue().isPresent())
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
		
		System.out.println(sanitizedOutput);
		
	}

}

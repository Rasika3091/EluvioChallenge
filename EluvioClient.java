package com.eluvio.challenge;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EluvioClient {
	
	/**
	 * Single item get
	 * 
	 * @param id
	 * @return
	 */
	Optional<String> get(String id);
	
	/**
	 * Batch get items.
	 * 
	 * @param ids
	 * @return
	 */
	Map<String, Optional<String>> batchGet(List<String> ids);
}

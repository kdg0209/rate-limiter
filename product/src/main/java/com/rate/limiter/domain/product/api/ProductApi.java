package com.rate.limiter.domain.product.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductApi {

	@GetMapping
	public Map<String, String> hello() {
		Map<String, String> map = new HashMap<>();
		map.put("hello", "world");

		return map;
	}
}

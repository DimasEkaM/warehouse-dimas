package com.dimasekam.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockMovementSumTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void movementsSumToCurrentQuantity() throws Exception {
		long itemId = createItem("MUG-WHT");

		// restock 40
		restockItem(itemId, 40);
		// adjust down 10
		adjustItem(itemId, -10);

		var response = mockMvc.perform(get("/api/v1/stock-movements?itemId=" + itemId))
				.andExpect(status().isOk())
				.andReturn();

		String body = response.getResponse().getContentAsString();
		var tree = objectMapper.readTree(body);
		int sum = 0;
		for (var node : tree) {
			sum += node.get("delta").asInt();
		}

		mockMvc.perform(get("/api/v1/items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(sum));
	}

	private long createItem(String sku) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\": \"" + sku + "\", \"name\": \"Item " + sku
								+ "\", \"description\": \"Item " + sku + "\", \"price\": 9.99}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("id").asLong();
	}

	private void restockItem(long itemId, int delta) throws Exception {
		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"delta\": " + delta + ", \"reason\": \"RESTOCK\"}"))
				.andExpect(status().isOk());
	}

	private void adjustItem(long itemId, int delta) throws Exception {
		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"delta\": " + delta + ", \"reason\": \"ADJUSTMENT\"}"))
				.andExpect(status().isOk());
	}
}

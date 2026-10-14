package com.dimasekam.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleConcurrencyTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void twoConcurrentSalesLeaveOneWinner() throws Exception {
		// This test is not @Transactional: the two threads need their own committed
		// transactions, so the SKU is unique per run instead of relying on rollback.
		long itemId = createItem("MUG-RACE-" + System.nanoTime());
		restockItem(itemId, 1);

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		int[] statuses = new int[2];

		executor.submit(() -> {
			try {
				start.await();
				int status = mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":[{\"type\":\"ITEM\",\"id\":" + itemId + ",\"quantity\":1}] }"))
						.andReturn().getResponse().getStatus();
				statuses[0] = status;
			} catch (Exception e) {
				statuses[0] = 500;
			}
		});

		executor.submit(() -> {
			try {
				start.await();
				int status = mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":[{\"type\":\"ITEM\",\"id\":" + itemId + ",\"quantity\":1}] }"))
						.andReturn().getResponse().getStatus();
				statuses[1] = status;
			} catch (Exception e) {
				statuses[1] = 500;
			}
		});

		start.countDown();
		executor.shutdown();
		assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		int winners = 0;
		int losers = 0;
		for (int s : statuses) {
			if (s == 201) winners++;
			else if (s == 409) losers++;
		}
		assertThat(winners).isEqualTo(1);
		assertThat(losers).isEqualTo(1);

		mockMvc.perform(get("/api/v1/items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(0));
	}

	private long createItem(String sku) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sku\":\"" + sku + "\",\"name\":\"Item " + sku + "\",\"description\":\"Item " + sku
						+ "\",\"price\":9.99}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("id").asLong();
	}

	private void restockItem(long itemId, int delta) throws Exception {
		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":" + delta + ",\"reason\":\"RESTOCK\"}"))
				.andExpect(status().isOk());
	}
}

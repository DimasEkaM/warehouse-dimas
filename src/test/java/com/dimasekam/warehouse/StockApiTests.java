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
class StockApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void restockIncreasesQuantity() throws Exception {
		long itemId = createItem("MUG-WHT");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": 40, "reason": "RESTOCK"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockLevelId").isNumber())
				.andExpect(jsonPath("$.sellable.type").value("ITEM"))
				.andExpect(jsonPath("$.sellable.id").value(itemId))
				.andExpect(jsonPath("$.sellable.sku").value("MUG-WHT"))
				.andExpect(jsonPath("$.quantity").value(40))
				.andExpect(jsonPath("$.movementId").isNumber());
	}

	@Test
	void adjustmentDecreasesQuantity() throws Exception {
		long itemId = createItem("MUG-WHT");
		restockItem(itemId, 10);

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": -3, "reason": "ADJUSTMENT"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(7));
	}

	@Test
	void adjustmentRefusesWhenItWouldGoBelowZero() throws Exception {
		long itemId = createItem("MUG-WHT");
		restockItem(itemId, 3);

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": -5, "reason": "ADJUSTMENT"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("not enough stock for item MUG-WHT: 3 available, 5 requested"));
	}

	@Test
	void fractionalDeltaReturns400() throws Exception {
		long itemId = createItem("MUG-WHT");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": 1.5, "reason": "RESTOCK"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void adjustmentRefusesSaleReasonOnManualEndpoint() throws Exception {
		long itemId = createItem("MUG-WHT");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": 1, "reason": "SALE"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void adjustmentRefusesItemWithVariants() throws Exception {
		long itemId = createItem("TEE-BLK");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "TEE-BLK-M"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/items/" + itemId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": 10, "reason": "RESTOCK"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("item TEE-BLK has variants; adjust the variants instead"));
	}

	@Test
	void variantRestockAndAdjustmentWork() throws Exception {
		long itemId = createItem("TEE-BLK");
		String created = mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "TEE-BLK-M"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long variantId = objectMapper.readTree(created).get("id").asLong();

		mockMvc.perform(post("/api/v1/variants/" + variantId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": 5, "reason": "RESTOCK"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sellable.type").value("VARIANT"))
				.andExpect(jsonPath("$.sellable.sku").value("TEE-BLK-M"))
				.andExpect(jsonPath("$.quantity").value(5));

		mockMvc.perform(post("/api/v1/variants/" + variantId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"delta": -2, "reason": "ADJUSTMENT"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(3));
	}

	@Test
	void movementRowIsWrittenOnRestock() throws Exception {
		long itemId = createItem("MUG-WHT");
		restockItem(itemId, 10);

		mockMvc.perform(get("/api/v1/stock-movements?itemId=" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].sellable.type").value("ITEM"))
				.andExpect(jsonPath("$[0].sellable.sku").value("MUG-WHT"))
				.andExpect(jsonPath("$[0].delta").value(10))
				.andExpect(jsonPath("$[0].quantityAfter").value(10))
				.andExpect(jsonPath("$[0].reason").value("RESTOCK"));
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
}

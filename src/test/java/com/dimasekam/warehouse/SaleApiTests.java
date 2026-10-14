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
class SaleApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void recordSaleSucceeds() throws Exception {
		long itemId = createItem("MUG-WHT");
		restockItem(itemId, 5);

		String body = mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":[{\"type\":\"ITEM\",\"id\":" + itemId + ",\"quantity\":2}] }"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.lines[0].sku").value("MUG-WHT"))
				.andExpect(jsonPath("$.total").value(19.98))
				.andReturn().getResponse().getContentAsString();

		long saleId = objectMapper.readTree(body).get("id").asLong();
		mockMvc.perform(get("/api/v1/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lines[0].quantity").value(2));
	}

	@Test
	void oversellReturns409() throws Exception {
		long itemId = createItem("MUG-WHT");
		restockItem(itemId, 3);

		mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":[{\"type\":\"ITEM\",\"id\":" + itemId + ",\"quantity\":5}] }"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void sellItemWithVariantsReturns409() throws Exception {
		long itemId = createItem("TEE-BLK");
		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"TEE-BLK-M\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":[{\"type\":\"ITEM\",\"id\":" + itemId + ",\"quantity\":1}] }"))
				.andExpect(status().isConflict());
	}

	@Test
	void failedLineWritesNoLinesAtAll() throws Exception {
		// Three lines: the third asks for more than is available. Nothing from this
		// sale may land - no quantities moved, no SALE movements, no sale rows.
		long firstId = createItem("MUG-WHT");
		long secondId = createItem("PLATE-BLU");
		long thirdId = createItem("BOWL-RED");
		restockItem(firstId, 5);
		restockItem(secondId, 5);
		restockItem(thirdId, 2);

		mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\":["
								+ "{\"type\":\"ITEM\",\"id\":" + firstId + ",\"quantity\":1},"
								+ "{\"type\":\"ITEM\",\"id\":" + secondId + ",\"quantity\":1},"
								+ "{\"type\":\"ITEM\",\"id\":" + thirdId + ",\"quantity\":5}"
								+ "]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));

		// every quantity is untouched
		assertStock(firstId, 5);
		assertStock(secondId, 5);
		assertStock(thirdId, 2);

		// the first two lines wrote no SALE movement either
		assertOnlyRestockMovement(firstId);
		assertOnlyRestockMovement(secondId);
	}

	private void assertStock(long itemId, int expected) throws Exception {
		mockMvc.perform(get("/api/v1/items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(expected));
	}

	private void assertOnlyRestockMovement(long itemId) throws Exception {
		mockMvc.perform(get("/api/v1/stock-movements?itemId=" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].reason").value("RESTOCK"));
	}

	private long createItem(String sku) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"" + sku + "\",\"name\":\"Item " + sku
								+ "\",\"description\":\"Item " + sku + "\",\"price\":9.99}"))
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

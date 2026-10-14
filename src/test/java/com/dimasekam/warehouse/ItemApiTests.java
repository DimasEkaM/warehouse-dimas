package com.dimasekam.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createReturnsTheNewItem() throws Exception {
		mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "TEE-BLK", "name": "Plain Tee", "description": "cotton", "price": 19.90}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.sku").value("TEE-BLK"))
				.andExpect(jsonPath("$.name").value("Plain Tee"))
				.andExpect(jsonPath("$.description").value("cotton"))
				.andExpect(jsonPath("$.price").value(19.90))
				.andExpect(jsonPath("$.stock").value(0))
				.andExpect(jsonPath("$.stockDerived").value(false))
				.andExpect(jsonPath("$.variantCount").value(0));
	}

	@Test
	void createRejectsABlankSku() throws Exception {
		mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "No sku", "price": 5.00}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
	}

	@Test
	void createRejectsANegativePrice() throws Exception {
		mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "NEG-1", "name": "Bad price", "price": -1.00}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
	}

	@Test
	void createRejectsADuplicateSku() throws Exception {
		createItem("DUP-1");

		mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "DUP-1", "name": "Second", "price": 3.00}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("sku already in use: DUP-1"));
	}

	@Test
	void createRejectsAMalformedBody() throws Exception {
		mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("request body is missing or malformed"));
	}

	@Test
	void getReturnsOneItem() throws Exception {
		long id = createItem("GET-1");

		mockMvc.perform(get("/api/v1/items/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.sku").value("GET-1"))
				.andExpect(jsonPath("$.stock").value(0))
				.andExpect(jsonPath("$.variants").isArray());
	}

	@Test
	void getReturns404ForAnUnknownId() throws Exception {
		mockMvc.perform(get("/api/v1/items/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("item not found: 999999"));
	}

	@Test
	void listReturnsTheItems() throws Exception {
		createItem("LIST-1");

		mockMvc.perform(get("/api/v1/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].sku").value("LIST-1"));
	}

	@Test
	void updateKeepsTheFieldsTheCallerOmitted() throws Exception {
		long id = createItem("UPD-1");

		// Only sku and name are sent, so description and price must stay as they were.
		mockMvc.perform(put("/api/v1/items/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "UPD-1", "name": "Renamed"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"))
				.andExpect(jsonPath("$.description").value("Item UPD-1"))
				.andExpect(jsonPath("$.price").value(9.99));
	}

	@Test
	void updateReturns404ForAnUnknownId() throws Exception {
		mockMvc.perform(put("/api/v1/items/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "NOPE", "name": "Nope"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateRejectsASkuThatAnotherItemHolds() throws Exception {
		createItem("UPD-A");
		long id = createItem("UPD-B");

		mockMvc.perform(put("/api/v1/items/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "UPD-A", "name": "Taken"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("sku already in use: UPD-A"));
	}

	@Test
	void deleteRemovesTheItem() throws Exception {
		long id = createItem("DEL-1");

		mockMvc.perform(delete("/api/v1/items/" + id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/items/" + id))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRefusesAnItemThatStillHasVariants() throws Exception {
		long id = createItem("DEL-VAR");

		mockMvc.perform(post("/api/v1/items/" + id + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "DEL-VAR-M", "attributes": [{"name": "size", "value": "M"}]}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/items/" + id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("item DEL-VAR has variants and cannot be deleted"));
	}

	@Test
	void deleteRefusesAnItemThatAppearsInASale() throws Exception {
		long id = createItem("DEL-SALE");
		restockItem(id, 3);
		recordSale(id, 1);

		mockMvc.perform(delete("/api/v1/items/" + id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("item DEL-SALE appears in a sale and cannot be deleted"));
	}

	@Test
	void deleteRefusesAnItemWithStockHistory() throws Exception {
		long id = createItem("DEL-STOCK");
		restockItem(id, 3);

		mockMvc.perform(delete("/api/v1/items/" + id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("item DEL-STOCK has stock history and cannot be deleted"));
	}

	// Creates an item through the API and returns its id, so tests stay short.
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

	private void recordSale(long itemId, int quantity) throws Exception {
		mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\": [{\"type\": \"ITEM\", \"id\": " + itemId
								+ ", \"quantity\": " + quantity + "}]}"))
				.andExpect(status().isCreated());
	}
}

package com.dimasekam.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VariantApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createReturnsTheNewVariant() throws Exception {
		long itemId = createItem("P-TEE");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "P-TEE-M", "price": 12.50, "attributes": [{"name": "size", "value": "M"}]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.itemId").value(itemId))
				.andExpect(jsonPath("$.sku").value("P-TEE-M"))
				.andExpect(jsonPath("$.price").value(12.50))
				.andExpect(jsonPath("$.effectivePrice").value(12.50))
				.andExpect(jsonPath("$.priceSource").value("VARIANT"))
				.andExpect(jsonPath("$.stock").value(0))
				.andExpect(jsonPath("$.attributes[0].name").value("size"))
				.andExpect(jsonPath("$.attributes[0].value").value("M"));
	}

	@Test
	void createFallsBackToTheItemPrice() throws Exception {
		long itemId = createItem("P-TEE");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "P-TEE-L"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.price").value(nullValue()))
				.andExpect(jsonPath("$.effectivePrice").value(9.99))
				.andExpect(jsonPath("$.priceSource").value("ITEM"));
	}

	@Test
	void createReturns404ForAnUnknownItem() throws Exception {
		mockMvc.perform(post("/api/v1/items/999999/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "ORPHAN-1"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("item not found: 999999"));
	}

	@Test
	void createRejectsADuplicateVariantSku() throws Exception {
		long itemId = createItem("P-TEE");
		createVariant(itemId, "DUP-V");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "DUP-V"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("sku already in use: DUP-V"));
	}

	@Test
	void createRejectsASkuThatAnItemHolds() throws Exception {
		createItem("SHARED-1");
		long itemId = createItem("P-TEE");

		// The sku is free among variants but taken by an item, and skus are unique across both.
		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "SHARED-1"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("sku already in use: SHARED-1"));
	}

	@Test
	void createRejectsABlankSku() throws Exception {
		long itemId = createItem("P-TEE");

		mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"price": 1.00}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
	}

	@Test
	void listReturnsTheVariantsOfAnItem() throws Exception {
		long itemId = createItem("P-TEE");
		createVariant(itemId, "P-TEE-S");
		createVariant(itemId, "P-TEE-M");

		mockMvc.perform(get("/api/v1/items/" + itemId + "/variants"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.sku == 'P-TEE-S')]").isNotEmpty())
				.andExpect(jsonPath("$[?(@.sku == 'P-TEE-M')]").isNotEmpty());
	}

	@Test
	void listReturns404ForAnUnknownItem() throws Exception {
		mockMvc.perform(get("/api/v1/items/999999/variants"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("item not found: 999999"));
	}

	@Test
	void updateKeepsThePriceAndAttributesWhenTheyAreOmitted() throws Exception {
		long itemId = createItem("P-TEE");
		long variantId = createVariantWithDetails(itemId, "V-OLD", 12.50);

		// Only sku is sent, so price and attributes must stay as they were.
		mockMvc.perform(put("/api/v1/variants/" + variantId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "V-NEW"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sku").value("V-NEW"))
				.andExpect(jsonPath("$.price").value(12.50))
				.andExpect(jsonPath("$.priceSource").value("VARIANT"))
				.andExpect(jsonPath("$.attributes.length()").value(1))
				.andExpect(jsonPath("$.attributes[0].name").value("size"));
	}

	@Test
	void updateReplacesTheAttributesWhenTheyAreSent() throws Exception {
		long itemId = createItem("P-TEE");
		long variantId = createVariantWithDetails(itemId, "V-ATTR", 12.50);

		mockMvc.perform(put("/api/v1/variants/" + variantId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "V-ATTR", "attributes": [{"name": "color", "value": "black"}]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attributes.length()").value(1))
				.andExpect(jsonPath("$.attributes[0].name").value("color"))
				.andExpect(jsonPath("$.attributes[0].value").value("black"));
	}

	@Test
	void updateCanGiveAVariantItsOwnPrice() throws Exception {
		long itemId = createItem("P-TEE");
		long variantId = createVariant(itemId, "V-PRICE");

		mockMvc.perform(put("/api/v1/variants/" + variantId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "V-PRICE", "price": 7.77}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").value(7.77))
				.andExpect(jsonPath("$.priceSource").value("VARIANT"));
	}

	@Test
	void updateReturns404ForAnUnknownId() throws Exception {
		mockMvc.perform(put("/api/v1/variants/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "NOPE"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateRejectsASkuThatAnotherVariantHolds() throws Exception {
		long itemId = createItem("P-TEE");
		createVariant(itemId, "V-A");
		long variantB = createVariant(itemId, "V-B");

		mockMvc.perform(put("/api/v1/variants/" + variantB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku": "V-A"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("sku already in use: V-A"));
	}

	@Test
	void deleteRemovesTheVariant() throws Exception {
		long itemId = createItem("P-TEE");
		long variantId = createVariant(itemId, "V-DEL");

		mockMvc.perform(delete("/api/v1/variants/" + variantId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.variantCount").value(0))
				.andExpect(jsonPath("$.variants.length()").value(0));
	}

	@Test
	void deleteReturns404ForAnUnknownId() throws Exception {
		mockMvc.perform(delete("/api/v1/variants/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRefusesAVariantThatAppearsInASale() throws Exception {
		long itemId = createItem("P-SALE");
		long variantId = createVariant(itemId, "V-SALE");
		restockVariant(variantId, 3);
		recordVariantSale(variantId, 1);

		mockMvc.perform(delete("/api/v1/variants/" + variantId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("variant V-SALE appears in a sale and cannot be deleted"));
	}

	@Test
	void deleteRefusesAVariantWithStockHistory() throws Exception {
		long itemId = createItem("P-HIST");
		long variantId = createVariant(itemId, "V-HIST");
		restockVariant(variantId, 3);

		mockMvc.perform(delete("/api/v1/variants/" + variantId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("variant V-HIST has stock history and cannot be deleted"));
	}

	// Creates an item through the API and returns its id.
	private long createItem(String sku) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\": \"" + sku + "\", \"name\": \"Item " + sku
								+ "\", \"description\": \"Item " + sku + "\", \"price\": 9.99}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(body).get("id").asLong();
	}

	// Creates a variant with a sku only, and returns its id.
	private long createVariant(long itemId, String sku) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\": \"" + sku + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(body).get("id").asLong();
	}

	// Creates a variant with a sku, a price, and one attribute, and returns its id.
	private long createVariantWithDetails(long itemId, String sku, double price) throws Exception {
		String body = mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\": \"" + sku + "\", \"price\": " + price
								+ ", \"attributes\": [{\"name\": \"size\", \"value\": \"M\"}]}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(body).get("id").asLong();
	}

	private void restockVariant(long variantId, int delta) throws Exception {
		mockMvc.perform(post("/api/v1/variants/" + variantId + "/stock")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"delta\": " + delta + ", \"reason\": \"RESTOCK\"}"))
				.andExpect(status().isOk());
	}

	private void recordVariantSale(long variantId, int quantity) throws Exception {
		mockMvc.perform(post("/api/v1/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lines\": [{\"type\": \"VARIANT\", \"id\": " + variantId
								+ ", \"quantity\": " + quantity + "}]}"))
				.andExpect(status().isCreated());
	}
}

# Warehouse API

A REST API for a  simple shop warehouse management system : products (**items**), their **variants**, **prices**,
**stock levels** with a movement history, and **sales**. Built with Java 21,
Spring Boot, JPA/Hibernate, and an embedded H2 database. There is no UI, everything
is JSON over HTTP.

## Requirements

- JDK 21
- Nothing else: the Maven wrapper (`mvnw`) and H2 come with the repository
- An internet connection for the very first run, because the wrapper downloads
  Maven and the project's dependencies once and caches them afterwards

## Run it

From the repository root:

Windows:

    .\mvnw.cmd spring-boot:run

macOS / Linux:

    ./mvnw spring-boot:run

The server listens on `http://localhost:8080`. Check it with:

    curl http://localhost:8080/api/v1/items

Stop it with `Ctrl+C`.

## Sample data (optional)

Windows (keep the `-D...` argument quoted, otherwise PowerShell splits it on the
dots and Maven reports an unknown lifecycle phase):

    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=seed"

macOS / Linux:

    ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed

This adds a few rows whose SKUs all start with `SEED-`: a mug with its own stock, a
tee with two size variants, and one sale recorded against them. Running it a second
time changes nothing, every step checks what already exists first. Without the
`seed` profile nothing is seeded and the application starts with whatever the
database already holds.

## Tests

    .\mvnw.cmd test

One command runs everything: the API tests, validation, the seed, the
sum-of-movements check, and the concurrency test where two sales race for the last
unit. The tests use a separate in-memory database, so your stored data is not
touched.

## Where the data lives

File: `data/warehouse.mv.db`, next to `pom.xml`. It is gitignored and survives
restarts. To start from an empty database, stop the application and delete the
`data` folder.

## Design decisions

1. **Pessimistic locking instead of a version column.** Sales and stock adjustments
   lock the stock row with `SELECT ... FOR UPDATE` inside one transaction, so when
   two sales race for the last unit one wins and the other gets `409`. The rule
   lives in one service method instead of a retry loop in every caller.

2. **Stock rows are created on first change.** A sellable starts with no stock row;
   reading a missing row means quantity `0`, and the first adjustment creates it.
   Nothing to initialise on create, and rows only exist where history exists.

3. **An item with variants has no stock of its own.** Its quantity is the sum of its
   variant rows, returned as `stock` with `"stockDerived": true`; adjustments go to
   the variants.

4. **`PUT` is a partial update.** `sku` and `name` are required (blank values are
   rejected with `400`); `price` and `description` keep their old value when left
   out. A variant's `price` is optional and falls back to the item's price, reported
   with `priceSource`.

5. **One SKU namespace.** Items and variants are both sellables, so a SKU is refused
   (`409`) if an item or any other variant already holds it.

6. **Sales are all-or-nothing and remember their prices.** Every line is checked
   before anything is written, one bad line means no sale at all. Each line stores
   the unit price charged at the time, so a later catalog price change never
   rewrites an existing sale, and the total is calculated once when the sale is
   recorded.

7. **Delete is refused when history exists.** `DELETE` returns `409` if the
   sellable still has variants, appears in a sale, or has stock movements, because
   history is never destroyed silently.

8. **One error shape.** Every error answers with the same JSON: `timestamp`,
   `status`, `error`, `message`, `path`, and `fieldErrors` (naming the offending
   field) when validation failed.

9. **Sample data only when asked for.** The seeder is a plain bean; a small runner
   gated on the `seed` profile decides whether startup calls it. Left free to run by
   itself it would insert rows under every test, so the tests stay independent of it.

10. **Fractions are rejected, not truncated.** Jackson's float-to-int coercion is
    turned off: `{"delta": 1.5}` is a `400`, where before it was silently stored
    as `1`.

11. **Lists come back in creation order.** Items, variants, and stock movements are
    returned in the order they were written; there is no pagination because the
    brief does not ask for it.

## Assumptions

Where the brief is silent these were the choices made. Each one is reversible.

1. One shop, one warehouse. Stock carries no location dimension.
2. An item without variants is sellable directly; an item with variants is not, its
   variants are the sellable units, so stock adjustments and sale lines point at the
   variants while the item reports their sum.
3. Variant differences are free-form name and value pairs, not a fixed schema of
   size and colour.
4. Deletes are hard deletes, refused while dependents exist, instead of a soft
   delete behind an `active` flag.
5. Stock goes through one endpoint that takes a signed delta; the `reason`
   separates a restock from a downward correction.
6. A sale is a recorded fact, not a workflow: no payment status, no cancellation,
   no shipment.
7. Unit prices are copied onto the sale line when the sale is recorded, so editing
   a catalog price later never rewrites an old sale.
8. No authentication. The API assumes it is reached over the shop's own network.
9. H2 in file mode is the database. Swapping it for PostgreSQL should touch
   configuration, not code.
10. Sample data sits behind the opt-in `seed` profile, so a reviewer can start from
    an empty database.

## API examples

Base URL `http://localhost:8080`, bodies are JSON. On Windows PowerShell call
`curl.exe`: a bare `curl` there is an alias for `Invoke-WebRequest`, which does not
understand these flags. The payloads use single quotes, which both PowerShell and
bash pass through unchanged.

| Method | Path | What it does |
|---|---|---|
| `POST` | `/api/v1/items` | create an item (`201`, `409` for a duplicate SKU) |
| `GET` | `/api/v1/items` | list items in creation order |
| `GET` | `/api/v1/items/{id}` | one item with its variants (`404` unknown id) |
| `PUT` | `/api/v1/items/{id}` | update; `sku` and `name` are required, the rest is optional |
| `DELETE` | `/api/v1/items/{id}` | delete (`409` while variants, sale lines, or stock movements exist) |
| `POST` | `/api/v1/items/{itemId}/variants` | create a variant (`404` unknown item, `409` duplicate SKU) |
| `GET` | `/api/v1/items/{itemId}/variants` | list the variants of an item |
| `PUT` | `/api/v1/variants/{id}` | update a variant; sending `attributes` replaces them |
| `DELETE` | `/api/v1/variants/{id}` | delete a variant (`409` while sale lines reference it) |
| `POST` | `/api/v1/items/{itemId}/stock` | change item stock by a signed delta |
| `POST` | `/api/v1/variants/{variantId}/stock` | change variant stock by a signed delta |
| `GET` | `/api/v1/stock-movements?itemId=1` | movement history, also filterable by `variantId` |
| `POST` | `/api/v1/sales` | record a sale (`201`, `409` when a line exceeds stock) |
| `GET` | `/api/v1/sales/{id}` | read one sale with its lines |

Stock reasons: `RESTOCK` for goods arriving, `ADJUSTMENT` for a manual correction,
`SALE` for a sale drawing stock down. Posting `SALE` to a stock endpoint yourself
returns `400`, the sale endpoint is the only thing that writes it.

### Create an item

    curl -X POST http://localhost:8080/api/v1/items -H "Content-Type: application/json" -d '{"sku":"MUG-WHT","name":"White mug","description":"Ceramic mug, 350ml","price":9.99}'

`201 Created`:

```json
{
  "id": 1,
  "sku": "MUG-WHT",
  "name": "White mug",
  "description": "Ceramic mug, 350ml",
  "price": 9.99,
  "stock": 0,
  "stockDerived": false,
  "variantCount": 0,
  "variants": []
}
```

### Restock it

    curl -X POST http://localhost:8080/api/v1/items/1/stock -H "Content-Type: application/json" -d '{"delta":12,"reason":"RESTOCK"}'

```json
{
  "stockLevelId": 1,
  "sellable": { "type": "ITEM", "id": 1, "sku": "MUG-WHT" },
  "quantity": 12,
  "movementId": 1
}
```

The stock row is created by this first change; reading the item before it would
have shown `0`.

### A variant that inherits the price

Create a second item `TEE-TEE` (price `19.90`, it becomes id `2`), then a variant
without its own price (the first variant in a fresh database is id `1`):

    curl -X POST http://localhost:8080/api/v1/items/2/variants -H "Content-Type: application/json" -d '{"sku":"TEE-TEE-L","attributes":[{"name":"Size","value":"L"}]}'

```json
{
  "id": 1,
  "itemId": 2,
  "sku": "TEE-TEE-L",
  "price": null,
  "effectivePrice": 19.90,
  "priceSource": "ITEM",
  "stock": 0,
  "attributes": [ { "name": "Size", "value": "L" } ]
}
```

Restock the variant the same way as an item:

    curl -X POST http://localhost:8080/api/v1/variants/1/stock -H "Content-Type: application/json" -d '{"delta":8,"reason":"RESTOCK"}'

### Record a sale

Two lines: two mugs and one large tee.

    curl -X POST http://localhost:8080/api/v1/sales -H "Content-Type: application/json" -d '{"lines":[{"type":"ITEM","id":1,"quantity":2},{"type":"VARIANT","id":1,"quantity":1}]}'

`201 Created`:

```json
{
  "id": 1,
  "createdAt": "2026-10-14T13:12:00Z",
  "lines": [
    { "id": 1, "type": "ITEM", "sellableId": 1, "sku": "MUG-WHT", "quantity": 2, "unitPrice": 9.99, "lineTotal": 19.98 },
    { "id": 2, "type": "VARIANT", "sellableId": 1, "sku": "TEE-TEE-L", "quantity": 1, "unitPrice": 19.90, "lineTotal": 19.90 }
  ],
  "total": 39.88
}
```

Every line is checked before anything is written, so if either line oversells the
whole request is refused and no line is stored. `GET /api/v1/sales/1` returns the
same body later, with the same unit prices even after a catalog price change.

### Movement history

    curl "http://localhost:8080/api/v1/stock-movements?itemId=1"

```json
[
  {
    "id": 1,
    "sellable": { "type": "ITEM", "id": 1, "sku": "MUG-WHT" },
    "delta": 12,
    "quantityAfter": 12,
    "reason": "RESTOCK",
    "referenceId": null,
    "createdAt": "2026-10-14T13:05:00Z"
  },
  {
    "id": 3,
    "sellable": { "type": "ITEM", "id": 1, "sku": "MUG-WHT" },
    "delta": -2,
    "quantityAfter": 10,
    "reason": "SALE",
    "referenceId": 1,
    "createdAt": "2026-10-14T13:12:00Z"
  }
]
```

`referenceId` on a `SALE` movement is the id of the sale that caused it.

### Errors

Selling more than is on the shelf:

    curl -X POST http://localhost:8080/api/v1/sales -H "Content-Type: application/json" -d '{"lines":[{"type":"ITEM","id":1,"quantity":50}]}'

`409 Conflict`:

```json
{
  "timestamp": "2026-10-14T13:15:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "not enough stock for item MUG-WHT: 10 available, 50 requested",
  "path": "/api/v1/sales",
  "fieldErrors": []
}
```

Posting an item without a sku returns `400 Bad Request` and names the field:

    curl -X POST http://localhost:8080/api/v1/items -H "Content-Type: application/json" -d '{"name":"No sku","price":5.00}'

```json
{
  "timestamp": "2026-10-14T13:16:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "validation failed",
  "path": "/api/v1/items",
  "fieldErrors": [ { "field": "sku", "message": "sku is required" } ]
}
```

### Update and delete

    curl -X PUT http://localhost:8080/api/v1/items/1 -H "Content-Type: application/json" -d '{"sku":"MUG-WHT","name":"White mug, 350ml","price":10.99}'
    curl -X DELETE http://localhost:8080/api/v1/items/1

A delete returns `409` while the item still has variants, appears in a sale, or
has stock movements, and `204` when nothing references it.

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

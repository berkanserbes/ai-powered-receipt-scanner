# Database Schema

This document describes the PostgreSQL database schema used by the Receipt Scanner application. Tables are created and managed automatically by Hibernate via `spring.jpa.hibernate.ddl-auto: update`.

---

## Overview

```
┌──────────────┐        ┌──────────────────┐        ┌─────────────────┐
│    users     │        │    receipts      │        │  receipt_items  │
├──────────────┤        ├──────────────────┤        ├─────────────────┤
│ id (PK)      │◄──┐    │ id (PK)          │◄──┐    │ id (PK)         │
│ username     │   │    │ user_id (FK)─────┘   │    │ receipt_id (FK)─┘
│ password     │   │    │ merchant_name    │   │    │ name            │
│ role         │   │    │ transaction_date │   │    │ quantity        │
│ created_at   │   │    │ total_amount     │   │    │ unit_price      │
│ updated_at   │   └────│ currency         │   │    │ total_price     │
└──────────────┘        │ image_url        │   │    └─────────────────┘
                        │ created_at       │   │
                        │ updated_at       │   │
                        └──────────────────┘   │
                                               │
┌───────────────────┐                          │
│  refresh_tokens   │                          │
├───────────────────┤                          │
│ id (PK)           │                          │
│ token             │                          │
│ user_id (FK)──────┼──────────────────────────┘
│ expiry_date       │
│ created_at        │
│ revoked           │
└───────────────────┘
```

---

## Tables

### `users`

Stores registered user accounts.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique user identifier |
| `username` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | Login username |
| `password` | `VARCHAR(255)` | `NOT NULL` | BCrypt-hashed password |
| `role` | `VARCHAR(20)` | `NOT NULL` | User role: `ADMIN` or `USER` |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Set on insert, never updated |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | | Set on update |

**Indexes:**
- `PRIMARY KEY` on `id`
- `UNIQUE` index on `username`

---

### `receipts`

Stores analyzed receipts linked to a user.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique receipt identifier |
| `user_id` | `BIGINT` | `NOT NULL`, `FK → users.id` | Owner of the receipt |
| `merchant_name` | `VARCHAR(255)` | | Name of the merchant/store |
| `transaction_date` | `TIMESTAMP WITH TIME ZONE` | | Date and time of the transaction |
| `total_amount` | `NUMERIC(10, 2)` | | Total receipt amount |
| `currency` | `VARCHAR(3)` | | ISO 4217 currency code (e.g. `USD`, `EUR`) |
| `image_url` | `VARCHAR(255)` | | Relative path to the stored image file |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Set on insert, never updated |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | | Set on update |

**Indexes:**
- `PRIMARY KEY` on `id`
- `idx_receipt_user_id` on `user_id`
- `idx_receipt_created_at` on `created_at`

**Relationships:**
- Many-to-one → `users`
- One-to-many → `receipt_items` (cascade all, orphan removal)

---

### `receipt_items`

Stores individual line items for each receipt.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique item identifier |
| `receipt_id` | `BIGINT` | `NOT NULL`, `FK → receipts.id` | Parent receipt |
| `name` | `VARCHAR(255)` | `NOT NULL` | Item name / description |
| `quantity` | `INTEGER` | | Quantity purchased (defaults to `1`) |
| `unit_price` | `NUMERIC(10, 2)` | | Price per single unit |
| `total_price` | `NUMERIC(10, 2)` | | `quantity × unit_price` (calculated on persist/update) |

**Indexes:**
- `PRIMARY KEY` on `id`

**Relationships:**
- Many-to-one → `receipts`

**Computed field note:**  
If `total_price` is `null` and `unit_price` is provided, `total_price` is automatically computed as `unit_price × quantity` in the `@PrePersist` / `@PreUpdate` lifecycle callback.

---

### `refresh_tokens`

Stores active refresh tokens for token rotation.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique token record identifier |
| `token` | `VARCHAR(500)` | `NOT NULL`, `UNIQUE` | The opaque refresh token string (UUID) |
| `user_id` | `BIGINT` | `NOT NULL`, `FK → users.id` | Token owner |
| `expiry_date` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Expiry timestamp |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Set on insert |
| `revoked` | `BOOLEAN` | default `false` | Whether the token has been explicitly revoked |

**Indexes:**
- `PRIMARY KEY` on `id`
- `idx_refresh_token_token` on `token`
- `idx_refresh_token_user_id` on `user_id`

**Relationships:**
- Many-to-one → `users`

**Token lifecycle:**
1. Created on login or token refresh.
2. Previous token is revoked on each refresh (rotation).
3. Expired tokens are deleted during verification.
4. A token is considered invalid if `revoked = true` OR `expiry_date < NOW()`.

---

## Roles

The `role` column in `users` is a string enum with two values:

| Value | Description |
|---|---|
| `ADMIN` | Full access — can read all users' receipts |
| `USER` | Restricted access — can only read own receipts |

---

## Notes

- **Schema management:** Hibernate manages the schema via `ddl-auto: update`. In production, consider using a migration tool like Flyway or Liquibase for controlled schema evolution.
- **Timestamps:** All timestamp columns use `TIMESTAMP WITH TIME ZONE` and store values as UTC `Instant`.
- **Soft delete:** Not implemented — records are hard deleted when removed.
- **Cascades:** Deleting a `Receipt` automatically deletes all its `ReceiptItem` records (`CascadeType.ALL` + `orphanRemoval = true`).

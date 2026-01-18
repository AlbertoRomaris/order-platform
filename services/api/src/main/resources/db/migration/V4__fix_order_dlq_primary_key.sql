-- Fix DLQ primary key mismatch between DB and JPA mapping

-- 1) Drop existing PK on id
ALTER TABLE order_dlq DROP CONSTRAINT order_dlq_pkey;

-- 2) Drop unused id column
ALTER TABLE order_dlq DROP COLUMN id;

-- 3) Make order_id the primary key
ALTER TABLE order_dlq ADD CONSTRAINT order_dlq_pkey PRIMARY KEY (order_id);

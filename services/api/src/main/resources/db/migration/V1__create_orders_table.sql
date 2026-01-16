CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        status VARCHAR(32) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

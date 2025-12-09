-- Create product_categories table
CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE product_categories IS 'Product categories for organizing inventory';
COMMENT ON COLUMN product_categories.status IS 'Category status: ACTIVE or INACTIVE';

-- Create products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category_id BIGINT REFERENCES product_categories(id),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    description TEXT,
    minimum_stock DECIMAL(15,3) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_product_unit CHECK (unit IN ('PCS', 'KG', 'LTR', 'BOX', 'ROLL', 'METER', 'CARTON'))
);

COMMENT ON TABLE products IS 'Product master data';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN products.unit IS 'Unit of measurement: PCS, KG, LTR, BOX, ROLL, METER, CARTON';
COMMENT ON COLUMN products.minimum_stock IS 'Minimum stock level for low stock alerts';

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status);

-- Create warehouse_product_prices table
CREATE TABLE warehouse_product_prices (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    purchase_rate DECIMAL(15,2),
    sale_rate DECIMAL(15,2),
    effective_from DATE,
    last_updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP,
    CONSTRAINT uq_warehouse_product UNIQUE (warehouse_id, product_id)
);

COMMENT ON TABLE warehouse_product_prices IS 'Warehouse-specific product pricing';
COMMENT ON COLUMN warehouse_product_prices.purchase_rate IS 'Purchase rate for this product in this warehouse';
COMMENT ON COLUMN warehouse_product_prices.sale_rate IS 'Sale rate for this product in this warehouse';
COMMENT ON COLUMN warehouse_product_prices.effective_from IS 'Date from which this price is effective';

CREATE INDEX idx_warehouse_prices_warehouse ON warehouse_product_prices(warehouse_id);
CREATE INDEX idx_warehouse_prices_product ON warehouse_product_prices(product_id);

-- Create stock_movements table
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    rate DECIMAL(15,2),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    balance_before DECIMAL(15,3),
    balance_after DECIMAL(15,3),
    notes TEXT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_movement_type CHECK (movement_type IN ('OPENING', 'PURCHASE', 'SALE', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT'))
);

COMMENT ON TABLE stock_movements IS 'Complete history of all stock movements';
COMMENT ON COLUMN stock_movements.movement_type IS 'Type: OPENING, PURCHASE, SALE, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT';
COMMENT ON COLUMN stock_movements.quantity IS 'Quantity moved (positive for IN, negative for OUT)';
COMMENT ON COLUMN stock_movements.balance_before IS 'Stock balance before this movement';
COMMENT ON COLUMN stock_movements.balance_after IS 'Stock balance after this movement';
COMMENT ON COLUMN stock_movements.reference_type IS 'Reference document type (e.g., purchase, sale)';
COMMENT ON COLUMN stock_movements.reference_id IS 'Reference document ID';

CREATE INDEX idx_stock_movement_warehouse ON stock_movements(warehouse_id);
CREATE INDEX idx_stock_movement_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movement_created ON stock_movements(created_at);
CREATE INDEX idx_stock_movement_reference ON stock_movements(reference_type, reference_id);

-- Create stock_current table
CREATE TABLE stock_current (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    current_quantity DECIMAL(15,3) NOT NULL DEFAULT 0,
    last_movement_id BIGINT REFERENCES stock_movements(id),
    last_updated TIMESTAMP,
    CONSTRAINT uq_warehouse_product_stock UNIQUE (warehouse_id, product_id)
);

COMMENT ON TABLE stock_current IS 'Current stock levels per warehouse and product';
COMMENT ON COLUMN stock_current.current_quantity IS 'Current available quantity';
COMMENT ON COLUMN stock_current.last_movement_id IS 'Reference to last stock movement';

CREATE INDEX idx_stock_current_warehouse ON stock_current(warehouse_id);
CREATE INDEX idx_stock_current_product ON stock_current(product_id);
CREATE INDEX idx_stock_current_quantity ON stock_current(current_quantity);

-- Insert default product categories
INSERT INTO product_categories (name, description, status, created_at, updated_at)
VALUES
    ('Food Products', 'Edible products from suppliers like Engro Foods', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dairy Products', 'Milk, cheese, yogurt, and other dairy items', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Beverages', 'Drinks and beverages', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Snacks', 'Chips, biscuits, and snack items', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Frozen Items', 'Frozen food products', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('General', 'General products', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

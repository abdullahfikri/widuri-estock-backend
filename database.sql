CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(100) NOT NULL,
    photo VARCHAR(255),
    role VARCHAR(100) NOT NULL ,
    date_in TIMESTAMP,
    date_out TIMESTAMP,
    token VARCHAR(100),
    token_expired_at BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    primary key (username),
    UNIQUE (token, email)
) engine = InnoDB;

INSERT INTO users (username, password, first_name, phone, role)
VALUES ('owner', '{bcrypt}$2a$10$9AWKBR894edWIg/3AXn92eMUCfctjRSTvvaoMfkb0m1h0KgN19e4C', 'owner', '00000000', 'OWNER');

SELECT * FROM users;

CREATE TABLE IF NOT EXISTS suppliers (
    id INT NOT NULL AUTO_INCREMENT,
    supplier_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    information VARCHAR(255),
    UNIQUE (supplier_name),
    UNIQUE (email),
    PRIMARY KEY (id)
) engine = InnoDB;

ALTER TABLE suppliers
    ADD COLUMN created_at TIMESTAMP;

ALTER TABLE suppliers
    ADD COLUMN updated_at TIMESTAMP;

ALTER TABLE suppliers
    ADD CONSTRAINT unique_email
    UNIQUE (email);

SELECT * FROM suppliers;

# DELETE FROM suppliers;
DROP TABLE IF EXISTS suppliers;

CREATE TABLE IF NOT EXISTS addresses (
    id int NOT NULL AUTO_INCREMENT,
    street VARCHAR(100) NOT NULL ,
    village VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100) NOT NULL ,
    province VARCHAR(100) NOT NULL ,
    country VARCHAR(100) NOT NULL ,
    postal_code VARCHAR(10),
    primary key (id)
) engine = InnoDB;

# CREATE TABLE IF NOT EXISTS user_addresses (
#     address_id INT NOT NULL ,
#     user_username VARCHAR(100) NOT NULL ,
#     primary key (address_id, user_username),
#     foreign key fk_users__user_addresses (user_username) REFERENCES users(username),
#     foreign key fk_addresses__user_addresses (address_id) REFERENCES addresses(id)
# ) ENGINE = InnoDB;

SELECT * FROM addresses;

# DELETE FROM addresses;

# INSERT INTO users (username, password, first_name, phone, role) VALUES ('testing', 'testing', 'john', '623213123', 'ADM_WAREHOUSE');


ALTER TABLE addresses
    ADD COLUMN created_at TIMESTAMP;

ALTER TABLE addresses
    ADD COLUMN updated_at TIMESTAMP;

# DROP TABLE user_addresses;

ALTER TABLE addresses
    ADD COLUMN user_id VARCHAR(100);


ALTER TABLE addresses
    ADD CONSTRAINT fk_user_addresses
    FOREIGN KEY (user_id) REFERENCES users (username);

ALTER TABLE addresses
    ADD COLUMN supplier_id INT;

ALTER TABLE addresses
    ADD CONSTRAINT uc_supplier UNIQUE (supplier_id);

ALTER TABLE addresses
    ADD CONSTRAINT fk_supplier_addresses
    FOREIGN KEY (supplier_id) REFERENCES suppliers (id);

SELECT * FROM addresses;

CREATE TABLE IF NOT EXISTS refresh_token (
    id INT NOT NULL AUTO_INCREMENT,
    refresh_token VARCHAR(255) NOT NULL ,
    expiredAt TIMESTAMP,
    userAgent VARCHAR(100),
    user_id VARCHAR(100),
    primary key (id),
    unique (refresh_token)
) engine = InnoDB;

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_user_refresh_token
    FOREIGN KEY (user_id) REFERENCES users (username);

SELECT * FROM refresh_token;

ALTER TABLE refresh_token
    RENAME COLUMN expiredAT to expired_at;

ALTER TABLE refresh_token
    RENAME COLUMN userAgent to user_agent;

SELECT * FROM users;

CREATE TABLE IF NOT EXISTS categories (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL ,
    description VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE (name)
) ENGINE = InnoDB;

SELECT * FROM categories;

CREATE TABLE IF NOT EXISTS products (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    has_variant BOOLEAN  ,
    stock SMALLINT,
    price INT,
    category_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

ALTER TABLE products
    ADD COLUMN created_at TIMESTAMP;

ALTER TABLE products
    ADD COLUMN updated_at TIMESTAMP;

ALTER TABLE products
    MODIFY has_variant BOOLEAN NOT NULL ;

ALTER TABLE products
    ADD CONSTRAINT fk_category_product
    FOREIGN KEY (category_id) REFERENCES categories (id);

SELECT * FROM products;

DELETE FROM products;

CREATE TABLE IF NOT EXISTS product_photos (
    id VARCHAR(255) NOT NULL,
    image_location VARCHAR(255),
    product_id INT NOT NULL ,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

# ALTER TABLE product_photos
#     DROP id;
#
# ALTER TABLE product_photos
#     ADD (id VARCHAR(255));
#
# DELETE FROM product_photos;
#
# ALTER TABLE product_photos
#     ADD CONSTRAINT pk_product_photo
#     PRIMARY KEY (id);
# DROP TABLE product_photos;

ALTER TABLE product_photos
    ADD CONSTRAINT fk_product_product_photo
    FOREIGN KEY (product_id) REFERENCES products (id);

SELECT * FROM product_photos;


CREATE TABLE IF NOT EXISTS product_variants (
    id INT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(255) NOT NULL,
    stock SMALLINT NOT NULL,
    price INT NOT NULL ,
    product_id int NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
# ADD FOREIGN KEY
ALTER TABLE product_variants
    ADD CONSTRAINT fk_product_product_variant
    FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE product_variants
    ADD CONSTRAINT uc_product_variants
    UNIQUE (sku, product_id);


SELECT * FROM product_variants;

CREATE TABLE IF NOT EXISTS variant_attributes (
    id INT NOT NULL AUTO_INCREMENT,
    attribute_key VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(100) NOT NULL,
    product_variants_id INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
# ADD FOREIGN KEY
ALTER TABLE variant_attributes
    ADD CONSTRAINT fk_product_variant_variant_attribute
    FOREIGN KEY (product_variants_id) REFERENCES product_variants (id);

# SELECT * FROM variant_attributes;

RENAME TABLE variant_attributes TO product_variant_attributes;

SELECT * FROM product_variant_attributes;

CREATE TABLE IF NOT EXISTS incoming_products (
    id INT NOT NULL AUTO_INCREMENT,
    date_in TIMESTAMP,
    supplier_id INT NOT NULL ,
    user_username VARCHAR(100) NOT NULL,
    total_products INT NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

ALTER TABLE incoming_products
    ADD CONSTRAINT fk_supplier_incoming_products
    FOREIGN KEY (supplier_id) REFERENCES suppliers (id);

ALTER TABLE incoming_products
    ADD CONSTRAINT fk_user_incoming_products
    FOREIGN KEY (user_username) REFERENCES users (username);

ALTER TABLE incoming_products
    ADD COLUMN update_reason VARCHAR(255);

ALTER TABLE incoming_products
    MODIFY date_in DATE;

SELECT * FROM incoming_products;
desc incoming_products;

CREATE TABLE IF NOT EXISTS incoming_product_details (
    id INT NOT NULL AUTO_INCREMENT,
    incoming_products_id INT NOT NULL ,
    product_id INT NOT NULL ,
    price_per_unit INT,
    quantity SMALLINT,
    total_price INT,
    has_variant BOOLEAN NOT NULL,
    total_variant_quantity SMALLINT,
    total_variant_price INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

ALTER TABLE incoming_product_details
    ADD CONSTRAINT fk_incoming_products_incoming_product_details
    FOREIGN KEY (incoming_products_id) REFERENCES incoming_products (id);

ALTER TABLE incoming_product_details
    ADD CONSTRAINT fk_products_incoming_product_details
    FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE incoming_product_details
    RENAME COLUMN incoming_products_id TO incoming_product_id;

SELECT * FROM incoming_product_details;

CREATE TABLE IF NOT EXISTS incoming_product_variant_details (
    id INT NOT NULL AUTO_INCREMENT,
    incoming_product_detail_id INT NOT NULL ,
    product_variant_id INT NOT NULL,
    price_per_unit INT,
    quantity SMALLINT,
    total_price INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

ALTER TABLE incoming_product_variant_details
    ADD CONSTRAINT fk_incoming_product_details_to_incoming_product_variant_details
    FOREIGN KEY (incoming_product_detail_id) REFERENCES incoming_product_details (id);

ALTER TABLE incoming_product_variant_details
    ADD CONSTRAINT fk_product_variants_to_incoming_product_variant_details
    FOREIGN KEY (product_variant_id) REFERENCES product_variants (id);

SELECT * FROM incoming_product_variant_details;
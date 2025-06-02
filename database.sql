CREATE TABLE users (
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

CREATE TABLE addresses (
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

# CREATE TABLE user_addresses (
#     address_id INT NOT NULL ,
#     user_username VARCHAR(100) NOT NULL ,
#     primary key (address_id, user_username),
#     foreign key fk_users__user_addresses (user_username) REFERENCES users(username),
#     foreign key fk_addresses__user_addresses (address_id) REFERENCES addresses(id)
# ) ENGINE = InnoDB;

SELECT * FROM users;
SELECT * FROM addresses;
INSERT INTO users (username, password, first_name, phone, role) VALUES ('testing', 'testing', 'john', '623213123', 'ADM_WAREHOUSE');


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
#
# ALTER TABLE addresses
#     ADD COLUMN supplier_id INT;
#
# ALTER TABLE addresses
#     ADD CONSTRAINT fk_user_addresses
#         FOREIGN KEY (supplier_id) REFERENCES suppliers (id);

CREATE TABLE refresh_token (
    id int NOT NULL AUTO_INCREMENT,
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
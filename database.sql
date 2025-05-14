CREATE TABLE users (
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(100) NOT NULL,
    photo VARCHAR(255),
    date_in TIMESTAMP,
    date_out TIMESTAMP,
    role VARCHAR(100) NOT NULL ,
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

CREATE TABLE user_addresses (
    address_id INT NOT NULL ,
    user_username VARCHAR(100) NOT NULL ,
    primary key (address_id, user_username),
    foreign key fk_users__user_addresses (user_username) REFERENCES users(username),
    foreign key fk_addresses__user_addresses (address_id) REFERENCES addresses(id)
) ENGINE = InnoDB;

SELECT * FROM users;
SELECT * FROM addresses;
SELECT * FROM user_addresses;


ALTER TABLE addresses
    ADD COLUMN created_at TIMESTAMP;

ALTER TABLE addresses
    ADD COLUMN updated_at TIMESTAMP;
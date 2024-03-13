CREATE TABLE IF NOT EXISTS users(
    user_id serial PRIMARY KEY,
    "login" text UNIQUE NOT NULL,
    password_hash UUID NOT NULL,
    is_admin boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS dishes(
    dish_id serial PRIMARY KEY,
    "name" text UNIQUE NOT NULL,
    quantity int4 NOT NULL DEFAULT 1,
    cook_time int8 NOT NULL,
    price int4 NOT NULL,
    CONSTRAINT correct_quantity CHECK (quantity >= 0),
    CONSTRAINT correct_cook_time CHECK (cook_time >= 0),
    CONSTRAINT correct_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS orders(
    order_id serial PRIMARY KEY,
    user_id int4 NOT NULL,
    start_time int8 NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::int8,
    started_cooking boolean NOT NULL DEFAULT FALSE,
    is_ready boolean NOT NULL DEFAULT FALSE,
    CONSTRAINT correct_start_time CHECK (start_time > 0),
    CONSTRAINT fk_user_id FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_dishes(
    relation_id bigserial PRIMARY KEY,
    order_id int4 NOT NULL,
    dish_id int4 NOT NULL,
    ordered_count int4 NOT NULL,
    UNIQUE (order_id, dish_id),
    CONSTRAINT correct_ordered_count CHECK (ordered_count >= 0),
    CONSTRAINT fk_order_id FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_dish_id FOREIGN KEY(dish_id) REFERENCES dishes(dish_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS restaurant_info(
    "key" varchar(63) PRIMARY KEY,
    "value" int8 NOT NULL
);

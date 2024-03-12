CREATE TABLE if NOT EXISTS users(
    user_id serial PRIMARY KEY,
    "login" text UNIQUE NOT NULL,
    password_hash UUID NOT NULL,
    is_admin boolean NOT NULL default FALSE
);

CREATE TABLE if NOT EXISTS dishes(
    dish_id serial PRIMARY KEY,
    "name" text UNIQUE NOT NULL,
    quantity int4 NOT NULL default 1,
    cook_time int8 NOT NULL,
    CONSTRAINT correct_quantity CHECK (quantity >= 0),
    CONSTRAINT correct_cook_time CHECK (cook_time > 0)
);

CREATE TABLE if NOT EXISTS orders(
    order_id serial PRIMARY KEY,
    user_id int4 NOT NULL,
    start_time int8 NOT NULL default (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::int8,
    is_ready boolean NOT NULL default FALSE,
    CONSTRAINT correct_start_time CHECK (start_time > 0),
    CONSTRAINT fk_user_id foreign KEY(user_id) references users(user_id) on delete cascade
);

CREATE TABLE if NOT EXISTS order_dishes(
    relation_id bigserial PRIMARY KEY,
    order_id int4 NOT NULL,
    dish_id int4 NOT NULL,
    ordered_count int4 NOT NULL,
    UNIQUE (order_id, dish_id),
    CONSTRAINT correct_ordered_count CHECK (ordered_count >= 0),
    CONSTRAINT fk_order_id foreign KEY(order_id) references orders(order_id) on delete cascade,
    CONSTRAINT fk_dish_id foreign KEY(dish_id) references dishes(dish_id) on delete cascade
);

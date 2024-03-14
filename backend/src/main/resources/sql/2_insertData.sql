insert into users("login", password_hash, user_role) values
    ('admin', '21232f29-7a57-35a7-8389-4a0e4a801fc3', 'ADMIN'), -- admin pwd
    ('root', '63a9f0ea-7bb9-3050-b96b-649e85481845', 'ADMIN'),   -- root pwd
    ('user', '81dc9bdb-52d0-3dc2-8036-dbd8313ed055', 'USER');  -- 1234 pwd

insert into restaurant_info("key", "value") values
    ('revenue', 0);

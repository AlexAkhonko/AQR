DELETE FROM items;
DELETE FROM users;
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE items_id_seq RESTART WITH 1;

-- Users
INSERT INTO users (id, login, password) VALUES
                                            (1, 'alice', '$2a$10$demo'),  -- BCrypt("password")
                                            (2, 'bob', '$2a$10$demo');

-- Items alice (13 шт)
INSERT INTO items (id, owner_id, text) VALUES
                                           (1, 1, 'Красные шляпы'),
                                           (2, 1, 'Синие ШЛЯПЫ'),
                                           (3, 1, 'Кот в шляпе'),
                                           (4, 1, 'Зелёная шляпа'),
                                           (5, 1, 'Куртка зимняя'),
                                           (6, 1, 'Ботинки'),
                                           (7, 1, 'Книга'),
                                           (8, 1, 'Ноутбук'),
                                           (9, 1, 'Телефон'),
                                           (10, 1, 'Часы'),
                                           (11, 1, 'Кофе'),
                                           (12, 1, 'Чай'),
                                           (13, 1, 'Шляпы осенние');

-- Items bob (3 шт)
INSERT INTO items (id, owner_id, text) VALUES
                                           (14, 2, 'Ручка'),
                                           (15, 2, 'Тетрадь'),
                                           (16, 2, 'Карандаш');

ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
ADD COLUMN last_name VARCHAR(100),
ADD COLUMN status VARCHAR(50) DEFAULT 'Абитуриент',
ADD COLUMN ent_score INT,
ADD COLUMN location VARCHAR(100),
ADD COLUMN avatar_url VARCHAR(500);

CREATE TABLE user_favorites (
                                user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                                university_id BIGINT REFERENCES universities(id) ON DELETE CASCADE,
                                PRIMARY KEY (user_id, university_id)
);

UPDATE users
SET first_name = 'Alikhan',
    last_name = 'Student',
    status = 'Абитуриент',
    ent_score = 115,
    location = 'Алматы, Казахстан',
    avatar_url = 'https://github.com/shadcn.png'
WHERE id = 1;

INSERT INTO user_favorites (user_id, university_id) VALUES
                                                        (1, 1), -- IITU
                                                        (1, 2); -- KBTU
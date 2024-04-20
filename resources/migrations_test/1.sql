SET MODE PostgreSQL;

CREATE ALIAS IF NOT EXISTS uuid_generate_v4 AS '
    UUID ez_uuid() {
        return UUID.randomUUID();
    }
';

CREATE TABLE IF NOT EXISTS schedulables (
                                     id uuid DEFAULT uuid_generate_v4(),
                                     name varchar(100) NOT NULL,
                                     description varchar(255) NULL,
                                     created_at timestamp NOT NULL,
                                     datetime_ranges TEXT NOT NULL,
                                     closed timestamp ARRAY NULL,
                                     minutes integer NOT NULL,

                                     PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_schedulables_name ON schedulables (name);

CREATE TABLE IF NOT EXISTS users (
                                     id uuid DEFAULT uuid_generate_v4(),
                                     fname varchar(100) NOT NULL,
                                     lname varchar(100) NOT NULL,
                                     phone varchar(32) NOT NULL,
                                     email varchar(100) NOT NULL,
                                     password VARCHAR(100) NULL,
                                     email_verified BOOLEAN DEFAULT false,
                                     password_recovering BOOLEAN DEFAULT false,
                                     role varchar(16) NOT NULL,
                                     email_token VARCHAR(512),
                                     created_at timestamp NOT NULL,

                                     external_type  varchar(16),
                                     external_token varchar(512),

                                     PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_users_fname ON users (fname);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

CREATE TABLE IF NOT EXISTS appointments (
                                     id uuid DEFAULT uuid_generate_v4(),
                                     schedulable_id uuid NOT NULL references schedulables(id),
                                     user_id uuid NOT NULL references users(id),
                                     scheduled_to timestamp NOT NULL,
                                     created_at timestamp NOT NULL,

                                     PRIMARY KEY (id)
);

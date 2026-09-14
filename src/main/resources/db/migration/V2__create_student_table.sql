CREATE TABLE student (
    id BIGINT PRIMARY KEY,
    student_index NVARCHAR(20) NOT NULL UNIQUE,
    full_name NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20),
    CONSTRAINT fk_student_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);
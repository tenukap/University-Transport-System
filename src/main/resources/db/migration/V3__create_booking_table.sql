CREATE TABLE booking (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    trip_id INT NOT NULL,
    pickup_loc_id INT NOT NULL,
    dropoff_loc_id INT NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_booking_student
        FOREIGN KEY (student_id) REFERENCES student(id)
);
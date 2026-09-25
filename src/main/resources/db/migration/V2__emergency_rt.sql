
CREATE TABLE Emergency_Report (
    Report_ID INT IDENTITY(1,1) PRIMARY KEY,
    Report_Title VARCHAR(255) NOT NULL,
    Emergency_Type VARCHAR(100) NOT NULL,
    Description NVARCHAR(MAX) NOT NULL,
    Timestamp DATETIME DEFAULT GETDATE(),
    Resolution_Status VARCHAR(50) DEFAULT 'Pending',
    
    user_id INT,  -- Changed from Student_No to user_id
    
    CONSTRAINT FK_Emergency_User FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE
);
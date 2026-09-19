CREATE TABLE Emergency_Report (
                                  Report_ID INT IDENTITY(1,1) PRIMARY KEY,
                                  Report_Title VARCHAR(255) NOT NULL,
                                  Emergency_Type VARCHAR(100) NOT NULL,
                                  Description NVARCHAR(MAX) NOT NULL,
                                  Timestamp DATETIME DEFAULT GETDATE(),
                                  Resolution_Status VARCHAR(50) DEFAULT 'Pending',

                                  Student_No INT,
                                  Officer_No INT,

                                  CONSTRAINT FK_Emergency_Student FOREIGN KEY (Student_No) REFERENCES Student(Student_No),
                                  CONSTRAINT FK_Emergency_Officer FOREIGN KEY (Officer_No) REFERENCES Transport_Officer(Officer_No)
);
import sqlite3

# Connect to SQLite database (creates it if it doesn't exist)
connection = sqlite3.connect('Family.db')
cursor = connection.cursor()


# Exercise A: Table creation 


# Create Persons table
cursor.execute('''
CREATE TABLE IF NOT EXISTS Persons (
    Person_Id INTEGER PRIMARY KEY,
    Personal_Name TEXT NOT NULL,
    Family_Name TEXT NOT NULL,
    Gender TEXT CHECK(Gender IN ('Male', 'Female')),
    Father_Id INTEGER REFERENCES Persons(Person_Id),
    Mother_Id INTEGER REFERENCES Persons(Person_Id),
    Spouse_Id INTEGER REFERENCES Persons(Person_Id)
)
''')
connection.commit()

# Create FamilyTree table for defining family relationships
cursor.execute('''
CREATE TABLE IF NOT EXISTS FamilyTree (
    Person_Id INTEGER,
    Relative_Id INTEGER,
    Connection_Type TEXT CHECK(Connection_Type IN (
        'Father', 'Mother', 'Brother', 'Sister', 'Son', 'Daughter', 'Spouse')),
    FOREIGN KEY (Person_Id) REFERENCES Persons(Person_Id),
    FOREIGN KEY (Relative_Id) REFERENCES Persons(Person_Id)
);
''')
connection.commit()

# Insert parent relationships into FamilyTree
cursor.execute('''
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)   
SELECT Person_Id, Father_Id, 'Father' FROM Persons WHERE Father_Id IS NOT NULL
UNION ALL
SELECT Person_Id, Mother_Id, 'Mother' FROM Persons WHERE Mother_Id IS NOT NULL; 
''')
connection.commit()

# Insert sibling relationships (brothers and sisters)
cursor.execute('''
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)   
SELECT s.Person_Id, p.Person_Id, 
       CASE WHEN s.Gender = 'Male' THEN 'Brother' ELSE 'Sister' END
FROM Persons p
JOIN Persons s 
    ON p.Father_Id = s.Father_Id AND s.Person_Id != p.Person_Id
''')
connection.commit()

# Insert children relationships (sons and daughters)
cursor.execute('''
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)
SELECT Father_Id, Person_Id, 'Son' FROM Persons WHERE Father_Id IS NOT NULL AND Gender = 'Male'
UNION ALL
SELECT Father_Id, Person_Id, 'Daughter' FROM Persons WHERE Father_Id IS NOT NULL AND Gender = 'Female'
UNION ALL
SELECT Mother_Id, Person_Id, 'Son' FROM Persons WHERE Mother_Id IS NOT NULL AND Gender = 'Male'
UNION ALL
SELECT Mother_Id, Person_Id, 'Daughter' FROM Persons WHERE Mother_Id IS NOT NULL AND Gender = 'Female';           
''')
connection.commit()

# Insert spouse relationships
cursor.execute('''
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)
SELECT Person_Id, Spouse_Id, 'Spouse' FROM Persons WHERE Spouse_Id IS NOT NULL;         
''')
connection.commit()


# Exercise B: Ensure relationships


# Option A: add missing spouse relationships into FamilyTree
cursor.execute('''  
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)
SELECT Relative_Id, Person_Id, Connection_Type
FROM FamilyTree AS F1
WHERE Connection_Type = 'Spouse'
  AND NOT EXISTS (
        SELECT 1
        FROM FamilyTree F2
        WHERE F2.Person_Id = F1.Relative_Id
          AND F2.Relative_Id = F1.Person_Id
          AND F2.Connection_Type = 'Spouse'
    );
''')
connection.commit()

# Option B: Update Persons table to fill missing spouse_id 
cursor.execute('''  
UPDATE Persons AS P1
SET Spouse_Id = (
    SELECT P2.Person_Id
    FROM Persons P2
    WHERE P1.Person_Id = P2.Spouse_Id
)
WHERE Spouse_Id IS NULL;
''')
connection.commit()

# Close the database connection
connection.close()

import sqlite3

# יצירת חיבור לבסיס נתונים 
connection = sqlite3.connect('Family.db')
cursor = connection.cursor()
#תרגיל א

# יצירת טבלה
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

#טבלת עץ משפחה
cursor.execute('''
CREATE TABLE IF NOT EXISTS FamilyTree (
    Person_Id INTEGER,
    Relative_Id INTEGER,
    Connection_Type TEXT CHECK(Connection_Type IN ('Father', 'Mother', 'Brother', 'Sister', 'Son', 'Daughter', 'Spouse')),
    FOREIGN KEY (Person_Id) REFERENCES Persons(Person_Id),
    FOREIGN KEY (Relative_Id) REFERENCES Persons(Person_Id)
);
''')
connection.commit()


#הוספת קשרי הורים
cursor.execute('''
INSERT OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)   
SELECT Person_Id, Father_Id, 'Father' FROM Persons WHERE Father_Id IS NOT NULL
UNION ALL
SELECT Person_Id, Mother_Id, 'Mother' FROM Persons WHERE Mother_Id IS NOT NULL; 
''')
connection.commit()

#הוספת קשרי אחים
cursor.execute('''
INSERT  OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)   
SELECT s.Person_Id, p.person_Id, 
               CASE WHEN s.Gender = 'Male' THEN 'Brother' ELSE 'Sister' END
FROM Persons p
JOIN Persons s ON p.Father_Id = S.Father_Id AND s.Person_Id != p.person_Id
''')
connection.commit()

#הוספת קשרי ילדים
cursor.execute('''
INSERT  OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)
SELECT Father_Id, Person_Id, 'Son' FROM Persons WHERE Father_Id IS NOT NULL AND Gender = 'Male'
UNION ALL
SELECT Father_Id, Person_Id, 'Daughter' FROM Persons WHERE Father_Id IS NOT NULL AND Gender = 'Female'
UNION ALL
SELECT Mother_Id, Person_Id, 'Son' FROM Persons WHERE Mother_Id IS NOT NULL AND Gender = 'Male'
UNION ALL
SELECT Mother_Id, Person_Id, 'Daughter' FROM Persons WHERE Mother_Id IS NOT NULL AND Gender = 'Female';           
''')
connection.commit()

#הוספת קשרי בן/בת זוג
cursor.execute('''
INSERT  OR IGNORE INTO FamilyTree (Person_Id, Relative_Id, Connection_Type)
SELECT Person_Id, Spouse_Id, 'Spouse' FROM Persons WHERE Spouse_Id IS NOT NULL;         
''')
connection.commit()

#תרגיל ב

#אופציה א'- להכניס לטבלת עץ משפחה
cursor.execute('''  
               INSERT OR IGNORE INTO FAMILYTREE (PERSON_ID, RELATIVE_ID, CONNECTION_TYPE)
               
                    SELECT RELATIVE_ID, PERSON_ID, CONNECTION_TYPE
                        FROM FAMILYTREE AS F1
                        WHERE Connection_type ='Spouse'
                           AND NOT EXISTS (SELECT PERSON_ID, RELATIVE_ID
                           FROM FAMILYTREE F2
                           WHERE F2.PERSON_ID = F1.RELATIVE_ID AND
                                 F2.RELATIVE_ID = F1.PERSON_ID anD
                                 F2.CONNECTION_TYPE = 'Spouse' );
               ''')
connection.commit()

#אופציה ב'- לעדכן את טבלת אנשים
cursor.execute('''  
               UPDATE PERSONS AS P1
                    SET SPOUSE_ID =  
                      (SELECT P2.PERSON_ID 
                           FROM PERSONS P2
                           WHERE P1.PERSON_ID = P2.SPOUSE_ID             
                       ) 
                    WHERE SPOUSE_ID IS NULL                
               ''')
connection.commit()


connection.close()


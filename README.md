# Mini DBMS ![Static Badge](https://img.shields.io/badge/MongoDB-%2347A248?logo=mongodb&logoColor=white)


> **Master Project - in collaboration with [Buboi Oana](https://github.com/OanaBuboi)**
>
> A mini relational DBMS system developed as part of the DBMS Implementation course
> 
> **Work in progress**


## :clipboard: About
The app executes typical SQL statements: Create Database, Create Table, INSERT, DELETE, SELECT etc.  
![image](https://github.com/Andreea2422/miniDBMS/assets/100094242/e1e910da-2aa8-47d1-95e1-be43abed5b3b)

**MongoDB** was used to store the data of the mini DBMS. 

For each table T in the relational DB the following key-value files are produced: 
- T data are stored in a master K-V file T.K-V 
- Key = T.PrimaryKey value 
- Value = concatenation of T non-primary key attributes (columns) 
- for every unique index file UniqInd of T with key UniqKey a new UniqInd.K-V file is created 
    - Key = UniqKey value 
    - Value = T.Key (of master K-V file) 
- for every non-unique index file NonUniqInd of T with key NonUniqKey a new NonUniqInd.K-V file is created 
    - Key = NonUniqKey 
    - Value = T.Key1#T.Key2#...

SQL Join is currently implemented only through the **Hash Join** algorithm.

## :gear: In Progress
- Server-Client component implementation
- Indexed Nested Loop/Sort-Merge join algorithms implementation
- Group by/Having statements implementation

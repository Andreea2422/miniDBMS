# Mini DBMS


> **Master Project - in collaboration with [Buboi Oana](https://github.com/OanaBuboi)**
> 
> **Work in progress**

A mini relational DBMS system developed as part of the DBMS Implementation course.

## :computer: Installation 
### Prerequisites

![Static Badge](https://img.shields.io/badge/MongoDB-%2347A248?logo=mongodb&logoColor=white)

> [!NOTE]
> In order .



## :clipboard: Usage
It 
will have a server and a client component, communicating by a communication 
protocol. 
The client can be a visual interface like SQL Developer (of Oracle) or Management 
Studio of MS SQL Server. You can choose to implement the client in command line 
too. 
The server will execute typical SQL statements: Create Database, Create Table, 
INSERT, DELETE, SELECT, etc. and send a message to the client about the success 
of failure of SQL statement execution. In case of the SELECT statement, the server 
will send the result rows of the statement to the client. In Students.sql you find an 
example of SQL statements your project will have to execute. 
A powerful feature of a DBMS are the index files. B+ trees are the most used 
index types, but it is a highly complex task to implement them. The index files are 
implemented in form of B+ trees in key-value systems, usually. We will use a keyvalue system to store the data of our mini SGBD. Typical statements in a key-value 
system

The tables of the system will be stored in a file of a key-value system, also the index 
files. 
For each table T in the relational DB the following key-value files are produced: 
 T data are stored in a master K-V file T.K-V 
 Key = T.PrimaryKey value 
 Value = concatenation of T non-primary key attributes (columns) 
 for every unique index file UniqInd of T with key UniqKey a new 
UniqInd.K-V file is created 
 Key = UniqKey value 
 Value = T.Key (of master K-V file) 
 for every non-unique index file NonUniqInd of T with key NonUniqKey a new 
NonUniqInd.K-V file is created 
 Key = NonUniqKey 
 Value = T.Key1#T.Key2#...#T.Keyp
Where p is the number of rows with the same NonUniqKey value and T.Keyi
(i=1,..,p) are the primary keys of the rows, which are keys in the master file. 


## In Progress
Server-Client component implementation

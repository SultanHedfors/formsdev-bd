CREATE DATABASE '/firebird/data/your-database.fdb' USER 'sysdba' PASSWORD 'masterkey';
CONNECT '/firebird/data/your-database.fdb' USER 'sysdba' PASSWORD 'masterkey';

CREATE TABLE entry (
                               id INT PRIMARY KEY,
                               name VARCHAR(100),
                               created_at TIMESTAMP
);

INSERT INTO entry (id, name, created_at) VALUES (1, 'Test Name', CURRENT_TIMESTAMP);

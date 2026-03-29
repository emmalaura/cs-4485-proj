# cs-4485-proj

#To install on MAC:
To start you wanna install mysql,
if you have homebrew just put this command in terminal:
brew install mysql
brew services start mysql

1.Then secure your db by putting this in the terminal:

mysql_secure_installation
then its going to ask you questions:
validate password? N
then set ur root password: (could be anything) but i put cs4485project
Remove anonymous users? Y
Disallow root login? Y
Remove test DB? Y
Reload privilege tables? Y

2.Then log in to the DB using these commands in terminal:

mysql -u root -p

3.Type these commands to create the database and user:

CREATE DATABASE CS4485DB;
CREATE USER 'javauser'@'localhost' IDENTIFIED BY 'cs4485';
GRANT ALL PRIVILEGES ON CS4485DB.* TO 'javauser'@'localhost';
FLUSH PRIVILEGES;
exit

4.Then you want to load the schemas, so exit the mysql terminal and go back into the regular terminal and type:

mysql -u javauser -p CS4485DB < (path to the sql file)
then enter the password: cs4485 (unless you chose differently)

5.Then verify your tables by typing this in terminal:

mysql -u javauser -p CS4485DB
SHOW TABLES;
there should be our tables in there.

#To install on Windows:
1. Download MySQL install for windows, run it and select Developer Defualt, set your root password.
2. Add MySQL to PATH. Go youj "Environemnt Varibles" Click "edit the system environemnt varibles", At the bottom there should be a "path" button and click edit then new. Paste the Address of your install location to mysql
   ex.C:\Program Files\MySQL\MySQL Server 8.0\bin
3. Then open Mysql Command line client and enter the root password
4. Create the DB
5. type these commands into the command line
  CREATE DATABASE CS4485DB;
  CREATE USER 'javauser'@'localhost' IDENTIFIED BY 'cs4485';
  GRANT ALL PRIVILEGES ON CS4485DB.* TO 'javauser'@'localhost';
  FLUSH PRIVILEGES;
  exit
6. Load your schema by typing this in a open Command prompt
  mysql -u javauser -p CS4485DB < C:\path\to\schema.sql
   enter the password: cs4485
8. Check to make sure the tables are loaded
     mysql -u javauser -p CS4485DB
     SHOW TABLES;



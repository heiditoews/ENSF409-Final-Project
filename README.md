# ENSF409-Final-Project

This is a project I completed with three teammates for a course I took in Winter 2021. 
It takes a database of used office furniture, and finds the cheapest way to make one or 
more full items from the pieces available, and outputs the result into a text file. 
It runs and takes user input from the command line. 
I contributed the Desk.java and OutputTxt.java files, and parts of the Main.java 
and SystemTest.java files. 


All java files need to be compiled. To run the program, run Main. The program will 
prompt the user to input the database username and password, furniture (chair, lamp etc.), 
type, and quantity one at a time in that order. Furniture and type are both case insensitive; also 
the program will not account for any leading or trailing whitespaces in the user entries.

Please note that the terminal will continue to prompt the user until all inputs are valid.
This includes valid furniture, type and quantity. While prompting the program will try to 
interact and assist the user by displaying the valid entries, whenever the user made an 
incorrect input.

Make sure to include the mySQL-connector, JUnit, and hamcrest jar files in your command 
when compiling and running the program; these jar files are stored inside the lib folder.

The unit tests are in SystemTest.java should be compiled and run. The database should match 
the one created by the inventory.sql file. 

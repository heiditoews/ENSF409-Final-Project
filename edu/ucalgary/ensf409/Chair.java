package edu.ucalgary.ensf409;
import java.sql.*;
import java.lang.StringBuilder;

/**
 @author Ahmed Waly <a href="mailto:ahmed.waly@ucalgary.ca"> ahmed.waly@ucalgary.ca</a>
 @version    2.0
 @since      1.0
 */
/**ENSF 409 Final Project Group 7
 * Chair.java
 * Ahmed Waly, Alexis Hamrak, Andrea Benavides Aguirre, Heidi Toews
 * */
/**class Chair is used to find the cheapest combination to make a 
 * certain amount of chairs that is requested by the user from class Main*/
public class Chair {
	 /**Database URL*/
    public final String DBURL;
    /**Database username*/
    public final String USERNAME;
    /**Database password*/
    public final String PASSWORD;
    
    private int numberOfChairsRequired; //The number of items requested
    private int numberOfChairsAssembled = 0; //The number of items purchased
    
    private String type; //The type of chair that the user has requested
    private String[][] itemsUsed = new String[1][2]; //A 2D array storing the price and id of the items that have been used
    private String[][] items; //A 2D array that Stores the Chairs that are in the database
    private String[][] options = new String[1][4]; //A 2D array that store the options of the completing the transaction
    private String[][] multipleOption = new String[1][5]; //A 2D array that is used to calculate the cheapest option
    private boolean found; //True if the order can be fulfilled and false if not
    private Connection connect = null; //Connection object for connecting to the database
    private ResultSet result = null; //ResultSet object for use with the database

    /**The user defined constructor assigns values for the URL, username, and 
     * password of the database that should be accessed. It also checks if the 
     * type is valid.
     * @param url String of the database URL
     * @param username String of the database username
     * @param password String of the database password
     * @param type String type of Chair that is required
     * @param quantity int of the number of Chair requested
     */
    public Chair(String url, String username, String password, String type, int quantity) {
        type = type.toLowerCase();
        char firstLetter = Character.toUpperCase(type.charAt(0));
        type = String.valueOf(firstLetter) + type.substring(1);
        //Throw an IllegalArgumentException if the type is invalid.
        if (type.equals("Mesh") || type.equals("Executive") || type.equals("Ergonomic") || type.equals("Kneeling") || type.equals("Task")) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("Invalid chair type, please enter mesh, executive, ergonomic, kneeling, or task.");
        }
        //Throw an IllegalArgumentException if the quantity is less than 0
        if (quantity > 0) {
            this.numberOfChairsRequired = quantity;
        } else {
            throw new IllegalArgumentException("Invalid quantity.");
        }
        //store the url, username, and password of the database
        DBURL = url;
        USERNAME = username;
        PASSWORD = password;
        initializeConnection(); // intialize the database connection
    }

    /**
     * This private method is named initializeConnection and it 
     * creates a connection between the Inventory Class 
     * and the local host that has database URL, username, and password that 
     * And if the connection wasn't successfully made, the method will catch the 
     * SQLException and print the stack trace. 
     * This method doesn't take in any arguments and also returns a void.
     */
    public void initializeConnection() {
        try {
            connect = DriverManager.getConnection(DBURL, USERNAME, PASSWORD);
        }
        catch (SQLException e) {
            close();
            System.err.print("Failed to connect to database with url " + DBURL);
            System.err.print(", username " + USERNAME + ", and password " + PASSWORD + "\n");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This private method is named updateDatabase and it updates the local 
     * database by deleting all the chair items that have been purchased.
     * This method doesn't take in any arguments and returns a void.
     */
    public void updateDatabase() {
        PreparedStatement stmt = null;
        try {
            //delete the chair IDs that have been used
            String query = "DELETE FROM Chair WHERE ID = ?";
            stmt = connect.prepareStatement(query);
            for (int i = 0; i < itemsUsed.length; i++) {
                for (int j = 0; j < itemsUsed[i].length; j++) {
                    stmt.setString(1, itemsUsed[i][j]);
                    stmt.executeUpdate();
                    //Re-use stmt to delete all the item needed
                }
            }
            stmt.close();
        }
        catch (SQLException e) {
            close();
            System.err.println("SQLException in updateDatabase.");
            System.exit(1);
        }
    }

    /**
     * This public method is named findChairs and it calculates the cheapest price to 
     * assemble the required number of chairs using the chair components 
     * within the invenory database.
     */
    public void findChairs() {
        result = null;
        items = new String[1][6];
     
        Statement stmt = null;
        //store the chairs that match the type that the user requested
        try {
            stmt = connect.createStatement();
            result = stmt.executeQuery("SELECT * FROM CHAIR");
            int i = 0;
            while(result.next()) {
                if (type.equals(result.getString("Type"))) {
                    if (i >= items.length) {
                        String copy[][] = new String[items.length + 1][6];
                        for (int j = 0; j < items.length; j++) {
                            copy[j] = items[j];
                        }
                        items = copy;
                    }
                    items[i] = new String[6];
                    items[i][0] = result.getString("ID");
                    items[i][1] = result.getString("Legs");
                    items[i][2] = result.getString("Arms");
                    items[i][3] = result.getString("Seat");
                    items[i][4] = result.getString("Cushion");
                    items[i][5] = String.valueOf(result.getInt("Price"));
                    i++;
                } //Get all items of the desired type from the database.
            }
            stmt.close();
        }
        catch (SQLException e) {
            close();
            System.err.println("SQLException in findChairs.");
            System.exit(1);
        }
        found = findOptions(); //Find all the options available (calls findCheapest)
        if (found) {
            //Only update the database if the order can be fulfilled.
            updateDatabase();
        }
        printString();
    }

    /**This private method attempts to find the cheapest way to make the 
     * required number of Chairs with the chair pieces
     * in the inventory and then returns a boolean.
     * @return True if the order can be fulfilled and false otherwise.
     */
    private boolean findOptions() {
        boolean f = false;
        multipleOption = new String[1][6];
        int size = 2;
        for (int i = 1; i < items.length; i++) {
            size = size * 2; //calculate the number of ways to make a chair
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < items.length; j++) {
                if ((i & (1 << j)) > 0){
                    if (index >= multipleOption.length) {
                        //If the multipleOption array isn't big enough, expand it.
                        String copy[][] = new String[multipleOption.length + 1][6];
                        for (int m = 0; m < multipleOption.length; m++) {
                            copy[m] = multipleOption[m];
                        }
                        multipleOption = copy;
                    }
                    multipleOption[index] = items[j];
                    index++;
                }
            }
            if (makesEnough()) {
                addToOptions();
                f = true;
            }
            index = 0;
            multipleOption = new String[1][6];
        }
        if (options[0][0] != null) {
            findCheapest();
        }
        return f;
    }

    /**This private method is named findCheapest finds the cheapest option 
     * from those listed in the options array, returns void. */
    private void findCheapest() {
        int lowest = Integer.parseInt(options[0][0]); //Start with the first option
        int index = 0;
        for (int i = 1; i < options.length && options[i][0] != null; i++) {
            if (Integer.parseInt(options[i][0]) < lowest) {
                //If a cheaper option is found, use that instead
                lowest = Integer.parseInt(options[i][0]);
                index = i; //Keep track of where the cheapest option is
            }
        }
        //Store the IDs and prices in the itemsUsed array
        itemsUsed = new String[options[index].length - 1][2];
        for (int i = 1; i < options[index].length; i++) {
            if (options[index][i] != null) {
                itemsUsed[i-1][0] = options[index][i];
                itemsUsed[i-1][1] = findPrice(options[index][i]);
            }
        }
    }

    /**This private method checks to see if there are enough pieces in multipleOption 
     * to make the required quantity of Chairs.
     * @return True if the order can be fulfilled, false otherwise.
     */
    private boolean makesEnough() {
        int numberLegs = 0;
        int numberArms = 0;
        int numberSeat = 0;
        int numberCushion = 0;
        for (int i = 0; i < multipleOption.length; i++) {
            if (multipleOption[i][1] != null && multipleOption[i][1].equals("Y")) {
                numberLegs++; //Count the number of legs
            }
            if (multipleOption[i][1] != null && multipleOption[i][2].equals("Y")) {
                numberArms++; //Count the number of tops
            }
            if (multipleOption[i][1] != null && multipleOption[i][3].equals("Y")) {
                numberSeat++; //Count the number of drawers
            }
            if (multipleOption[i][1] != null && multipleOption[i][4].equals("Y")) {
                numberCushion++; //Count the number of drawers
            }
        }
        if (numberLegs >= numberOfChairsRequired && numberArms >= numberOfChairsRequired && numberSeat >= numberOfChairsRequired && numberCushion >= numberOfChairsRequired) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This private method named addToOptions adds the IDs of the items in the 
     * multipleOption array to the options array.
     */
    private void addToOptions() {
        int price = 0;
        for (int i = 0; i < multipleOption.length; i++) {
            //Find the cost of the option
            price += Integer.parseInt(multipleOption[i][5]);
        }
        if (numberOfChairsAssembled >= options.length) {
            //If the options array isn't big enough, expand it.
            String copy[][] = new String[options.length + 1][6];
            for (int m = 0; m < options.length; m++) {
                copy[m] = options[m];
            }
            options = copy;
        }
        options[numberOfChairsAssembled] = new String[multipleOption.length];
        options[numberOfChairsAssembled][0] = String.valueOf(price);
        for (int i = 0; i < multipleOption.length; i++) {
            if (i + 1 >= options[numberOfChairsAssembled].length) {
                //If the options array isn't big enough to hold all of the
                //IDs for a particular option, expand it
                String copy[] = new String[options[numberOfChairsAssembled].length + 1];
                for (int m = 0; m < options[numberOfChairsAssembled].length; m++) {
                    copy[m] = options[numberOfChairsAssembled][m];
                }
                options[numberOfChairsAssembled] = copy;
            }
            options[numberOfChairsAssembled][i + 1] = multipleOption[i][0];
        }
        numberOfChairsAssembled++;
    }

    /**Finds the price of an item given its ID. 
     * @param id String of the ID that is needed to find the price
     * @return A String of the price, or 0 if the ID can't be found
     */
    private String findPrice(String id) {
        for (int i = 0; i < items.length; i++) {
            if (id.equals(items[i][0])) {
                return items[i][5];
            }
        }
        return "0";
    }

    /**
     * 	This public method is named close and it ensures that the database 
     * connection and the ResultSet object are properly closed after all the 
     * functions in main.
     */
    public void close() {
        if (connect != null) {
            try { //try to close the connection
                connect.close();
            }
            catch (SQLException e) {
                System.err.print("Failed to close connection to database.");
                System.exit(1);
            }
        }
        if (result != null) { //if the resultSet, attempt to close it
            try {
                result.close();
            }
            catch (SQLException e) {
                System.err.print("Failed to close ResultSet object.");
                System.exit(1);
            }
        }
    }

    /**
     * This public method returns the required information to make the 
     * output file in OutputTxt class.
     * @return The String of the message that will be in the output text file.
     */
    public String getOrderString() {
        //Add the original request
        StringBuilder order = new StringBuilder("Original Request: ");
        order.append(type + " Chair, " + numberOfChairsRequired + "\n\n");
        if (found) {
            //If the order was fulfilled, append the purchased IDs and total price
            order.append("Items Ordered\n");
            for (int i = 0; i < itemsUsed.length; i++) {
                order.append("ID: " + itemsUsed[i][0] + "\n");
            }
            order.append("\nTotal Price: $" + getTotal());
        } else {
            //If the order couldn't be fulfilled, list the manufacturers
            order.append("Order cannot be fulfilled based on current inventory. ");
            order.append("Suggested manufacturers are Office Furnishings, Chairs R Us, ");
            order.append("Furniture Goods, and Fine Office Supplies.");
        }
        return order.toString();
    }

    /**This method returns the String that will be printed to the terminal, which is
     *  the purchased items along with the 
     * price or the suggested manufacturers
     * @return String that will be printed in the terminal*/
    public String printString() {
        StringBuilder order = new StringBuilder();
        if (found) {
            //If the order could be fulfilled, list the IDs and total price
            order.append("Purchase ");
            for (int i = 0; i < itemsUsed.length; i++) {
                order.append(itemsUsed[i][0]);
                if (i < itemsUsed.length - 1) {
                    order.append(" and ");
                }
            }
            order.append(" for $" + getTotal() + ".");
        } else {
            //If the order couldn't be fulfilled, list the manufacturers
            order.append("Order cannot be fulfilled based on current inventory. ");
            order.append("Suggested manufacturers are Office Furnishings, Chairs R Us, ");
            order.append("Furniture Goods, and Fine Office Supplies.");
        }
        return order.toString();
    }

    /**Getter method for the total price of the order
     * @return int of the total price
     */
    public int getTotal() {
        int total = 0;
        for (int i = 0; i < itemsUsed.length; i++) {
            if (itemsUsed[i][1] != null) {
                total += Integer.parseInt(itemsUsed[i][1]);
            }
        }
        return total;
    }

    /**getter method for the type of chair that was required
     * @return String of the Chair type
     */
    public String getType() {
        return type;
    }

    /**
     * getter method for the number of chairs that was required
     * @return Returns an int of the number
     */
    public int getNumberOfChairsRequired() {
        return numberOfChairsRequired;
    }

    /**Getter method for the found data member
     * @return boolean
     */
    public boolean getFound() {
        return found;
    }

    /**Getter method for the items used for Chair
     * @return 2D String array of the Chair items that were used
     */
    public String[][] getItemsUsed() {
        return itemsUsed;
    }

    /**Getter method for a row in the itemsUsed array at the specified index.
     * @param index int of the index needed
     * @return A String array of the row for the Chair item that was used
     */
    public String[] getItemsUsed(int index) {
        return itemsUsed[index];
    }

    /**Getter method for the items in the Chair table
     * @return 2D String array of the Chair items
     */
    public String[][] getItems() {
        return items;
    }
    /**
     * getter method for the the database connection
     * @return Returns a Connection of the database
     */
    public Connection getConnect() {
        return this.connect;
    }
}

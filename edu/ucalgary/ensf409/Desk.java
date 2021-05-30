/**@author Heidi Toews
 * <a>
 * href="mailto:heidi.toews@ucalgary.ca">heidi.toews@ucalgary.ca</a>
 * @version 3.1
 * @since 1.0
 */

/**ENSF 409 Final Project Group 7
Desk.java
Ahmed Waly, Alexis Hamrak, Andrea Benavides Aguirre, Heidi Toews*/

package edu.ucalgary.ensf409;

import java.sql.*;
import java.lang.StringBuilder;

/**Class Desk is used to find the cheapest option for a particular type of desk. */
public class Desk {
    private String type; //The type of desk requested
    private String[][] itemsUsed = new String[1][2]; //itemsUsed[][0] = id, itemsUsed[][1] = price
    private String[][] items; //Stores the desks in the database
    private String[][] options = new String[1][4]; //Stores the ways to make a desk (or desks)
    private String[][] multipleOption = new String[1][5]; //Used when calculating
    private boolean found; //True if the order can be fulfilled and false if not
    //the cheapest way to make multiple desks
    private int quantity; //The number of items requested
    private int number = 0; //The number of items purchased
    /**Database URL*/
    public final String DBURL;
    /**Database username*/
    public final String USERNAME;
    /**Database password*/
    public final String PASSWORD;
    private Connection connect = null; //Connection object for connecting to the database
    private ResultSet result = null; //ResultSet object for use with the database

    /**Constructor that requires a url, username, and password for the database,
     * the desired desk type, and the number of desks required.
     * @param url String of the database URL
     * @param username String of the database username
     * @param password String of the database password
     * @param type String type of Desk that is required
     * @param quantity int of the number of Desks requested
     */
    public Desk(String url, String username, String password, String type, int quantity) {
        type = type.toLowerCase();
        char firstLetter = Character.toUpperCase(type.charAt(0));
        type = String.valueOf(firstLetter) + type.substring(1);
        //Throw an IllegalArgumentException if the type is invalid.
        if (type.equals("Traditional") || type.equals("Adjustable") || type.equals("Standing")) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("Invalid desk type, please enter traditional, adjustable, or standing.");
        }
        //Throw an IllegalArgumentException if the quantity is less than 0
        if (quantity > 0) {
            this.quantity = quantity;
        } else {
            throw new IllegalArgumentException("Invalid quantity.");
        }
        DBURL = url;
        USERNAME = username;
        PASSWORD = password;
        initializeConnection();
    }

    /**Initializes the connection to the database with the stored url, username, and password. */
    public void initializeConnection() {
        try {
            connect = DriverManager.getConnection(DBURL, USERNAME, PASSWORD);
        }
        catch (SQLException e) {
            closeAll();
            System.err.print("Failed to connect to database with url " + DBURL);
            System.err.print(", username " + USERNAME + ", and password " + PASSWORD + "\n");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**Updates the database to reflect the items that have been purchased by 
     * deleting the rows of the items that have been purchased.
    */
    public void updateDatabase() {
        PreparedStatement stmt = null;
        try {
            String query = "DELETE FROM Desk WHERE ID = ?";
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
            closeAll();
            System.err.println("SQLException in updateDatabase.");
            System.exit(1);
        }
    }

    /**Finds the cheapest way to make a desk (or desks) of the desired type.<!-- --> Updates the
     * itemsUsed, options, multipleOption, and found data members.
     */
    public void findDesks() {
        result = null;
        items = new String[1][5];
        //items[][0] = id; items[][1] = legs; items[][2] = top; items[][3] = drawer; items[][4] = price
        Statement stmt = null;
        try {
            stmt = connect.createStatement();
            result = stmt.executeQuery("SELECT * FROM DESK");
            int i = 0;
            while(result.next()) {
                if (type.equals(result.getString("Type"))) {
                    if (i >= items.length) {
                        String copy[][] = new String[items.length + 1][5];
                        for (int j = 0; j < items.length; j++) {
                            copy[j] = items[j];
                        }
                        items = copy;
                    }
                    items[i] = new String[5];
                    items[i][0] = result.getString("ID");
                    items[i][1] = result.getString("Legs");
                    items[i][2] = result.getString("Top");
                    items[i][3] = result.getString("Drawer");
                    items[i][4] = String.valueOf(result.getInt("Price"));
                    i++;
                } //Get all items of the desired type from the database.
            }
            stmt.close();
        }
        catch (SQLException e) {
            closeAll();
            System.err.println("SQLException in getDesk.");
            System.exit(1);
        }
        found = findOptions(); //Find all the options available (calls findCheapest)
        if (found) {
            //Only update the database if the order can be fulfilled.
            updateDatabase();
        }
        printString();
    }

    /**Finds the cheapest way to make the required number of desks with the desks
     * in the inventory.
     * @return True if the order can be fulfilled and false otherwise.
     */
    private boolean findOptions() {
        boolean f = false;
        multipleOption = new String[1][5];
        int size = 2;
        for (int i = 1; i < items.length; i++) {
            size = size * 2;
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < items.length; j++) {
                if ((i & (1 << j)) > 0) {
                    if (index >= multipleOption.length) {
                        //If the multipleOption array isn't big enough, expand it.
                        String copy[][] = new String[multipleOption.length + 1][5];
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
            multipleOption = new String[1][5];
        }
        if (options[0][0] != null) {
            findCheapest();
        }
        return f;
    }

    /**Finds the cheapest option of those listed in the options array, returns void. */
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

    /**Checks to see if the desks in multipleOption have enough pieces to make
     * the required quantity of desks.
     * @return True if the order can be fulfilled, false otherwise.
     */
    private boolean makesEnough() {
        int numberLegs = 0;
        int numberTop = 0;
        int numberDrawer = 0;
        for (int i = 0; i < multipleOption.length; i++) {
            if (multipleOption[i][1] != null && multipleOption[i][1].equals("Y")) {
                numberLegs++; //Count the number of legs
            }
            if (multipleOption[i][1] != null && multipleOption[i][2].equals("Y")) {
                numberTop++; //Count the number of tops
            }
            if (multipleOption[i][1] != null && multipleOption[i][3].equals("Y")) {
                numberDrawer++; //Count the number of drawers
            }
        }
        if (numberLegs >= quantity && numberTop >= quantity && numberDrawer >= quantity) {
            return true;
        } else {
            return false;
        }
    }
    
    /**Adds the IDs of the items in the multipleOption array to the options array
     * (representing a valid option). */
    private void addToOptions() {
        int price = 0;
        for (int i = 0; i < multipleOption.length; i++) {
            //Find the cost of the option
            price += Integer.parseInt(multipleOption[i][4]);
        }
        if (number >= options.length) {
            //If the options array isn't big enough, expand it.
            String copy[][] = new String[options.length + 1][5];
            for (int m = 0; m < options.length; m++) {
                copy[m] = options[m];
            }
            options = copy;
        }
        options[number] = new String[multipleOption.length];
        options[number][0] = String.valueOf(price);
        for (int i = 0; i < multipleOption.length; i++) {
            if (i + 1 >= options[number].length) {
                //If the options array isn't big enough to hold all of the
                //IDs for a particular option, expand it
                String copy[] = new String[options[number].length + 1];
                for (int m = 0; m < options[number].length; m++) {
                    copy[m] = options[number][m];
                }
                options[number] = copy;
            }
            options[number][i + 1] = multipleOption[i][0];
        }
        number++;
    }

    /**Finds the price of an item given its ID. 
     * @param id String of the ID that is needed to find the price
     * @return A String of the price, or 0 if the ID can't be found
     */
    private String findPrice(String id) {
        for (int i = 0; i < items.length; i++) {
            if (id.equals(items[i][0])) {
                return items[i][4];
            }
        }
        return "0";
    }

    /**Attempts to close the Connection object and the ResultSet object. */
    public void closeAll() {
        if (connect != null) {
            try {
                connect.close();
            }
            catch (SQLException e) {
                System.err.print("Failed to close connection to database.");
                System.exit(1);
            }
        }
        if (result != null) {
            try {
                result.close();
            }
            catch (SQLException e) {
                System.err.print("Failed to close ResultSet object.");
                System.exit(1);
            }
        }
    }

    /**Returns a String that will be outputed in the text file.
     * @return A String of the formatted items used and the total price or the 
     * suggested manufacturers
     */
    public String getOrderString() {
        //Add the original request
        StringBuilder order = new StringBuilder("Original Request: ");
        order.append(type + " Desk, " + quantity + "\n\n");
        if (found) {
            //If the order was fulfilled, get the purchased IDs and total price
            order.append("Items Ordered\n");
            for (int i = 0; i < itemsUsed.length; i++) {
                order.append("ID: " + itemsUsed[i][0] + "\n");
            }
            order.append("\nTotal Price: $" + getTotal());
        } else {
            //If the order couldn't be fulfilled, list the manufacturers
            order.append("Order cannot be fulfilled based on current inventory. ");
            order.append("Suggested manufacturers are Academic Desks, Office Furnishings, ");
            order.append("Furniture Goods, and Fine Office Supplies.");
        }
        return order.toString();
    }

    /**Prints a summary of the order to the command line. */
    public void printString() {
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
            order.append("Suggested manufacturers are Academic Desks, Office Furnishings, ");
            order.append("Furniture Goods, and Fine Office Supplies.");
        }
        System.out.println(order);
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

    /**Getter method for the type of Desk that was requested
     * @return String of the Desk type
     */
    public String getType() {
        return type;
    }

    /**Getter method for the quantity of Desks that was requested
     * @return int of the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**Getter method for the found data member
     * @return boolean
     */
    public boolean getFound() {
        return found;
    }

    /**Getter method for the items used for Desks
     * @return 2D String array of the Desk items that were used
     */
    public String[][] getItemsUsed() {
        return itemsUsed;
    }

    /**Getter method for a row in the itemsUsed array at the specified index.
     * @param index int of the index needed
     * @return A String array of the row for the Desk item that was used
     */
    public String[] getItemsUsed(int index) {
        return itemsUsed[index];
    }

    /**Getter method for the items in the Desk table
     * @return 2D String array of the Desk items
     */
    public String[][] getItems() {
        return items;
    }
}

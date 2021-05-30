package edu.ucalgary.ensf409;

/**
 * @author Alexis Hamrak
 * @version 3.6
 * @since 1.0
 */

/*ENSF 409 Final Project Group 7
Filing.java
Ahmed Waly, Alexis Hamrak, Andrea Benavides Aguirre, Heidi Toews*/

import java.sql.*;
import java.lang.StringBuilder;

/**
 * Filing class used for filing types of furniture
 */
public class Filing {
    /**url of database */
    public final String DBURL;
    /**username of database */
    public final String USERNAME;
    /**password of database */
    public final String PASSWORD;
    private Connection dbConnect = null; //Connection object
    private ResultSet result = null; //ResultSet object

    private String type; //String variable corresponding to the type of item to be used
    private String[][] used = new String[1][2]; //2D String array corresponding to the used items
    private String[][] items; //2D String array corresponding to rows of filing table
    private String[] manufacturers = {"Office Furnishings", "Furniture Goods", "Fine Office Supplies"};
    //String array corresponding to each of the manufacturers for filing objects
    private int toBuy; //int corresponding to number of items requested
    private boolean found; //boolean corresponding to whether the pieces were found
    private int itemNum = 0; //The number of items purchased

    private String[][] allOptions = new String[1][4];
    private String[][] multOptions = new String[1][5];

    /**
     * Constructor for filing object. Establishes connection to the database and
	 * stores all local variables. Also checks if the type is valid and throws an
	 * IllegalArgumentException if invalid
	 * @param connect String argument for database url
	 * @param user String argument for username
	 * @param pass String argument for password
	 * @param type String argument for object type
	 * @param items int argument for the number of items
     */
    public Filing(String connect, String user, String pass, String type, int items) {
        this.DBURL = connect;
        this.USERNAME = user;
        this.PASSWORD = pass;
        type = type.toLowerCase();
        char f = Character.toUpperCase(type.charAt(0)); //convert so input is not case sensitive
        type = String.valueOf(f) + type.substring(1).toLowerCase();

        if (type.equals("Small") || type.equals("Medium") || type.equals("Large")) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("Invalid type, please use small, medium or large.");
        }
        if (items > 0) {
            this.toBuy= items;
        } else {
            throw new IllegalArgumentException("Invalid item number.");
        }
        this.initializeConnection();
    }
    
    /**
     * getter for type
     * @return String of type
     */
    public String getType() {
        return type;
    }

    /**
     * getter for toBuy
     * @return int of what number of items we are purchasing
     * */
    public int getToBuy() {
        return toBuy;
    }
/**
 * getetr for found
 * @return boolean of whether item was found
 */
    public boolean getFound(){
        return found;
    }

    /**
     * getter for used
     * @return 2D String array of used items
     * */
    public String[][] getUsed() {
        return used;
    }

     /**
     * getter for items used at specific index
     * @param index int of the index
     * @return String array of a specific table row
     * */
    public String[] getUsed(int index) {
        return used[index];
    }

    /**
     * getter for items
     * @return 2D String array of items from table
     * */
    public String[][] getItems() {
        return items;
    }

    /**
     * getter for manufacturers
     * @return String array of manufacturers
     * */
    public String[] getManufacturers() {
        return manufacturers;
    }

    /**
	 * initializeConnection() establishes a connection with the existing database.
	 * Doesn't return anything. Doesn't take in any arguments
	 */
    public void initializeConnection() {
         try {
            dbConnect = DriverManager.getConnection(DBURL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            close();
            System.err.print("Failed to connect to database with url " + DBURL);
            System.err.print(", username " + USERNAME + ", and password " + PASSWORD + "\n");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Calculates the cheapest way to make a filing system of the given type.
     * This updates which items have been used.
     */
    public void findFiling() {
        result = null;
        items = new String[1][5];
        try {
            Statement stmt = dbConnect.createStatement();
            result = stmt.executeQuery("SELECT * FROM Filing"); //select filing table from database
            int i = 0;
            while(result.next()) {
                if (type.equals(result.getString("Type"))) {
                    if (i >= items.length) {
                        String arr[][] = new String[items.length + 1][5];
                        for (int j = 0; j < items.length; j++) {
                            arr[j] = items[j];
                        }
                        items = arr;
                    }
                    items[i] = new String[5]; //read in all tables and store them locally into items array
                    items[i][0] = result.getString("ID");
                    items[i][1] = result.getString("Rails");
                    items[i][2] = result.getString("Drawers");
                    items[i][3] = result.getString("Cabinet");
                    items[i][4] = String.valueOf(result.getInt("Price"));
                    i++;
                } //read items from the table from the database
            }
            stmt.close();
        }
        catch (SQLException e) {
            close();
            System.err.println("SQLException when reading Filing table.");
            System.exit(1);
        }
        this.found = findOptions();
        if (this.found==true) {
            updateDatabase();
            //update the database if the order can be fulfilled
        }

        printString();
    }

    /**
     * findOptions finds the cheapest way to make the requested number of files given our
     * table.
     * @return boolean corresponding to whether order can be fulfilled (true- yes, false- no)
     */
    private boolean findOptions() {
        boolean f = false;
        this.multOptions = new String[1][5];
        int size = 2;
        for (int i = 1; i < items.length; i++) {
            size = size * 2;
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < items.length; j++) {
                if ((i & (1 << j)) > 0) {
                    if (index >= this.multOptions.length) {
                        //If the multipleOption array isn't big enough, expand it.
                        String copy[][] = new String[this.multOptions.length + 1][5];
                        for (int m = 0; m < this.multOptions.length; m++) {
                            copy[m] = this.multOptions[m];
                        }
                        this.multOptions = copy;
                    }
                    this.multOptions[index] = items[j];
                    index++;
                }
            }
            if (makesEnough()) {//check if we can make enough of the objects requested
                addToOptions();
                f = true;
            }
            index = 0;
            multOptions = new String[1][5];
        }
        if (this.allOptions[0][0] != null) {//calculate cheapest option
            findCheapest();
        }
        return f;
    }

    /**
     * findCheapest finds the cheapest option when more than one filing object 
     * is required. No parameters, returns void
     */
    private void findCheapest() {
        int cheapest = Integer.parseInt(this.allOptions[0][0]); //Start with the first option
        int index = 0;
        for (int i = 1; i < this.allOptions.length && this.allOptions[i][0] != null; i++) {
            if (Integer.parseInt(this.allOptions[i][0]) < cheapest) {
                //If a cheaper option is found, use that instead
                cheapest = Integer.parseInt(this.allOptions[i][0]);
                index = i; //store which index the cheapest option is at
            }
        }
        this.used = new String[this.allOptions[index].length - 1][2];//store all used items in an array
        for (int i = 1; i < this.allOptions[index].length; i++) {
            if (this.allOptions[index][i] != null) {
                this.used[i-1][0] = this.allOptions[index][i];
                this.used[i-1][1] = findPrice(this.allOptions[index][i]);
            }
        }
    }

    /**
     * makesEnough checks to see if we can make the required number of files 
     * using the pieces we have.
     * @return boolean corresponding to whether order can be fulfilled or not
     */
    private boolean makesEnough(){
        int railNum = 0;
        int drawerNum = 0;
        int cabinetNum =0;

        for(int i=0; i < this.multOptions.length; i++){
            if (this.multOptions[i][1] != null && this.multOptions[i][1].equals("Y")) {
                railNum++; //Count the number of legs
            }
            if (this.multOptions[i][1] != null && this.multOptions[i][2].equals("Y")) {
                drawerNum++; //Count the number of tops
            }
            if (this.multOptions[i][1] != null && this.multOptions[i][3].equals("Y")) {
                cabinetNum++; //Count the number of drawers
            }
        }
        if(railNum >= toBuy && drawerNum >= toBuy && cabinetNum >= toBuy){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * addToOptions adds the ID's inside out multOptions array to the array consisting
     * of all options.
     */
    private void addToOptions() {
        int price = 0;
        for (int i = 0; i < this.multOptions.length; i++) {
            price += Integer.parseInt(multOptions[i][4]);
        }
        if (itemNum >= allOptions.length) {
            //If the options array isn't big enough, expand it.
            String copy[][] = new String[this.allOptions.length + 1][5];
            for (int m = 0; m < this.allOptions.length; m++) {
                copy[m] = this.allOptions[m];
            }
            this.allOptions = copy;
        }
        this.allOptions[itemNum] = new String[this.multOptions.length];
        this.allOptions[itemNum][0] = String.valueOf(price);
        for (int i = 0; i < multOptions.length; i++) {
            if (i + 1 >= allOptions[itemNum].length) {
                //If the options array isn't big enough to hold all of the
                //IDs for a particular option, expand it
                String copy[] = new String[this.allOptions[itemNum].length + 1];
                for (int k = 0; k < this.allOptions[itemNum].length; k++) {
                    copy[k] = this.allOptions[itemNum][k];
                }
                this.allOptions[itemNum] = copy;
            }
            this.allOptions[itemNum][i + 1] = this.multOptions[i][0];
        }
        itemNum++;
    }

    /**
     * findPrice finds the price of the item at a given String ID
     * @param id String argument for ID
     * @return returns String of price
     */
    private String findPrice(String id) {
        for (int i = 0; i < items.length; i++) {
            if (id.equals(items[i][0])) {
                return items[i][4];
            }
        }
        return "0";
    }

    /**
     * getOrderString creates the String to be sent for the given order
     * @return returns String consisting of correct formatting
     */
    public String getOrderString() {
        StringBuilder toOrder = new StringBuilder("Original Request: ");
        toOrder.append(type + " Filing, " + toBuy + "\n\n");

        if (found) { //items have been found, send this string as output
            toOrder.append("Items Ordered\n");
            for (int i = 0; i < used.length; i++) {
                toOrder.append("ID: " + used[i][0] + "\n");
            }
            toOrder.append("\nTotal Price: $" + getCost());
        } else { //items not found, send the manufacturers instead
            toOrder.append("Order cannot be fulfilled based on current inventory. ");
            toOrder.append("Suggested manufacturers are Office Furnishings, ");
            toOrder.append("Furniture Goods, and Fine Office Supplies.");
        }
        return toOrder.toString();
    }

    /**
     * Prints the IDs used and the total price or the suggested manufacturers 
     * depending on if the transaction is successful.
     */
    public void printString() {
        StringBuilder order = new StringBuilder();
        if (found) {
            order.append("Purchase ");
            for (int i = 0; i < used.length; i++) {
                order.append(used[i][0]);
                if (i < used.length - 1) {
                    order.append(" and ");
                }
            }
            order.append(" for $" + getCost() + ".");
        } else {
            order.append("Order cannot be fulfilled based on current inventory. ");
            order.append("Suggested manufacturers are Office Furnishings, ");
            order.append("Furniture Goods, and Fine Office Supplies.");
        }

        System.out.println(order);
    }

    /**
     * getter for the total order price
     * @return returns int of the total price
     */
    public int getCost() {
        int total = 0;
        for (int i = 0; i < used.length; i++) {
            if(used[i][1]!=null){
                total += Integer.parseInt(used[i][1]);
            }
        }
        return total;
    }

    
    /**
	 * updateDatabase updates the given database to remove the used items.
	 * This method is only called if there is success in creating an order.
	 */
    public void updateDatabase() {
        PreparedStatement stmt = null;
        try {
            String query = "DELETE FROM Filing WHERE ID = ?";
            stmt = dbConnect.prepareStatement(query);
            for (int i = 0; i < used.length; i++) {
                for (int j = 0; j < used[i].length; j++) {
                    stmt.setString(1, used[i][j]);
                    stmt.executeUpdate();
                }
            }
            stmt.close();
        }
        catch (SQLException e) {
            close();
            System.err.println("SQLException when updating Filing.");
            System.exit(1);
        }
    }
    /**
     * close closes the connection and ResultSet objects. 
     */
    public void close() {
    	try {
			result.close();
			dbConnect.close();
		} catch (SQLException e) {
            System.out.println("Unable to close");
            System.exit(1);
		}
    }

}

package server.topic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Keep & generates a topic.
public class TopicGenerator {
    private List<Topic> topicList = new ArrayList<>();

    public TopicGenerator() throws Exception {
        // JDBC driver & database URL
        final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/Draw";

        // User name and password.
        final String USER = "root";
        final String PASS = "";

        Connection conn = null;
        Statement stmt = null;

        boolean except = false;
        try {
            // Register JDBC driver
            Class.forName(JDBC_DRIVER);

            // Open the database.
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // Execute statement.
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT `name`, `type`,  `length` FROM topics";
            ResultSet rs = stmt.executeQuery(sql);

            // show the results.
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                int length = rs.getInt("length");
                Topic tmp = new Topic(name, type, length);
                topicList.add(tmp);
            }
            // Close database.
            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            // Handle Class.forName error.
            e.printStackTrace();
            except = true;
        } catch (SQLException se) {
            // Handle JDBC error.
            se.printStackTrace();
            except = true;
        } finally {
            // Release source.
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se2) {
                se2.printStackTrace();
                except = true;
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
                except = true;
            }
            // Throw an exception to stop the server.
            if (except)
                throw new Exception();
        }
    }

    public Topic GiveATopic() {
        // Randomly choose a topic.
        Random r = new Random();
        return topicList.get(r.nextInt(topicList.size()));
    }
}

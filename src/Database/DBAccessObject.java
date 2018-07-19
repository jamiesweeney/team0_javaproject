package Database;

import java.sql.*;

public class DBAccessObject {

    public void runSQLQuery() {
        Connection connection = null;
        try {
            connection = DBConnectionManager.getConnection();

            String query = "SELECT * FROM orders";
            PreparedStatement statement = connection.prepareStatement(query);
            runQuery(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnectionManager.closeConnection(connection);
        }
    }

    private void runQuery(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        for(int colIndex = 1; colIndex < resultSetMetaData.getColumnCount(); colIndex++) {
            System.out.print(resultSetMetaData.getColumnName(colIndex) + ", ");
        }
        System.out.println();
        while (resultSet.next()) {
            for(int colIndex = 1; colIndex < resultSetMetaData.getColumnCount(); colIndex++) {
                System.out.print(resultSet.getString(colIndex) + ", ");
            }
            System.out.println();
        }
    }
}

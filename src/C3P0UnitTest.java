import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class C3P0UnitTest {
    private ComboPooledDataSource cpds;

    @Before
    public void setUp() {
        cpds = new ComboPooledDataSource("default-config");
    }

    @After
    public void tearDown() throws SQLException {
        DataSources.destroy(cpds);
    }

    @Test
    public void testDatabaseOperations() throws SQLException {
        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // DDL
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table2 (id INT PRIMARY KEY, name VARCHAR(50), age INT)");

            // 检查主键是否存在
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table2 WHERE id = 1");
            if (rs.next() && rs.getInt(1) == 0) {
                // DML
                stmt.executeUpdate("INSERT INTO test_table2 (id, name, age) VALUES (1, 'lx', 19)");
            }

            stmt.executeUpdate("UPDATE test_table2 SET age = 20 WHERE id = 1");
            rs = stmt.executeQuery("SELECT * FROM test_table2 WHERE id = 1");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("lx", rs.getString("name"));
            assertEquals(20, rs.getInt("age"));
            stmt.executeUpdate("DELETE FROM test_table2 WHERE id = 1");

            // DCL
            stmt.execute("GRANT SELECT ON test_table2 TO lx");
            stmt.execute("REVOKE SELECT ON test_table2 FROM lx");

            // 事务
            conn.setAutoCommit(false);
            stmt.executeUpdate("INSERT INTO test_table2 (id, name, age) VALUES (2, 'yjh', 29)");
            conn.rollback();

            // 存储过程和函数
            stmt.execute("CREATE PROCEDURE add_user(IN user_id INT, IN user_name VARCHAR(50), IN user_age INT) BEGIN INSERT INTO test_table (id, name, age) VALUES (user_id, user_name, user_age); END");
            stmt.execute("CALL add_user(3, 'Alice', 25)");
            stmt.execute("DROP PROCEDURE add_user");

            stmt.execute("CREATE FUNCTION get_user_age(user_id INT) RETURNS INT BEGIN DECLARE user_age INT; SELECT age INTO user_age FROM test_table WHERE id = user_id; RETURN user_age; END");
            rs = stmt.executeQuery("SELECT get_user_age(3)");
            assertTrue(rs.next());
            assertEquals(25, rs.getInt(1));
            stmt.execute("DROP FUNCTION get_user_age");

        }
    }
}

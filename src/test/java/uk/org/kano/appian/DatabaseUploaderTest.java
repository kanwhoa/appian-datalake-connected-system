package uk.org.kano.appian;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.kano.appian.path.Delete;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * A test script for the database uploader
 */
public class DatabaseUploaderTest extends TestBase {
    private static final String[] TABLES = new String[] { "table1", "table2" };
    private static final String PATH = "/appiantest";
    private static final String JDNI_RESOURCE = "jdbc/Appian";

    private DatabaseUploader databaseUploader = new DatabaseUploader();
    private Delete pathDelete = new Delete();

    @Before
    public void createDatabase() throws NamingException, SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:appian;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("sa");

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        Context initCtx = new InitialContext();
        initCtx.createSubcontext("jdbc");
        initCtx.bind(JDNI_RESOURCE, ds);

        Connection conn = ds.getConnection();
        // Create the default tables
        for (String table: TABLES) {
            // Create the table
            String tableCreateSql = "create table " + table + "(id IDENTITY, name varchar(255), amount DECIMAL, created timestamp with time zone)";
            Statement createStatement = conn.createStatement();
            createStatement.execute(tableCreateSql);
            createStatement.close();

            String insertSql = "insert into " + table + "(name, amount, created) values (?, ?, current_timestamp)";
            PreparedStatement insertStatement = conn.prepareStatement(insertSql);

            insertStatement.setString(1, "name \"1\"");
            insertStatement.setBigDecimal(2, new BigDecimal("1"));
            insertStatement.executeUpdate();

            insertStatement.setString(1, null);
            insertStatement.setBigDecimal(2, new BigDecimal("2"));
            insertStatement.executeUpdate();

            insertStatement.close();

            conn.commit();
        }

        // Cleanup
        conn.close();
    }

    //@After
    public void cleanup() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;

        integrationConfiguration = getIntegrationConfiguration(pathDelete);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, PATH);
        values.put(Constants.SC_ATTR_RECURSIVE, Boolean.TRUE);
        setValues(integrationConfiguration, values);
        pathDelete.execute(integrationConfiguration.toConfiguration(), connectedSystemConfiguration.toConfiguration(), null);
    }

    @Test
    public void uploadTables_Success() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;
        IntegrationResponse response;

        integrationConfiguration = getIntegrationConfiguration(databaseUploader);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, PATH);
        values.put(Constants.SC_ATTR_TABLES, Arrays.asList(TABLES));
        values.put(Constants.SC_ATTR_JNDI_RESOURCE, JDNI_RESOURCE);
        setValues(integrationConfiguration, values);

        response = databaseUploader.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
    }
}

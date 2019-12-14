/*
{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package odbfb.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import odbpack.CmdLineArgs;
import odbpack.Odbpack;
import org.firebirdsql.jdbc.FBConnection;
import org.firebirdsql.jdbc.FBDriver;
import org.firebirdsql.jdbc.FirebirdConnection;
import org.firebirdsql.jdbc.FirebirdConnectionProperties;

/**
 *
 * @author RZP16
 */
public class OdbFbDriver extends FBDriver implements Driver {

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return super.getParentLogger(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean jdbcCompliant() {
        return super.jdbcCompliant(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMinorVersion() {
        return super.getMinorVersion(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMajorVersion() {
        return super.getMajorVersion(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return super.getPropertyInfo(url, info); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {

        return url.startsWith("jdbc:odbfb:local:");//super.acceptsURL(url); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FirebirdConnectionProperties newConnectionProperties() {
        return super.newConnectionProperties(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FirebirdConnection connect(FirebirdConnectionProperties properties) throws SQLException {
        return super.connect(properties); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!url.startsWith("jdbc:odbfb:local:")) {
            throw new SQLException("No");
        }
        String tmpfile;
        tmpfile = Odbpack.unpackOdb(url.substring(17), null, new CmdLineArgs());
        return (tmpfile == null ? null : new OdbFirebirdConnection((FBConnection) super.connect("jdbc:firebirdsql:local:" + tmpfile, info), url.substring(17), tmpfile, new CmdLineArgs()));
    }

}

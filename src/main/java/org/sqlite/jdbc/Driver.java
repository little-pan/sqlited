/*
 * Copyright (c) 2021 little-pan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sqlite.jdbc;

import org.sqlite.jdbc.adapter.DriverAdapter;
import org.sqlite.jdbc.rmi.JdbcRMIDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Driver extends DriverAdapter {

    static {
        try {
            Driver driver = new Driver();
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            // Ignore
        }
    }

    private final java.sql.Driver[] drivers = {
            new JdbcRMIDriver() // jdbc:sqlited:[rmi:]
    };

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException {

        for (java.sql.Driver d: this.drivers) {
            if (d.acceptsURL(url)) {
                return d.connect(url, info);
            }
        }

        return null;
    }

}

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

package org.sqlited.jdbc;

import org.junit.Test;
import static junit.framework.TestCase.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.Callable;

public class PerfTest extends BaseTest {

    protected void prepare() throws Exception {
        prepare(null);
    }

    protected void prepare(String url) throws Exception {
        if (url == null) {
            url = getTestUrl();
        }
        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {
            s.executeUpdate("drop table if exists account");
            s.executeUpdate(TBL_ACCOUNT_DDL);
            s.executeUpdate("delete from account");
            s.executeUpdate("insert into account(id, name, balance, create_at) " +
                    "values(1, 'Tom', 5000000, '2021-05-21 20:30:45.000')");
        }
    }

    @Test
    public void test() throws Exception {
        prepare();
        doTest(1);
        doTest(10);
        doTest(100);
        doTest(1000);
    }

    @Test
    public void testConcur() throws Exception {
        prepare();
        doTest(100,10);
        doTest(10,100);
        doTest(20,50);
    }

    @Test
    public void testSQLite() throws Exception {
        String url = getSQLiteUrl();
        prepare(url);
        doTest(1, url);
        doTest(10, url);
        doTest(100, url);
        doTest(1000, url);
    }

    @Test
    public void testConcurSQLite() throws Exception {
        String url = getSQLiteUrl();
        prepare(url);
        doTest(100,10, url);
        doTest(10,100, url);
        doTest(20,50, url);
    }

    @Test
    public void testMySQL() {
        testMySQL(() -> {
            String url = getMySQLUrl();
            prepare(url);
            doTest(1, url);
            doTest(10, url);
            doTest(100, url);
            doTest(1000, url);
        });
    }

    @Test
    public void testConcurMySQL() {
        testMySQL(() -> {
            String url = getMySQLUrl();
            prepare(url);
            doTest(100,10, url);
            doTest(10,100, url);
            doTest(20,50, url);
        });
    }

    void doTest(int times) throws Exception {
        doTest(times, 1, null);
    }

    void doTest(int times, String url) throws Exception {
        doTest(times, 1, url);
    }

    void doTest(int times, int threads) throws Exception {
        doTest(times, threads, null);
    }

    void doTest(int times, int threads, String url) throws Exception {
        Callable<?> callable = () -> {
            String cStr = url == null? getTestUrl(): url;
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            BigDecimal balance = (cStr.startsWith("jdbc:sqlite:")?
                    new BigDecimal("5000000"): new BigDecimal("5000000.0"));
            try (Connection c = DriverManager.getConnection(cStr);
                 Statement s = c.createStatement()) {
                for (int i = 0; i < times; ++i) {
                    String sql = "select id, name, balance, create_at " +
                            "from account where id = 1";
                    ResultSet rs = s.executeQuery(sql);
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("id"));
                    assertFalse(rs.wasNull());
                    assertEquals("Tom", rs.getString("name"));
                    assertFalse(rs.wasNull());
                    assertEquals(balance, rs.getBigDecimal(3));
                    assertFalse(rs.wasNull());
                    assertEquals(df.parse("2021-05-21 20:30:45.000"),
                            rs.getTimestamp("create_at"));
                    assertFalse(rs.wasNull());
                    assertFalse(rs.next());
                    rs.close();
                }
            }

            return null;
        };

        if (threads <= 1) {
            callable.call();
        } else {
            execute(callable, threads, "testConcur");
        }
    }

}
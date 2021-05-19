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

package org.sqlite.rmi.util;

import org.sqlite.util.IOUtils;
import org.sqlite.util.MDUtils;
import org.sqlite.util.logging.LoggerFactory;
import static org.sqlite.rmi.util.SocketUtils.*;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AuthSocket extends Socket {

    static final Logger log = LoggerFactory.getLogger(AuthSocket.class);
    private static final AtomicLong ID = new AtomicLong();

    protected final long id;

    public AuthSocket(Properties props, String host, int port) throws IOException {
        super(host, port);

        boolean failed = true;
        try {
            this.id = nextId();
            login(props, this);
            log.fine(() -> String.format("%s: Create socket#%d",
                    Thread.currentThread().getName(), this.id));
            failed = false;
        } finally {
            if (failed) IOUtils.close(this);
        }
    }

    protected static long nextId() {
        return ID.incrementAndGet();
    }

    @Override
    public void close() throws IOException {
        super.close();
        log.fine(() -> String.format("%s: Close socket#%d",
                Thread.currentThread().getName(), this.id));
    }

    @Override
    public int hashCode() {
        return (int)this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AuthSocket) {
            AuthSocket s = (AuthSocket)o;
            return s.id == this.id;
        } else {
            return false;
        }
    }

    static void login(Properties props, Socket socket) throws IOException {
        int soTimeout = socket.getSoTimeout();

        String loginTimeout = props.getProperty("loginTimeout");
        socket.setSoTimeout(Integer.decode(loginTimeout));
        InputStream is = socket.getInputStream();
        DataInputStream in = new DataInputStream(is);

        byte protocol = in.readByte();
        if (protocol != 0x01) {
            throw new IOException("Unknown server protocol version " + protocol);
        }
        String server = in.readUTF();
        log.fine(() -> String.format("%s handshake", server));
        byte mCode = in.readByte();
        boolean mFound = false;
        for (Map.Entry<String, Byte> i: METHODS.entrySet()) {
            if (mCode == i.getValue()) {
                mFound = true;
                break;
            }
        }
        if (!mFound) {
            throw new IOException("Unknown server auth method " + mCode);
        }
        byte[] challenge = new byte[8];
        in.readFully(challenge);

        // Do login
        String client = props.getProperty("client", "SQLited-jdbc");
        OutputStream os = socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(os);
        MessageDigest md5 = MDUtils.md5();
        md5.update(challenge);
        String password = props.getProperty("password");
        if (password != null) {
            byte[] data = password.getBytes(UTF_8);
            md5.update(data);
        }
        byte[] authData = md5.digest();
        out.writeUTF(client);
        out.writeUTF(props.getProperty("user"));
        out.write(authData);
        out.flush();

        socket.setSoTimeout(soTimeout);
    }
}
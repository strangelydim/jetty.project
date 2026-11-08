//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.io;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.TypeUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransportTest
{
    @Test
    public void testSocketTransportInitializesWhileTcpIpIsInitializing() throws Exception
    {
        try (TcpIpPausingClassLoader loader = new TcpIpPausingClassLoader())
        {
            // One thread starts initializing Transport.TCP_IP, and is paused when it loads Transport.TCPIP.
            FutureTask<Object> tcpIp = start(() -> loader.loadClass(Transport.class.getName()).getField("TCP_IP").get(null));
            assertTrue(loader.paused.await(5, TimeUnit.SECONDS));

            // Another thread initializes a Transport.Socket subclass.
            // If that waits for TCP_IP to finish initializing, the two threads deadlock once the first resumes,
            // because initializing TCP_IP then waits for Transport.Socket, which the second thread is initializing.
            FutureTask<Object> tcpUnix = start(() -> Class.forName(Transport.TCPUnix.class.getName(), true, loader));
            try
            {
                tcpUnix.get(5, TimeUnit.SECONDS);
            }
            finally
            {
                loader.resume.countDown();
            }
            tcpIp.get(5, TimeUnit.SECONDS);
        }
    }

    private static FutureTask<Object> start(Callable<Object> task)
    {
        FutureTask<Object> future = new FutureTask<>(task);
        Thread thread = new Thread(future);
        // A deadlocked thread never finishes, so it must not keep the JVM alive.
        thread.setDaemon(true);
        thread.start();
        return future;
    }

    private static URL location(Class<?> type)
    {
        return type.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * <p>Loads fresh copies of the Transport classes, since a class is initialized only once per class loader,
     * and pauses the thread that loads {@code Transport.TCPIP} until it is told to resume.</p>
     */
    private static class TcpIpPausingClassLoader extends URLClassLoader
    {
        static
        {
            // Lets other threads load classes while one thread is paused.
            registerAsParallelCapable();
        }

        private final CountDownLatch paused = new CountDownLatch(1);
        private final CountDownLatch resume = new CountDownLatch(1);

        private TcpIpPausingClassLoader()
        {
            // The parent is the bootstrap class loader. The platform class loader would return the
            // already loaded Transport classes when this module is on the module path.
            super(new URL[]{location(Transport.class), location(TypeUtil.class)}, null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            if (name.equals(Transport.TCPIP.class.getName()))
            {
                paused.countDown();
                try
                {
                    resume.await();
                }
                catch (InterruptedException x)
                {
                    throw new ClassNotFoundException(name, x);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}

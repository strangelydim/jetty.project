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

/**
 * <p>The {@link Transport} singletons, which {@link Transport} inherits so that
 * they are referenced as {@code Transport.TCP_IP} and {@code Transport.UDP_IP}.</p>
 */
public interface TransportConstants
{
    // These constants must not be declared in Transport, or class initialization can deadlock.
    // Initializing Transport would then create a TCPIP, which needs Transport.Socket initialized.
    // Initializing any Socket subclass, for example by a first new Transport.TCPUnix(path), needs
    // Transport initialized, because Transport declares default methods (JLS 12.4.2).
    // Two threads doing one each would wait for each other forever.
    // Reading a constant initializes only the interface that declares it (JLS 12.4.1),
    // and initializing this interface does not initialize Transport.

    /**
     * <p>The TCP/IP {@code Transport}.</p>
     */
    Transport TCP_IP = new Transport.TCPIP();

    /**
     * <p>The UDP/IP {@code Transport}.</p>
     */
    Transport UDP_IP = new Transport.UDPIP();
}

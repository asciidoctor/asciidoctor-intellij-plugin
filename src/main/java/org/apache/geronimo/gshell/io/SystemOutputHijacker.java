/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.gshell.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Hijacks the systems standard output and error streams on a per-thread basis
 * and redirects to given streams.
 *
 * @version $Rev: 725707 $ $Date: 2008-12-11 16:00:30 +0100 (Thu, 11 Dec 2008) $
 */
public class SystemOutputHijacker
{
  /**
   * Contains a {@link StreamRegistration} for the current thread if its registered, else null.
   */
  private static final InheritableThreadLocal<StreamRegistration> registrations = new InheritableThreadLocal<StreamRegistration>();

  /**
   * The previously installed System streams, initialized when installing.
   */
  private static StreamPair previous;

  /**
   * Flag to indicate if the hijacker is installed or not.
   */
  private static boolean installed;

  /**
   * Check if the hijacker has been installed.
   */
  public static synchronized boolean isInstalled() {
    return installed;
  }

  private static synchronized void ensureInstalled() {
    if (!isInstalled()) {
      throw new IllegalStateException("Not installed");
    }
  }

  /**
   * Install the hijacker.
   */
  public static synchronized void install() {
    if (installed) {
      throw new IllegalStateException("Already installed");
    }

    // Capture the current set of streams
    previous = new StreamPair(System.out, System.err);

    // Install our streams
    System.setOut(new DelegateStream(StreamPair.Type.OUT));
    System.setErr(new DelegateStream(StreamPair.Type.ERR));

    installed = true;
  }

  /**
   * Install the hijacker and register streams for the current thread.
   */
  public static synchronized void install(final PrintStream out, final PrintStream err) {
    install();
    register(out, err);
  }

  /**
   * Install the hijacker and register combinded streams for the current thread.
   */
  public static synchronized void install(final PrintStream out) {
    install();
    register(out);
  }

  /**
   * Install the hijacker and register streams for the current thread.
   */
  public static synchronized void install(final StreamPair pair) {
    install();
    register(pair);
  }

  /**
   * Uninstall the hijacker.
   */
  public static synchronized void uninstall() {
    ensureInstalled();

    System.setOut(previous.out);
    System.setErr(previous.err);

    previous = null;
    installed = false;
  }

  /**
   * Get the current stream registration.
   */
  private static synchronized StreamRegistration registration(final boolean required) {
    if (required) {
      ensureRegistered();
    }

    return registrations.get();
  }

  /**
   * Check if there are streams registered for the current thread.
   */
  public static synchronized boolean isRegistered() {
    return registration(false) != null;
  }

  private static synchronized void ensureRegistered() {
    ensureInstalled();

    if (!isRegistered()) {
      throw new IllegalStateException("Streams not registered for thread: " + Thread.currentThread());
    }
  }

  /**
   * Register streams for the current thread.
   */
  public static synchronized void register(final PrintStream out, final PrintStream err) {
    ensureInstalled();

    StreamRegistration prev = registration(false);

    StreamPair pair = new StreamPair(out, err);

    StreamRegistration next = new StreamRegistration(pair, prev);

    registrations.set(next);
  }

  /**
   * Register combinded streams for the current thread.
   */
  public static synchronized void register(final PrintStream out) {
    register(out, out);
  }

  /**
   * Register streams for the current thread.
   */
  public static synchronized void register(final StreamPair pair) {
    assert pair != null;

    register(pair.out, pair.err);
  }

  /**
   * Reregister streams for the current thread, and restore the previous if any.
   */
  public static synchronized void deregister() {
    StreamRegistration cur = registration(true);

    registrations.set(cur.previous);
  }

  /**
   * Stream registration information.
   */
  private static class StreamRegistration
  {
    public final StreamPair streams;

    public final StreamRegistration previous;

    public StreamRegistration(final StreamPair streams, final StreamRegistration previous) {
      assert streams != null;

      this.streams = streams;
      this.previous = previous;
    }
  }

  /**
   * Returns the currently registered streams.
   */
  private static synchronized StreamPair current() {
    StreamRegistration reg = registration(false);
    if (reg == null) {
      return previous;
    }
    return reg.streams;
  }

  /**
   * Delegates write calls to the currently registered stream.
   */
  private static class DelegateStream
    extends PrintStream
  {
    private static final ByteArrayOutputStream NULL_OUTPUT = new ByteArrayOutputStream();

    private final StreamPair.Type type;

    public DelegateStream(final StreamPair.Type type) {
      super(NULL_OUTPUT);

      assert type != null;

      this.type = type;
    }

    private PrintStream get() {
      return current().get(type);
    }

    @Override
    public void write(final int b) {
      get().write(b);
    }

    @Override
    public void write(final byte b[]) throws IOException {
      get().write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
      get().write(b, off, len);
    }

    @Override
    public void flush() {
      get().flush();
    }

    @Override
    public void close() {
      get().close();
    }
  }

  //
  // System Stream Restoration.  This stuff needs more testing, not sure its working as I'd like... :-(
  //

  /**
   * Restores the System streams to the given pair and resets the hijacker state to uninstalled.
   */
  public static synchronized void restore(final StreamPair streams) {
    assert streams != null;

    StreamPair.system(streams);

    previous = null;
    installed = false;
  }

  /**
   * Restores the original System streams from {@link StreamPair#SYSTEM} and resets
   * the hijacker state to uninstalled.
   */
  public static synchronized void restore() {
    restore(StreamPair.SYSTEM);
  }

}

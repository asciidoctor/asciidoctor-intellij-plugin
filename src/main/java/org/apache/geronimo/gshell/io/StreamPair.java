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

import java.io.PrintStream;

/**
 * Contains a pair of {@link PrintStream} objects.
 *
 * @version $Rev: 705608 $ $Date: 2008-10-17 15:28:45 +0200 (Fri, 17 Oct 2008) $
 */
public class StreamPair {
  public final PrintStream out;

  public final PrintStream err;

  public final boolean combined;

  public StreamPair(final PrintStream out, final PrintStream err) {
    assert out != null;
    assert err != null;

    this.out = out;
    this.err = err;
    this.combined = (out == err);
  }

  public StreamPair(final PrintStream out) {
    assert out != null;

    this.out = out;
    this.err = out;
    this.combined = true;
  }

  public PrintStream get(final Type type) {
    assert type != null;

    return type.get(this);
  }

  public void flush() {
    Flusher.flush(out);

    if (!combined) {
      Flusher.flush(err);
    }
  }

  public void close() {
    Closer.close(out);

    if (!combined) {
      Closer.close(err);
    }
  }

  /**
   * Create a new pair as System is currently configured.
   */
  public static StreamPair system() {
    return new StreamPair(System.out, System.err);
  }

  /**
   * Install the given stream pair as the System streams.
   */
  public static void system(final StreamPair streams) {
    assert streams != null;

    System.setOut(streams.out);
    System.setErr(streams.err);
  }

  /**
   * The original System streams (as they were when this class loads).
   */
  public static final StreamPair SYSTEM = system();

  /**
   * Stream type.
   */
  public static enum Type {
    OUT, ERR;

    private PrintStream get(final StreamPair pair) {
      assert pair != null;

      switch (this) {
        case OUT:
          return pair.out;

        case ERR:
          return pair.err;
      }

      // Should never happen
      throw new InternalError();
    }
  }
}

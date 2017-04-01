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

import java.io.Closeable;
import java.io.IOException;

/**
 * Quietly closes {@link Closeable}s.
 *
 * @version $Rev: 705608 $ $Date: 2008-10-17 15:28:45 +0200 (Fri, 17 Oct 2008) $
 */
public class Closer
{
  public static void close(final Closeable... closeables) {
    if (closeables != null) {
      for (Closeable c : closeables) {
        try {
          c.close();
        }
        catch (IOException e) {
          // ignore
        }
      }
    }
  }
}

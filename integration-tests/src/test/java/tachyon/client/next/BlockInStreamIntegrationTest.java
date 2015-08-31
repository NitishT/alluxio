/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.client.next;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import tachyon.Constants;
import tachyon.TachyonURI;
import tachyon.client.WriteType;
import tachyon.client.next.file.FileInStream;
import tachyon.client.next.file.FileOutStream;
import tachyon.client.next.file.TachyonFS;
import tachyon.client.next.file.TachyonFile;
import tachyon.conf.TachyonConf;
import tachyon.master.next.LocalTachyonCluster;
import tachyon.util.io.BufferUtils;
import tachyon.util.io.PathUtils;

/**
 * Integration tests for <code>tachyon.client.BlockInStream</code>.
 */
public class BlockInStreamIntegrationTest {
  private static final int MIN_LEN = 0;
  private static final int MAX_LEN = 255;
  private static final int MEAN = (MIN_LEN + MAX_LEN) / 2;
  private static final int DELTA = 33;

  private static LocalTachyonCluster sLocalTachyonCluster;
  private static TachyonFS sTfs;
  private static TachyonConf sTachyonConf;

  @AfterClass
  public static final void afterClass() throws Exception {
    sLocalTachyonCluster.stop();
    System.clearProperty("tachyon.user.quota.unit.bytes");
  }

  @BeforeClass
  public static final void beforeClass() throws Exception {
    sLocalTachyonCluster = new LocalTachyonCluster(10000000, 1000, Constants.GB);
    sLocalTachyonCluster.start();
    sTfs = sLocalTachyonCluster.getClient();
    sTachyonConf = sLocalTachyonCluster.getMasterTachyonConf();
  }

  /**
   * Test <code>void read()</code>.
   */
  @Test
  public void newReadTest1() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    for (int k = MIN_LEN; k <= MAX_LEN; k += DELTA) {
      for (ClientOptions op : getOptionSet()) {
        TachyonURI path = new TachyonURI(uniqPath + "/file_" + k + "_" + op);
        FileOutStream os = sTfs.getOutStream(path, op);
        for (int j = 0; j < k; j ++) {
          os.write((byte) j);
        }
        os.close();

        TachyonFile f = sTfs.open(path);

        FileInStream is = sTfs.getInStream(f, op);
        byte[] ret = new byte[k];
        int value = is.read();
        int cnt = 0;
        while (value != -1) {
          Assert.assertTrue(value >= 0);
          Assert.assertTrue(value < 256);
          ret[cnt ++] = (byte) value;
          value = is.read();
        }
        Assert.assertEquals(cnt, k);
        Assert.assertTrue(BufferUtils.equalIncreasingByteArray(k, ret));
        is.close();

        is = sTfs.getInStream(f, op);
        ret = new byte[k];
        value = is.read();
        cnt = 0;
        while (value != -1) {
          Assert.assertTrue(value >= 0);
          Assert.assertTrue(value < 256);
          ret[cnt ++] = (byte) value;
          value = is.read();
        }
        Assert.assertEquals(cnt, k);
        Assert.assertTrue(BufferUtils.equalIncreasingByteArray(k, ret));
        is.close();
      }
    }
  }

  private List<ClientOptions> getOptionSet() {
    List<ClientOptions> ret = new ArrayList<ClientOptions>(10);
    ClientOptions writeBoth =
        new ClientOptions.Builder(sTachyonConf).setCacheType(CacheType.CACHE)
            .setUnderStorageType(UnderStorageType.PERSIST).build();
    ClientOptions writeTachyon =
        new ClientOptions.Builder(sTachyonConf).setCacheType(CacheType.CACHE)
            .setUnderStorageType(UnderStorageType.NO_PERSIST).build();
    ClientOptions writeUnderStore =
        new ClientOptions.Builder(sTachyonConf).setCacheType(CacheType.NO_CACHE)
            .setUnderStorageType(UnderStorageType.PERSIST).build();
    ret.add(writeBoth);
    ret.add(writeTachyon);
    //ret.add(writeUnderStore);
    return ret;
  }
}

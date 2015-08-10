/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.bloomfilter;

import com.google.common.hash.HashCode;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Random;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class TestBloomFilter
{
    @Test
    public void testBloomFilterCreate()
    {
        BloomFilter bf = BloomFilter.newInstance();
        assertNotNull(bf);
    }

    @Test
    public void testBloomFilterElementExists()
    {
        BloomFilter bf = BloomFilter.newInstance();
        bf.put(Slices.wrappedBuffer("robin".getBytes()));
        assertTrue(bf.mightContain(Slices.wrappedBuffer("robin".getBytes())));
        assertFalse(bf.mightContain(Slices.wrappedBuffer("verlangen".getBytes())));
    }

    @Test
    public void testBloomFilterPerformancePut()
    {
        BloomFilter bf = BloomFilter.newInstance();
        long start = new Date().getTime();
        Random rand = new Random();
        byte[] buf = new byte[32];
        for (int i = 0; i < 1000000; i++) {
            rand.nextBytes(buf);
            Slice x = Slices.wrappedBuffer(buf);
            bf.put(x);
        }
        long took = new Date().getTime() - start;
        assertTrue(took < 10000L);
    }

    @Test
    public void testBloomFilterPerformanceContains()
    {
        BloomFilter bf = BloomFilter.newInstance();
        long start = new Date().getTime();
        Random rand = new Random();
        byte[] buf = new byte[32];
        for (int i = 0; i < 1000000; i++) {
            rand.nextBytes(buf);
            Slice x = Slices.wrappedBuffer(buf);
            bf.mightContain(x);
        }
        long took = new Date().getTime() - start;
        assertTrue(took < 10000L);
    }

    @Test
    public void testBloomFilterPerformancePutAndMightContains()
    {
        BloomFilter bf = BloomFilter.newInstance();
        long start = new Date().getTime();
        Random rand = new Random();

        // Load data
        byte[] buf = new byte[4]; // not much of data as we want to find matches
        for (int i = 0; i < 1000000; i++) {
            rand.nextBytes(buf);
            Slice x = Slices.wrappedBuffer(buf);
            bf.put(x);
        }

        // Read data
        int matches = 0;
        for (int i = 0; i < 1000000; i++) {
            rand.nextBytes(buf);
            Slice x = Slices.wrappedBuffer(buf);
            if (bf.mightContain(x)) {
                matches++;
            }
        }

        long took = new Date().getTime() - start;
        assertTrue(took < 10000L);
        assertTrue(matches >= 1);
    }

    @Test
    public void testBloomFilterMerge()
    {
        BloomFilter bf = BloomFilter.newInstance();
        bf.put(Slices.wrappedBuffer("robin".getBytes()));

        BloomFilter bf2 = BloomFilter.newInstance();
        bf2.put(Slices.wrappedBuffer("verlangen".getBytes()));

        // Merge
        bf.putAll(bf2);

        // Check whether contents of the second BF can be found
        assertTrue(bf.mightContain(Slices.wrappedBuffer("verlangen".getBytes())));
    }

    @Test
    public void testBloomFilterHash()
    {
        BloomFilter bf = BloomFilter.newInstance();
        Slice s = bf.serialize();

        // Consistent read test
        HashCode a = BloomFilter.readHash(s);
        HashCode b = BloomFilter.readHash(s);
        assertEquals(a, b);

        // Consistent hash test
        BloomFilter bf2 = BloomFilter.newInstance();
        Slice s2 = bf2.serialize();
        HashCode c = BloomFilter.readHash(s2);
        assertEquals(b, c);
    }

    @Test
    public void testBloomFilterSizeEstimation()
    {
        // Default options (10MM items with 1% error rate)
        BloomFilter bf = BloomFilter.newInstance();
        assertEquals(bf.estimatedInMemorySize(), 11981323);

        // Smaller
        BloomFilter bf2 = BloomFilter.newInstance(100);
        assertEquals(bf2.estimatedInMemorySize(), 120);

        // Smaller with lower false positive percentage
        BloomFilter bf3 = BloomFilter.newInstance(100, 0.001);
        assertEquals(bf3.estimatedInMemorySize(), 180);
    }

    @Test
    public void testBloomFilterHashCodePerformance()
    {
        BloomFilter bf = BloomFilter.newInstance();
        long start = new Date().getTime();
        Slice x = Slices.wrappedBuffer("robin".getBytes());
        bf.put(x);
        Slice bfSer = bf.serialize();
        for (int i = 0; i < 1000000; i++) {
            HashCode h = BloomFilter.readHash(bfSer);
            assertNotNull(h);
        }
        long took = new Date().getTime() - start;
        assertTrue(took < 5000L);
    }
}
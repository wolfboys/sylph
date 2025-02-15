/*
 * Copyright (C) 2018 The Sylph Authors
 *
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
package ideal.sylph.etl.api;

import com.github.harbby.sylph.api.Sink;
import org.junit.Assert;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.List;

public class SinkTest
{
    @Test
    public void genericTest()
    {
        Type[] type = TestSink.class.getGenericInterfaces();

        Type checkType = ParameterizedTypeImpl.make(Sink.class, new Type[] {ParameterizedTypeImpl.make(List.class, new Type[] {String.class}, null)}, null);
        Assert.assertArrayEquals(type, new Type[] {checkType});
    }

    private static class TestSink
            implements Sink<List<String>>
    {
        @Override
        public void run(List<String> stream)
        {
        }
    }
}
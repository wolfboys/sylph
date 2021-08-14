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
package ideal.sylph.runner.flink.etl;

import com.github.harbby.gadtry.base.JavaTypes;
import com.github.harbby.gadtry.ioc.IocFactory;
import ideal.sylph.etl.OperatorType;
import ideal.sylph.etl.Schema;
import ideal.sylph.etl.api.RealTimeSink;
import ideal.sylph.etl.api.RealTimeTransForm;
import ideal.sylph.etl.api.Sink;
import ideal.sylph.etl.api.Source;
import ideal.sylph.etl.api.TransForm;
import ideal.sylph.spi.NodeLoader;
import ideal.sylph.spi.OperatorMetaData;
import ideal.sylph.spi.exception.SylphException;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkState;
import static ideal.sylph.spi.exception.StandardErrorCode.JOB_BUILD_ERROR;
import static java.util.Objects.requireNonNull;

public final class FlinkNodeLoader
        implements NodeLoader<DataStream<Row>>
{
    private static final Logger logger = LoggerFactory.getLogger(FlinkNodeLoader.class);
    private final OperatorMetaData operatorMetaData;
    private final IocFactory iocFactory;

    public FlinkNodeLoader(OperatorMetaData operatorMetaData, IocFactory iocFactory)
    {
        this.operatorMetaData = requireNonNull(operatorMetaData, "binds is null");
        this.iocFactory = requireNonNull(iocFactory, "iocFactory is null");
    }

    @Override
    public UnaryOperator<DataStream<Row>> loadSource(String driverStr, final Map<String, Object> config)
    {
        final Class<?> driverClass = operatorMetaData.getConnectorDriver(driverStr, OperatorType.source);
        checkState(Source.class.isAssignableFrom(driverClass),
                "The Source driver must is Source.class, But your " + driverClass);
        checkDataStreamRow(Source.class, driverClass);

        @SuppressWarnings("unchecked") final Source<DataStream<Row>> source = (Source<DataStream<Row>>) getPluginInstance(driverClass, config);

        return (stream) -> {
            logger.info("source {} schema:{}", driverClass, source.getSource().getType());
            return source.getSource();
        };
    }

    private static void checkDataStreamRow(Class<?> pluginInterface, Class<?> driverClass)
    {
        Type streamRow = JavaTypes.make(DataStream.class, new Type[] {Row.class}, null);
        Type checkType = JavaTypes.make(pluginInterface, new Type[] {streamRow}, null);

        for (Type type : driverClass.getGenericInterfaces()) {
            if (checkType.equals(type)) {
                return;
            }
        }
        throw new IllegalStateException(driverClass + " not is " + checkType + " ,your Generic is " + Arrays.asList(driverClass.getGenericInterfaces()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public UnaryOperator<DataStream<Row>> loadSink(String driverStr, final Map<String, Object> config)
    {
        Class<?> driverClass = operatorMetaData.getConnectorDriver(driverStr, OperatorType.sink);
        checkState(RealTimeSink.class.isAssignableFrom(driverClass) || Sink.class.isAssignableFrom(driverClass),
                "The Sink driver must is RealTimeSink.class or Sink.class, But your " + driverClass);
        if (Sink.class.isAssignableFrom(driverClass)) {
            checkDataStreamRow(Sink.class, driverClass);
        }
        final Object driver = getPluginInstance(driverClass, config);

        final Sink<DataStream<Row>> sink;
        if (driver instanceof RealTimeSink) {
            sink = loadRealTimeSink((RealTimeSink) driver);
        }
        else if (driver instanceof Sink) {
            sink = (Sink<DataStream<Row>>) driver;
        }
        else {
            throw new SylphException(JOB_BUILD_ERROR, "NOT SUPPORTED Sink:" + driver);
        }

        return (stream) -> {
            requireNonNull(stream, "Sink find input stream is null");
            DataStreamSink<?> dataStreamSink = sink.addSink(stream);
            return new DataStreamSinkSupplier(stream.getExecutionEnvironment(), stream.getTransformation(), dataStreamSink);
        };
    }

    private class DataStreamSinkSupplier
            extends DataStream<Row>
            implements Supplier<DataStreamSink<?>>
    {
        private final DataStreamSink<?> dataStreamSink;

        public DataStreamSinkSupplier(StreamExecutionEnvironment environment, Transformation<Row> transformation, DataStreamSink<?> dataStreamSink)
        {
            super(environment, transformation);
            this.dataStreamSink = dataStreamSink;
        }

        @Override
        public DataStreamSink<?> get()
        {
            return dataStreamSink;
        }
    }

    @Override
    public IocFactory getIocFactory()
    {
        return iocFactory;
    }

    /**
     * transform api
     **/
    @SuppressWarnings("unchecked")
    @Override
    public final UnaryOperator<DataStream<Row>> loadTransform(String driverStr, final Map<String, Object> config)
    {
        Class<?> driverClass = operatorMetaData.getConnectorDriver(driverStr, OperatorType.transform);
        checkState(RealTimeTransForm.class.isAssignableFrom(driverClass) || TransForm.class.isAssignableFrom(driverClass),
                "driverStr must is RealTimeSink.class or Sink.class");
        if (TransForm.class.isAssignableFrom(driverClass)) {
            checkDataStreamRow(TransForm.class, driverClass);
        }
        final Object driver = getPluginInstance(driverClass, config);

        final TransForm<DataStream<Row>> transform;
        if (driver instanceof RealTimeTransForm) {
            transform = loadRealTimeTransForm((RealTimeTransForm) driver);
        }
        else if (driver instanceof TransForm) {
            transform = (TransForm<DataStream<Row>>) driver;
        }
        else {
            throw new SylphException(JOB_BUILD_ERROR, "NOT SUPPORTED TransForm:" + driver);
        }

        return (stream) -> {
            requireNonNull(stream, "Transform find input stream is null");
            DataStream<Row> dataStream = transform.transform(stream);
            logger.info("transform {} schema to: {}", driver, dataStream.getType());
            return dataStream;
        };
    }

    private static Sink<DataStream<Row>> loadRealTimeSink(RealTimeSink realTimeSink)
    {
        // or user stream.addSink(new FlinkSink(realTimeSink, stream.getType()));
        //return (Sink<DataStream<Row>>) stream -> stream.addSink(new FlinkSink(realTimeSink, stream.getType())).name(realTimeSink.getClass().getName());
        return new Sink<DataStream<Row>>()
        {
            @Override
            public void run(DataStream<Row> stream)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public <S> S addSink(DataStream<Row> stream)
            {
                return (S) stream.addSink(new FlinkSink(realTimeSink, stream.getType())).name(realTimeSink.getClass().getName());
            }
        };
    }

    private static TransForm<DataStream<Row>> loadRealTimeTransForm(RealTimeTransForm realTimeTransForm)
    {
        return (TransForm<DataStream<Row>>) stream -> {
            final SingleOutputStreamOperator<Row> tmp = stream
                    .flatMap(new FlinkTransFrom(realTimeTransForm, stream.getType()));
            // schema必须要在driver上面指定
            Schema schema = realTimeTransForm.getSchema();
            if (schema != null) {
                RowTypeInfo outPutStreamType = FlinkRecord.parserRowType(schema);
                return tmp.returns(outPutStreamType);
            }
            return tmp;
        };
    }
}

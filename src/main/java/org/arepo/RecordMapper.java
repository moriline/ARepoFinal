package org.arepo;

import org.codejargon.fluentjdbc.api.mapper.ObjectMapperRsExtractor;
import org.codejargon.fluentjdbc.api.query.Mapper;
import org.codejargon.fluentjdbc.internal.mappers.DefaultObjectMapperRsExtractors;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class RecordMapper<T> implements Mapper<T> {
    private final Class<T> type;
    private final Function<String, String> converter;
    private final ConcurrentMap<String, String> converterCache = new ConcurrentHashMap();
    private Map<Class, ObjectMapperRsExtractor> extractors = DefaultObjectMapperRsExtractors.extractors();
    public RecordMapper(Class<T> type) {
        this(type, f -> f.replace("_", ""));
    }

    public RecordMapper(Class<T> type, Function<String, String> converter) {
        this.type = type;
        this.converter = converter;
    }

    private Object value(Class fieldType, ResultSet rs, Integer index) throws SQLException {
        ObjectMapperRsExtractor converter = extractors.get(fieldType);
        Object value = converter != null ? converter.extract(rs, index) : rs.getObject(index);
        return (rs.wasNull() && !fieldType.isPrimitive()) ? null : value;
    }
    private Class<?> typeOfField(Field var1) {
        return var1.getType().equals(Optional.class) ? (Class)((ParameterizedType)var1.getGenericType()).getActualTypeArguments()[0] : var1.getType();
    }
    private void mapColumn(String fieldName, int i, ResultSet rs, T result) throws IllegalArgumentException, SQLException {
        /*
        Field field = fields.get(fieldName);
        if (field != null) {
            Object value = value(typeOfField(field), rs, i);
            setField(field, result, value);
        }*/
    }
    private String convert(String field){
        return converterCache.computeIfAbsent(field, converter);
    }
    @Override
    public T map(ResultSet rs) throws SQLException {
        if(!type.isRecord()) throw new SQLException("is not Record:"+type);
        ResultSetMetaData metadata = rs.getMetaData();
        T result = null;
        var list = type.getDeclaredConstructors();
        //System.out.println("rec list:"+list.toString());
        for(var con : list){
            con.setAccessible(true);
            Map<String, Class<?>> convMap = new HashMap<>();
            Map<String, Object> params = new LinkedHashMap<>();
                /*
                Field[] fields = type.getDeclaredFields();
                for (Field field : fields) {
                    System.out.println("field = " + field.getName()+";"+field.getType().getTypeName());

                }*/
            RecordComponent[] recordComponents = type.getRecordComponents();

            Class<?>[] componentTypes = new Class<?>[recordComponents.length];
            for (int i = 0; i < recordComponents.length; i++) {
                // recordComponents are ordered, see javadoc:
                // "The components are returned in the same order that they are declared in the record header"
                componentTypes[i] = recordComponents[i].getType();
                var name = convert(recordComponents[i].getName());
                params.put(name, null);
                convMap.put(name, componentTypes[i]);
            }

            int count = metadata.getColumnCount();
            for(var i = 1;i<=count;i++){
                var colname = convert(metadata.getColumnName(i));
                var clazz = convMap.get(colname);
                var value = value(clazz, rs, i);
                params.put(colname, value);
            }
            //System.out.println("tmp:"+params+";"+Arrays.toString(params.values().toArray()));
            try {
                result = type.getDeclaredConstructor(componentTypes).newInstance(params.values().toArray());
                //System.out.println("rec inst:"+result);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            break;
        }
        return result;
    }
}
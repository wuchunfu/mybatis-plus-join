package com.github.yulichang.extension.mapping.relation;

import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.extension.mapping.config.DeepConfig;
import com.github.yulichang.extension.mapping.mapper.MPJTableFieldInfo;
import com.github.yulichang.extension.mapping.mapper.MPJTableInfo;
import com.github.yulichang.extension.mapping.mapper.MPJTableInfoHelper;
import com.github.yulichang.extension.mapping.wrapper.MappingQuery;
import com.github.yulichang.toolkit.LambdaUtils;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Relation {

    /**
     * 通过注解实现单表多次查询
     *
     * @param r      BaseMapper执行结果
     * @param config 映射配置
     * @see com.github.yulichang.annotation.EntityMapping
     * @see com.github.yulichang.annotation.FieldMapping
     */
    public static <R, T> R mpjGetRelation(R r, DeepConfig<T> config) {
        int start = ConfigProperties.mappingMaxCount - config.getMaxCount();
        if (Objects.isNull(r)) {
            return null;
        } else if (r instanceof List) {
            List<T> data = (List<T>) r;
            if (CollectionUtils.isEmpty(data)) {
                return r;
            } else {
                T t = data.get(0);
                if (Map.class.isAssignableFrom(t.getClass())) {
                    throw ExceptionUtils.mpe("暂不支持Map类型映射");
                }
                if (Object.class == t.getClass()) {
                    return r;
                }
                return (R) Relation.list(data, config.getProp(), config.isLoop(), start);
            }
        } else if (r instanceof IPage) {
            IPage<T> data = (IPage<T>) r;
            if (!CollectionUtils.isEmpty(data.getRecords())) {
                T t = data.getRecords().get(0);
                if (Map.class.isAssignableFrom(t.getClass())) {
                    throw ExceptionUtils.mpe("暂不支持Map类型映射");
                }
                if (Object.class == t.getClass()) {
                    return r;
                }
                Relation.list(data.getRecords(), config.getProp(), config.isLoop(), start);
            }
            return r;
        } else if (r instanceof Integer) {
            return r;
        } else if (r instanceof Long) {
            return r;
        } else if (r instanceof Boolean) {
            return r;
        } else if (Object.class == r.getClass()) {
            return r;
        } else {
            return (R) Relation.one((T) r, config.getProp(), config.isLoop(), start);
        }
    }

    public static <T> List<T> list(List<T> data, List<SFunction<T, ?>> property, boolean loop, int count) {
        if (CollectionUtils.isEmpty(data)) {
            return data;
        }
        Class<?> entityClass = data.get(0).getClass();
        MPJTableInfo tableInfo = MPJTableInfoHelper.getTableInfo(entityClass);
        if (tableInfo.isHasMappingOrField()) {
            boolean hasProperty = CollectionUtils.isNotEmpty(property);
            List<String> listProperty = hasProperty ? property.stream().map(LambdaUtils::getName).collect(
                    Collectors.toList()) : null;
            for (MPJTableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                if (!hasProperty || listProperty.contains(fieldInfo.getProperty())) {
                    List<Object> itemList = data.stream().map(fieldInfo::thisFieldGet).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(itemList)) {
                        List<?> joinList = MappingQuery.mpjQueryList(fieldInfo.getJoinMapper(), SqlKeyword.IN,
                                fieldInfo.getJoinColumn(), itemList, fieldInfo);
                        data.forEach(i -> mpjBindData(i, property, fieldInfo, joinList, loop, count));
                        fieldInfo.removeJoinField(joinList);
                        if (CollectionUtils.isEmpty(joinList)) {
                            continue;
                        }
                    } else {
                        data.forEach(i -> fieldInfo.fieldSet(i, new ArrayList<>()));
                    }
                }
            }
        }
        return data;
    }


    /**
     * 查询映射关系<br/>
     * 对结果进行二次查询<br/>
     * 可以自行查询然后在通过此方法进行二次查询<br/>
     * list为null或空，会查询全部映射关系<br/>
     *
     * @param t 第一次查询结果
     */
    public static <T> T one(T t, List<SFunction<T, ?>> property, boolean loop, int count) {
        if (t == null) {
            return null;
        }
        MPJTableInfo tableInfo = MPJTableInfoHelper.getTableInfo(t.getClass());
        if (tableInfo.isHasMappingOrField()) {
            boolean hasProperty = CollectionUtils.isNotEmpty(property);
            List<String> list = hasProperty ? property.stream().map(LambdaUtils::getName).collect(
                    Collectors.toList()) : null;
            for (MPJTableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                if (!hasProperty || list.contains(fieldInfo.getProperty())) {
                    Object obj = fieldInfo.thisFieldGet(t);
                    if (obj != null) {
                        List<?> joinList = MappingQuery.mpjQueryList(fieldInfo.getJoinMapper(), SqlKeyword.EQ,
                                fieldInfo.getJoinColumn(), obj, fieldInfo);
                        mpjBindData(t, property, fieldInfo, joinList, loop, count);
                        fieldInfo.removeJoinField(joinList);
                    }
                }
            }
        }
        return t;
    }

    public static <R, T, E> void mpjBindData(R t, List<SFunction<T, ?>> property, MPJTableFieldInfo fieldInfo, List<?> joinList, boolean loop, int count) {
        if (fieldInfo.isMappingEntity()) {
            if (count > ConfigProperties.mappingMaxCount) {
                throw ExceptionUtils.mpe("超过最大查询深度");
            }
            List<E> list = (List<E>) joinList.stream().filter(j -> fieldInfo.joinFieldGet(j).equals(fieldInfo.thisFieldGet(t)))
                    .collect(Collectors.toList());
            MPJTableFieldInfo.bind(fieldInfo, t, list);
            if (loop && CollectionUtils.isNotEmpty(list)) {
                int newCount = count + 1;
                if (CollectionUtils.isNotEmpty(property) && LambdaUtils.getEntityClass(property.get(0)).isAssignableFrom(list.get(0).getClass())) {
                    List<SFunction<E, ?>> property1 = ((List<SFunction<E, ?>>) ((Object) property));
                    list(list, property1, loop, count);
                } else {
                    list(list, Collections.EMPTY_LIST, loop, newCount);
                }
            }
        }
        if (fieldInfo.isMappingField()) {
            MPJTableFieldInfo.bind(fieldInfo, t, joinList.stream().filter(j -> fieldInfo.joinFieldGet(j).equals(
                    fieldInfo.thisFieldGet(t))).map(fieldInfo::bindFieldGet).collect(Collectors.toList()));
        }
    }
}

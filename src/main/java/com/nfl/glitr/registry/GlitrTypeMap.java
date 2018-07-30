package com.nfl.glitr.registry;

import com.nfl.glitr.exception.GlitrException;
import graphql.schema.GraphQLType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.nfl.glitr.util.NamingUtil.compatibleClassName;

/**
 * A map implementation to sync the two kinds of registries currently in {@link com.nfl.glitr.registry.TypeRegistry}.
 * Syncs happen dynamically, keeping the nameRegistry appraised of classRegistry additions to ensure unique GraphQLTypes
 */
public class GlitrTypeMap implements ConcurrentMap {

    private final Map<Class, GraphQLType> classRegistry = new ConcurrentHashMap<>();
    private final Map<String, GraphQLType> nameRegistry = new ConcurrentHashMap<>();


    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (isClass(key)) {
            return classRegistry.getOrDefault(key, (GraphQLType) defaultValue);
        } else if (isString(key)) {
            return nameRegistry.getOrDefault(key, (GraphQLType) defaultValue);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (isClass(key)) {
            nameRegistry.putIfAbsent(compatibleClassName((Class) key), (GraphQLType) value);
            return classRegistry.putIfAbsent((Class) key, (GraphQLType) value);
        } else if (isString(key)) {
            return nameRegistry.putIfAbsent((String) key, (GraphQLType) value);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (isClass(key)) {
            nameRegistry.remove(compatibleClassName((Class) key), value);
            return classRegistry.remove(key, value);
        } else if (isString(key)) {
            return nameRegistry.remove(key, value);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        if (isClass(key)) {
            nameRegistry.replace(compatibleClassName((Class) key), (GraphQLType) oldValue, (GraphQLType) newValue);
            return classRegistry.replace((Class) key, (GraphQLType) oldValue, (GraphQLType) newValue);
        } else if (isString(key)) {
            return nameRegistry.replace((String) key, (GraphQLType) oldValue, (GraphQLType) newValue);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public Object replace(Object key, Object value) {
        if (isClass(key)) {
            nameRegistry.replace(compatibleClassName((Class) key), (GraphQLType) value);
            return classRegistry.replace((Class) key, (GraphQLType) value);
        } else if (isString(key)) {
            return nameRegistry.replace((String) key, (GraphQLType) value);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public boolean isEmpty() {
        return nameRegistry.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (isClass(key)) {
            return classRegistry.containsKey(key);
        } else if (isString(key)) {
            return nameRegistry.containsKey(key);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public boolean containsValue(Object value) {
        return nameRegistry.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if (isClass(key)) {
            return classRegistry.get(key);
        } else if (isString(key)) {
            return nameRegistry.get(key);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public Object put(Object key, Object value) {
        if (isClass(key)) {
            nameRegistry.put(compatibleClassName((Class) key), (GraphQLType) value);
            return classRegistry.put((Class) key, (GraphQLType) value);
        } else if (isString(key)) {
            return nameRegistry.put((String) key, (GraphQLType) value);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public Object remove(Object key) {
        if (isClass(key)) {
            nameRegistry.remove(compatibleClassName((Class) key));
            return classRegistry.remove(key);
        } else if (isString(key)) {
            return nameRegistry.remove(key);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public void clear() {
        classRegistry.clear();
        nameRegistry.clear();
    }

    @Override
    public int size() {
        return nameRegistry.size();
    }

    @Override
    public void putAll(Map m) {
        Set set = m.keySet();
        Object next = set.iterator().next();
        if (isClass(next)) {
            for (Object o : m.entrySet()) {
                Entry pair = (Entry) o;
                nameRegistry.put(compatibleClassName((Class) pair.getKey()), (GraphQLType) pair.getValue());
                classRegistry.put((Class) pair.getKey(), (GraphQLType) pair.getValue());
            }
        } else if (isString(next)) {
            nameRegistry.putAll(m);
        }
        throw new GlitrException("Unsupported type passed as key to GlitrTypeMap");
    }

    @Override
    public Collection values() {
        return nameRegistry.values();
    }

    @Override
    public Set keySet() {
        throw new GlitrException("Unsupported method, GlitrTypeMap does not support this method to ensure expected return types");
    }

    @Override
    public Set<Entry> entrySet() {
        throw new GlitrException("Unsupported method, GlitrTypeMap does not support this method to ensure expected return types");
    }

    public Set ClassKeySet() {
        return classRegistry.keySet();
    }

    public Set NameKeySet() {
        return nameRegistry.keySet();
    }

    public Set<Entry<Class, GraphQLType>> ClassEntrySet() {
        return classRegistry.entrySet();
    }

    public Set<Entry<String, GraphQLType>> NameEntrySet() {
        return nameRegistry.entrySet();
    }

    private boolean isClass(Object obj) {
        return obj instanceof Class;
    }

    private boolean isString(Object obj) {
        return obj instanceof String;
    }
}

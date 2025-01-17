/*
 * Copyright 2003-2007 the original author or authors.
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
package org.grails.orm.hibernate.cfg

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder
import org.hibernate.FetchMode
import org.hibernate.InvalidMappingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.MutablePropertyValues
import org.springframework.context.ApplicationContext
import org.springframework.validation.DataBinder

/**
 * Implements the ORM mapping DSL constructing a model that can be evaluated by the
 * GrailsDomainBinder class which maps GORM classes onto the database.
 *
 * @author Graeme Rocher
 * @since 1.0
 */

class HibernateMappingBuilder implements MappingConfigurationBuilder<Mapping, PropertyConfig>{

    static final Logger LOG = LoggerFactory.getLogger(this)

    Mapping mapping
    String className

    /**
     * Constructor for builder
     *
     * @param className The name of the class being mapped
     */
    HibernateMappingBuilder(String className) {
        this.className = className
    }

    HibernateMappingBuilder(Mapping mapping, String className) {
        this.mapping = mapping
        this.className = className
    }

    @Override
    Map<String, PropertyConfig> getProperties() {
        return mapping.columns
    }

    /**
     * Central entry point for the class. Passing a closure that defines a set of mappings will evaluate said mappings
     * and populate the "mapping" property of this class which can then be obtained with getMappings()
     *
     * @param mappingClosure The closure that defines the ORM DSL
     */

    @Override
    @CompileStatic
    Mapping evaluate(Closure mappingClosure, Object context = null) {
        if (mapping == null) {
            mapping = new Mapping()
        }
        mappingClosure.resolveStrategy = Closure.DELEGATE_ONLY
        mappingClosure.delegate = this
        try {
            if(context != null) {
                mappingClosure.call(context)
            }
            else {
                mappingClosure.call()
            }
        }
        finally {
            mappingClosure.delegate = null
        }
        mapping
    }
    /**
     * Include another config in this one
     */
    @CompileStatic
    void includes(Closure callable) {
        if (!callable) {
            return
        }

        callable.resolveStrategy = Closure.DELEGATE_ONLY
        callable.delegate = this
        try {
            callable.call()
        }
        finally {
            callable.delegate = null
        }
    }
    @CompileStatic
    void hibernateCustomUserType(Map args) {
        if (args.type && (args['class'] instanceof Class)) {
            mapping.userTypes[args['class']] = args.type
        }
    }

    /**
     * <p>Configures the table name. Example:
     * <code> { table 'foo' }
     *
     * @param name The name of the table
     */
    @CompileStatic
    void table(String name) {
        mapping.tableName = name
    }

    /**
    * <p>Configures the discriminator name. Example:
    * <code> { discriminator 'foo' }
    *
    * @param name The name of the table
    */
    @CompileStatic
    void discriminator(String name) {
        mapping.discriminator = name
    }

    /**
     * <p>Configures the discriminator name. Example:
     * <code> { discriminator name:'foo', column:'type' }
     *
     * @param name The name of the table
     */
    @CompileStatic
    void discriminator(Map args) {
        mapping.discriminator = args?.remove('value')
        if (args.column instanceof String) {
            mapping.discriminatorColumn = new ColumnConfig(name:args.column.toString())
        }
        else if (args.column instanceof Map) {
            ColumnConfig config = new ColumnConfig()
            DataBinder dataBinder = new DataBinder(config)
            dataBinder.bind(new MutablePropertyValues((Map)args.column))
            mapping.discriminatorColumn = config
        }
        mapping.discriminatorMap.type = args?.remove('type')
        mapping.discriminatorMap.insert = args?.remove('insert')
        mapping.discriminatorMap.formula = args?.remove('formula')
    }

    /**
     * <p>Configures whether to auto import packages domain classes in HQL queries. Default is true
     * <code> { autoImport false }
     */
    @CompileStatic
    void autoImport(boolean b) {
        mapping.autoImport = b
    }

    /**
     * <p>Configures the table name. Example:
     * <code> { table name:'foo', schema:'dbo', catalog:'CRM' }
     */
    @CompileStatic
    void table(Map tableDef) {
        mapping.table.name = tableDef?.name?.toString()
        mapping.table.schema = tableDef?.schema?.toString()
        mapping.table.catalog = tableDef?.catalog?.toString()
    }

    /**
     * <p>Configures the default sort column. Example:
     * <code> { sort 'foo' }
     *
     * @param name The name of the property to sort by
     */
    @CompileStatic
    void sort(String name) {
        if (name) {
            mapping.sort.name = name
        }
    }

    void autowire(boolean autowire) {
        mapping.autowire = autowire
    }

    /**
     * Whether to use dynamic update queries
     */
    @CompileStatic
    void dynamicUpdate(boolean b) {
        mapping.dynamicUpdate = b
    }

    /**
     * Whether to use dynamic update queries
     */
    @CompileStatic
    void dynamicInsert(boolean b) {
        mapping.dynamicInsert = b
    }

    /**
     * <p>Configures the default sort column. Example:
     * <code> { sort foo:'desc' }
     *
     * @param namesAndDirections The names and directions of the property to sort by
     */
    @CompileStatic
    void sort(Map namesAndDirections) {
        if (namesAndDirections) {
            mapping.sort.namesAndDirections = namesAndDirections
        }
    }

    /**
     * Configures the batch-size used for lazy loading
     * @param num The batch size to use
     */
    @CompileStatic
    void batchSize(Integer num) {
        if (num) {
            mapping.batchSize = num
        }
    }

    /**
     * <p>Configures the default sort direction. Example:
     * <code> { order 'desc' }
     *
     * @param name The name of the property to sort by
     */
    @CompileStatic
    void order(String direction) {
        if ("desc".equalsIgnoreCase(direction) || "asc".equalsIgnoreCase(direction)) {
            mapping.sort.direction = direction
        }
    }

    /**
     * Set whether auto time stamping should occur for last_updated and date_created columns
     */
    @CompileStatic
    void autoTimestamp(boolean b) {
        mapping.autoTimestamp = b
    }

    /**
     * <p>Configures whether to use versioning for optimistic locking
     * <code> { version false }
     *
     * @param isVersioned True if a version property should be configured
     */
    @CompileStatic
    void version(boolean isVersioned) {
        mapping.versioned = isVersioned
    }

    /**
     * <p>Configures the name of the version column
     * <code> { version 'foo' }
     *
     * @param isVersioned True if a version property should be configured
     */
    @CompileStatic
    void version(String versionColumn) {
        PropertyConfig pc = mapping.getPropertyConfig("version")
        if (!pc) {
            pc = new PropertyConfig()
            mapping.columns['version'] = pc
        }
        pc.columns << new ColumnConfig(name:versionColumn)
    }

    /**
     * <p>Configures the second-level cache for the class
     * <code> { cache usage:'read-only', include:'all' }
     *
     * @param args Named arguments that contain the "usage" and/or "include" parameters
     */
    @CompileStatic
    void cache(Map args) {
        mapping.cache = new CacheConfig(enabled:true)
        if (args.usage) {
            if (CacheConfig.USAGE_OPTIONS.contains(args.usage)) {
                mapping.cache.usage = args.usage
            }
            else {
                LOG.warn("ORM Mapping Invalid: Specified [usage] with value [$args.usage] of [cache] in class [$className] is not valid")
            }
        }
        if (args.include) {
            if (CacheConfig.INCLUDE_OPTIONS.contains(args.include)) {
                mapping.cache.include = args.include
            }
            else {
                LOG.warn("ORM Mapping Invalid: Specified [include] with value [$args.include] of [cache] in class [$className] is not valid")
            }
        }
    }

    /**
     * <p>Configures the second-level cache for the class
     * <code> { cache 'read-only' }
     *
     * @param usage The usage type for the cache which is one of CacheConfig.USAGE_OPTIONS
     */
    @CompileStatic
    void cache(String usage) {
        cache(usage:usage)
    }

    /**
     * <p>Configures the second-level cache for the class
     * <code> { cache 'read-only', include:'all }
     *
     * @param usage The usage type for the cache which is one of CacheConfig.USAGE_OPTIONS
     */
    @CompileStatic
    void cache(String usage, Map args) {
        args = args ? args : [:]
        args.usage = usage
        cache(args)
    }

    /**
     * If true the class and its sub classes will be mapped with table per hierarchy mapping
     */
    @CompileStatic
    void tablePerHierarchy(boolean isTablePerHierarchy) {
        mapping.tablePerHierarchy = isTablePerHierarchy
    }

    /**
     * If true the class and its subclasses will be mapped with table per subclass mapping
     */
    @CompileStatic
    void tablePerSubclass(boolean isTablePerSubClass) {
        mapping.tablePerHierarchy = !isTablePerSubClass
    }

    /**
     * If true the class and its subclasses will be mapped with table per subclass mapping
     */
    @CompileStatic
    void tablePerConcreteClass(boolean isTablePerConcreteClass) {
        if(isTablePerConcreteClass) {
            mapping.tablePerHierarchy = false
            mapping.tablePerConcreteClass = true
        }
    }


    /**
     * <p>Configures the second-level cache with the default usage of 'read-write' and the default include of 'all' if
     *  the passed argument is true
     *
     * <code> { cache true }
     *
     * @param shouldCache True if the default cache configuration should be applied
     */
    @CompileStatic
    void cache(boolean shouldCache) {
        mapping.cache = new CacheConfig(enabled:shouldCache)
    }

    /**
     * <p>Configures the identity strategy for the mapping. Examples
     *
     * <code>
     *    { id generator:'sequence' }
     *    { id composite: ['one', 'two'] }
     * </code>
     *
     * @param args The named arguments to the id method
     */
    void id(Map args) {
        if (args.composite) {
            mapping.identity = new CompositeIdentity(propertyNames:args.composite as String[])
            if (args.compositeClass) {
                mapping.identity.compositeClass = args.compositeClass
            }
        }
        else {
            if (args?.generator) {
                mapping.identity.generator = args.remove('generator')
            }
            if (args?.name) {
                mapping.identity.name = args.remove('name').toString()
            }
            if (args?.params) {
                def params = args.remove('params')
                for (entry in params) {
                    params[entry.key] = entry.value?.toString()
                }
                mapping.identity.params = params
            }
            if (args?.natural) {
                def naturalArgs = args.remove('natural')
                def propertyNames = naturalArgs instanceof Map ? naturalArgs.remove('properties') : naturalArgs

                if (propertyNames) {
                    def ni = new NaturalId()
                    ni.mutable = (naturalArgs instanceof Map) && naturalArgs.mutable ?: false
                    if (propertyNames instanceof List) {
                        ni.propertyNames = propertyNames
                    }
                    else {
                        ni.propertyNames = [propertyNames.toString()]
                    }
                    mapping.identity.natural = ni
                }
            }
            // still more arguments?
            if (args) {
                handleMethodMissing("id", [args] as Object[])
            }
        }
    }

    /**
     * A closure used by methodMissing to create column definitions
     */
    private handleMethodMissing = { String name, args ->
        if (args && ((args[0] instanceof Map) || (args[0] instanceof Closure))) {
            def namedArgs = args[0] instanceof Map ? args[0] : [:]
            PropertyConfig property = mapping.columns[name] ?: new PropertyConfig()
            property.formula = namedArgs.formula ?: property.formula
            property.type = namedArgs.type ?: property.type
            property.setLazy( namedArgs.lazy instanceof Boolean ? namedArgs.lazy : property.getLazy() )
            property.insertable = namedArgs.insertable != null ? namedArgs.insertable : property.insertable
            property.updateable = namedArgs.updateable != null ? namedArgs.updateable : property.updateable
            property.cascade = namedArgs.cascade ?: property.cascade
            property.sort = namedArgs.sort ?: property.sort
            property.order = namedArgs.order ?: property.order
            property.batchSize = namedArgs.batchSize instanceof Integer ? namedArgs.batchSize : property.batchSize
            property.ignoreNotFound = namedArgs.ignoreNotFound != null ? namedArgs.ignoreNotFound : property.ignoreNotFound
            property.typeParams = namedArgs.params ?: property.typeParams
            property.setUnique( namedArgs.unique ? namedArgs.unique : property.unique)
            property.nullable = namedArgs.nullable ? true : property.nullable
            property.maxSize = namedArgs.maxSize instanceof Number ? namedArgs.maxSize : property.maxSize
            property.minSize = namedArgs.minSize instanceof Number ? namedArgs.minSize : property.minSize
            property.max = namedArgs.max instanceof Comparable ? namedArgs.max : property.max
            property.min = namedArgs.min instanceof Comparable ? namedArgs.min : property.min
            property.range = namedArgs.range instanceof ObjectRange ? namedArgs.range : null
            property.scale = namedArgs.scale instanceof Integer ? namedArgs.scale : property.scale
            property.inList = namedArgs.inList instanceof List ? namedArgs.inList : property.inList

            if (namedArgs.fetch) {
                switch(namedArgs.fetch) {
                    case ~/(join|JOIN)/:
                        property.fetch = FetchMode.JOIN; break
                    case ~/(select|SELECT)/:
                        property.fetch = FetchMode.SELECT; break
                    default:
                        property.fetch = FetchMode.DEFAULT
                }
            }

            // Deal with any column configuration for this property.
            if (args[-1] instanceof Closure) {
                // Multiple column definitions for this property.
                Closure c = args[-1]
                c.delegate = new PropertyDefinitionDelegate(property)
                c.resolveStrategy = Closure.DELEGATE_ONLY
                c.call()
            }
            else {
                // There is no sub-closure containing multiple column
                // definitions, so pick up any column settings from
                // the argument map.
                def cc
                if (property.columns) {
                    cc = property.columns[0]
                }
                else {
                    cc = new ColumnConfig()
                    property.columns << cc
                }

                if (namedArgs["column"]) cc.name = namedArgs["column"]
                if (namedArgs["sqlType"]) cc.sqlType = namedArgs["sqlType"]
                if (namedArgs["enumType"]) cc.enumType = namedArgs["enumType"]
                if (namedArgs["index"]) cc.index = namedArgs["index"]
                if (namedArgs["unique"]) cc.unique = namedArgs["unique"]
                if (namedArgs["read"]) cc.read = namedArgs["read"]
                if (namedArgs["write"]) cc.write = namedArgs["write"]
                if (namedArgs.defaultValue) cc.defaultValue = namedArgs.defaultValue
                if (namedArgs.comment) cc.comment = namedArgs.comment
                cc.length = namedArgs["length"] ?: -1
                cc.precision = namedArgs["precision"] ?: -1
                cc.scale = namedArgs["scale"] ?: -1
            }

            if (namedArgs.cache instanceof String) {
                CacheConfig cc = new CacheConfig()
                if (CacheConfig.USAGE_OPTIONS.contains(namedArgs.cache)) {
                    cc.usage = namedArgs.cache
                }
                else {
                    LOG.warn("ORM Mapping Invalid: Specified [usage] of [cache] with value [$args.usage] for association [$name] in class [$className] is not valid")
                }
                property.cache = cc
            }
            else if (namedArgs.cache == true) {
                property.cache = new CacheConfig()
            }
            else if (namedArgs.cache instanceof Map) {
                def cacheArgs = namedArgs.cache
                CacheConfig cc = new CacheConfig()
                if (CacheConfig.USAGE_OPTIONS.contains(cacheArgs.usage)) {
                    cc.usage = cacheArgs.usage
                }
                else {
                    LOG.warn("ORM Mapping Invalid: Specified [usage] of [cache] with value [$args.usage] for association [$name] in class [$className] is not valid")
                }
                if (CacheConfig.INCLUDE_OPTIONS.contains(cacheArgs.include)) {
                    cc.include = cacheArgs.include
                }
                else {
                    LOG.warn("ORM Mapping Invalid: Specified [include] of [cache] with value [$args.include] for association [$name] in class [$className] is not valid")
                }
                property.cache = cc
            }

            if (namedArgs.indexColumn) {
                def pc = new PropertyConfig()
                property.indexColumn = pc
                def cc = new ColumnConfig()
                pc.columns << cc
                def indexColArg = namedArgs.indexColumn
                if (indexColArg instanceof Map) {
                    if (indexColArg.type) {
                        pc.type = indexColArg.remove('type')
                    }
                    bindArgumentsToColumnConfig(indexColArg, cc)
                }
                else {
                    cc.name = indexColArg.toString()
                }
            }
            if (namedArgs.joinTable) {
                def join = new JoinTable()
                def joinArgs = namedArgs.joinTable
                if (joinArgs instanceof String) {
                    join.name = joinArgs
                }
                else if (joinArgs instanceof Map) {
                    if (joinArgs.schema) join.schema = joinArgs.remove('schema')
                    if (joinArgs.catalog) join.catalog = joinArgs.remove('catalog')
                    if (joinArgs.name) join.name = joinArgs.remove('name')
                    if (joinArgs.key) {
                        join.key = new ColumnConfig(name:joinArgs.remove('key'))
                    }
                    if (joinArgs.column) {
                        ColumnConfig cc = new ColumnConfig(name: joinArgs.column)
                        join.column = cc
                        bindArgumentsToColumnConfig(joinArgs, cc)
                    }
                }
                property.joinTable = join
            }
            else if (namedArgs.containsKey('joinTable') && namedArgs.joinTable == false) {
                property.joinTable = null
            }

            mapping.columns[name] = property
        }
    }

    private bindArgumentsToColumnConfig(argMap, ColumnConfig cc) {
        argMap.each { k, v ->
            if (cc.metaClass.hasProperty(cc, k)) {
                try {
                    cc."$k" = v
                }
                catch (Exception e) {
                    LOG.warn("Parameter [$k] cannot be used with [joinTable] argument")
                }
            }
        }
    }

    /**
     * <p>Consumes the columns closure and populates the value into the Mapping objects columns property
     *
     * @param callable The closure containing the column definitions
     */
    @CompileStatic
    void columns(Closure callable) {
        callable.resolveStrategy = Closure.DELEGATE_ONLY
        callable.delegate = new Object() {
            def invokeMethod(String methodName, args) {
                handleMethodMissing(methodName, args)
            }
        }
        callable.call()
    }

    @CompileStatic
    void datasource(String name) {
        mapping.datasources = [name]
    }

    @CompileStatic
    void datasources(List<String> names) {
        mapping.datasources = names
    }

    @CompileStatic
    void comment(String comment) {
        mapping.comment = comment
    }

    void methodMissing(String name, Object args) {
        boolean hasArgs = args.asBoolean()
        if ('user-type' == name && hasArgs && (args[0] instanceof Map)) {
            hibernateCustomUserType(args[0])
        }
        else if('importFrom' == name && hasArgs && (args[0] instanceof Class)) {
            // ignore, handled by constraints
        }
        else if (args && ((args[0] instanceof Map) || (args[0] instanceof Closure))) {
            handleMethodMissing(name, args)
        }
        else {
            LOG.error "ORM Mapping Invalid: Specified config option [$name] does not exist for class [$className]!"
        }
    }
}


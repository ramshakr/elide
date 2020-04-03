/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.dynamic.config;

import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.utils.ClassScanner;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

/**
 * Dynamic Configuration For Elide Services. Override any of the beans (by
 * defining your own) and setting flags to disable in properties to change the
 * default behavior.
 */

@Slf4j
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.dynamic-config.enabled:false}")
public class ElideDynamicConfiguration {

    @Autowired
    private ElideConfigProperties configProperties;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory (EntityManagerFactoryBuilder builder,
            DataSource source, JpaProperties jpaProperties, HibernateProperties hibernateProperties) {

        try {

            //Map for Persistent Unit properties
            Map<String, Object> puiPropertyMap = new HashMap<>();

            //Bind entity classes from classpath
            ArrayList<Class> bindClasses = new ArrayList<>();
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Entity.class));

            //Map of JPA Properties
            Map<String, String> jpaPropMap = jpaProperties.getProperties();
            String hibernateGetDDLAuto = hibernateProperties.getDdlAuto();

            if (jpaPropMap.get("hibernate.hbm2ddl.auto") == null && hibernateGetDDLAuto != null) {
               jpaPropMap.put("hibernate.hbm2ddl.auto", hibernateGetDDLAuto);
             }

            ElideDynamicEntityCompiler compiler = new ElideDynamicEntityCompiler
                    (configProperties.getDynamicConfig().getPath());
            compiler.compile(configProperties.getDynamicConfig().getPath());

            Collection<ClassLoader> classLoaders = new ArrayList<>();
            classLoaders.add(compiler.getClassLoader());

            //Add dynamic classes to Pui Map
            puiPropertyMap.put(AvailableSettings.CLASSLOADERS, classLoaders);
            //Add classpath entity model classes to Pui Map
            puiPropertyMap.put(AvailableSettings.LOADED_CLASSES, bindClasses);

            //pui properties from pui map
            Properties puiProps = new Properties();
            puiProps.putAll(puiPropertyMap);

            //Create Elide dynamic Persistence Unit
            ElideDynamicPersistenceUnit elideDynamicPersistenceUnit =
                    new ElideDynamicPersistenceUnit("default", ElideDynamicEntityCompiler.classNames, puiProps,
                    compiler.getClassLoader());
            elideDynamicPersistenceUnit.setNonJtaDataSource(source);
            elideDynamicPersistenceUnit.setJtaDataSource(source);

            new HibernateJpaVendorAdapter();

            LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            bean.setJpaVendorAdapter(vendorAdapter);

            //Add JPA Properties from Application.yaml
            bean.setJpaPropertyMap(jpaPropMap);

            //Add Classes
            bean.setJpaPropertyMap(puiPropertyMap);

            bean.setPersistenceUnitManager(new PersistenceUnitManager() {
                @Override
                public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException {
                    return elideDynamicPersistenceUnit;
                }

                @Override
                public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
                        throws IllegalArgumentException, IllegalStateException {
                    return elideDynamicPersistenceUnit;
                }
            });

            return bean;
        } catch (Exception e) {
            log.error("Setting up Dynamic Configuration failed " + e.getMessage());
            return null;
        }
    }
}

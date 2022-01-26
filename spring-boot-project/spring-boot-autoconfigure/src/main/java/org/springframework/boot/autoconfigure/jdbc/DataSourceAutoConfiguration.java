/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 1.0.0
 *
 * 如果暂时不需要数据源配置，可以通过自动配置的排除功能，将本类排除。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })		// 环境条件符合
@EnableConfigurationProperties(DataSourceProperties.class)		// 装配配置文件信息
@Import({ DataSourcePoolMetadataProvidersConfiguration.class, DataSourceInitializationConfiguration.class })	// 引入自动配置类
public class DataSourceAutoConfiguration {

	// 内嵌数据库
	@Configuration(proxyBeanMethods = false)
	@Conditional(EmbeddedDatabaseCondition.class)		// 不能支持池化的数据源
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedDatabaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(PooledDataSourceCondition.class)		// 设置了spring.datasource.type或PooledDataSourceAvailableCondition。
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	// 支持Hikari、Tomcat JDBC、Dbcp2三种DataSource
	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.Generic.class,
			DataSourceJmxConfiguration.class })
	protected static class PooledDataSourceConfiguration {

	}

	/**
	 * {@link AnyNestedCondition} that checks that either {@code spring.datasource.type}
	 * is set or {@link PooledDataSourceAvailableCondition} applies.
	 *
	 * 检查是否设置了spring.datasource.type或PooledDataSourceAvailableCondition。
	 */
	static class PooledDataSourceCondition extends AnyNestedCondition {

		PooledDataSourceCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);		// 表示被@Configuration注解的类是解析阶段的判断条件
		}

		// 配置条件判断
		@ConditionalOnProperty(prefix = "spring.datasource", name = "type")
		static class ExplicitType {

		}

		// 判断条件
		@Conditional(PooledDataSourceAvailableCondition.class)
		static class PooledDataSourceAvailable {

		}

	}

	/**
	 * {@link Condition} to test if a supported connection pool is available.
	 *
	 * 检查类加载器中是否存在指定的数据源对象。
	 */
	static class PooledDataSourceAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("PooledDataSource");
			// 指定的类加载器中存在默认指定的数据源，存在则匹配
			if (DataSourceBuilder.findType(context.getClassLoader()) != null) {		// 任意有一个就行
				return ConditionOutcome.match(message.foundExactly("supported DataSource"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("supported DataSource").atAll());
		}

	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 * If a pooled {@link DataSource} is available, it will always be preferred to an
	 * {@code EmbeddedDatabase}.
	 *
	 * 检测何时可以使用内嵌的DataSource。
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";

		private final SpringBootCondition pooledCondition = new PooledDataSourceCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("EmbeddedDataSource");
			if (hasDataSourceUrlProperty(context)) {
				return ConditionOutcome.noMatch(message.because(DATASOURCE_URL_PROPERTY + " is set"));
			}
			// 支持池化的数据源，返回不匹配
			if (anyMatches(context, metadata, this.pooledCondition)) {
				return ConditionOutcome.noMatch(message.foundExactly("supported pooled data source"));
			}
			// 判断是否存在合适的内嵌数据库类型。
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(context.getClassLoader()).getType();	// cl下是否有SpringBoot支持的内嵌数据库
			if (type == null) {
				return ConditionOutcome.noMatch(message.didNotFind("embedded database").atAll());
			}
			// 枚举类中存在
			return ConditionOutcome.match(message.found("embedded database").items(type));
		}

		private boolean hasDataSourceUrlProperty(ConditionContext context) {
			Environment environment = context.getEnvironment();
			if (environment.containsProperty(DATASOURCE_URL_PROPERTY)) {
				try {
					return StringUtils.hasText(environment.getProperty(DATASOURCE_URL_PROPERTY));
				}
				catch (IllegalArgumentException ex) {
					// Ignore unresolvable placeholder errors
				}
			}
			return false;
		}

	}

}

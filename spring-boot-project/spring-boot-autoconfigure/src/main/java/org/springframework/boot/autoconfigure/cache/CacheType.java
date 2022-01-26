/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

/**
 * Supported cache types (defined in order of precedence).
 *
 * 支持的缓存类型，按照优先级定义。
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.3.0
 */
public enum CacheType {

	/**
	 * Generic caching using 'Cache' beans from the context.
	 *
	 * 使用上下文中的Cache Bean进行通用缓存操作。
	 */
	GENERIC,

	/**
	 * JCache (JSR-107) backed caching.
	 *
	 * JSR-107支持的缓存。
	 */
	JCACHE,

	/**
	 * EhCache backed caching.
	 *
	 * EhCache支持的缓存。
	 */
	EHCACHE,

	/**
	 * Hazelcast backed caching.
	 */
	HAZELCAST,

	/**
	 * Infinispan backed caching.
	 */
	INFINISPAN,

	/**
	 * Couchbase backed caching.
	 */
	COUCHBASE,

	/**
	 * Redis backed caching.
	 */
	REDIS,

	/**
	 * Caffeine backed caching.
	 */
	CAFFEINE,

	/**
	 * Simple in-memory caching.
	 */
	SIMPLE,

	/**
	 * No caching.
	 */
	NONE

}

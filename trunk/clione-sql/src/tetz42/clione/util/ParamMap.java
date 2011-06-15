/*
 * Copyright 2011 tetsuo.ohta[at]gmail.com
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
package tetz42.clione.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamMap extends HashMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6103186429868779178L;

	public static final Pattern KEY_PTN = Pattern.compile("([\"-),-~ ])[ -~]*");
	public static final Pattern SYMBOL_PTN = Pattern.compile("[^A-Za-z0-9]+");
	public static final Object ON = new Object();

	@Override
	public Object get(Object key) {
		return super.get(convKey(key));
	}

	@Override
	public Object put(String key, Object value) {
		return super.put(convKey(key), value);
	}

	public ParamMap $(String key, Object value) {
		this.put(key, value);
		return this;
	}

	public <T> ParamMap $(String key, T... values) {
		this.put(key, values);
		return this;
	}

	public ParamMap $e(String key, Object value) {
		if (ClioneUtil.isNotEmpty(value))
			this.put(key, value);
		return this;
	}

	public <T> ParamMap $e(String key, T... values) {
		for (T value : values) {
			if (ClioneUtil.isNotEmpty(value))
				this.put(key, value);
		}
		return this;
	}

	public ParamMap $on(String... keys) {
		for (String key : keys)
			this.put(key, ON);
		return this;
	}

	public ParamMap object(Object obj) {
		if (obj == null)
			return null;
		else if (obj instanceof Map<?, ?>)
			map((Map<?, ?>) obj);
		else
			bean(obj);
		return this;
	}

	public ParamMap map(Map<?, ?> map) {
		for (Map.Entry<?, ?> e : map.entrySet()) {
			this.put(String.valueOf(e.getKey()), e.getValue());
		}
		return this;
	}

	public ParamMap bean(Object bean) {
		Class<?> clazz = bean.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field f : clazz.getDeclaredFields()) {
				try {
					if (this.containsKey(f.getName()))
						continue;
					boolean backup = f.isAccessible();
					f.setAccessible(true);
					this.put(f.getName(), f.get(bean));
					f.setAccessible(backup);
				} catch (IllegalArgumentException e) {
					// ignore the exception
				} catch (IllegalAccessException e) {
					// ignore the exception
				}
			}
			clazz = clazz.getSuperclass();
		}
		return this;
	}

	private String convKey(Object key) {
		if (!String.class.isInstance(key))
			throw new UnsupportedOperationException(
					"Parameter key must be String.");
		return convKey((String) key);
	}

	private String convKey(String key) {
		if (key == null)
			throw new NullPointerException("Parameter key must not be null.");
		Matcher keyM = KEY_PTN.matcher(key);
		if (!keyM.matches())
			throw new UnsupportedOperationException("Uusupported key : " + key);
		Matcher symM = SYMBOL_PTN.matcher(keyM.group(1));
		if (symM.matches())
			key = key.substring(keyM.group(1).length());
		return key;
	}
}
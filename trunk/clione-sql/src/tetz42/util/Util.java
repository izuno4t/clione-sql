package tetz42.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Util {

	public static final <K, V> HashMap<K, V> newHashMap(){
		return new HashMap<K, V>();
	}

	public static final <K, V> ConcurrentHashMap<K, V> newConcurrentMap(){
		return new ConcurrentHashMap<K, V>();
	}

	public static final <E> ArrayList<E> newArrayList(){
		return new ArrayList<E>();
	}

	public static boolean isContains(Object key, Object... values) {
		for (Object value : values) {
			if (key.equals(value))
				return true;
		}
		return false;
	}
}
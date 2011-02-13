package tetz42.clione;

import static tetz42.clione.SQLManager.*;
import static tetz42.clione.util.ClioneUtil.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tetz42.clione.exception.WrapException;
import tetz42.clione.gen.SQLGenerator;
import tetz42.clione.node.LineNode;
import tetz42.clione.parsar.SQLParser;

public class SQLExecutor {

	private final SQLManager manager;
	private final int hashValue;
	final SQLGenerator sqlGenerator;
	private String resourceInfo = null;

	final List<LineNode> lineTreeList;

	PreparedStatement stmt;
	ResultSet rs;

	SQLExecutor(SQLManager manager, InputStream in) {
		this.manager = manager;
		this.lineTreeList = new SQLParser(resourceInfo).parse(in);
		this.sqlGenerator = new SQLGenerator(manager.getNullValues());
		this.hashValue = (int) (Math.random() * Integer.MAX_VALUE);
	}

	void setResourceInfo(String resourceInfo) {
		this.resourceInfo = resourceInfo;
		this.sqlGenerator.setResourceInfo(resourceInfo);
	}

	public Map<String, Object> find() throws SQLException {
		return this.find((Map<String, Object>) null);
	}

	@Override
	public int hashCode() {
		return hashValue;
	}

	public Map<String, Object> find(Object paramObj) throws SQLException {
		return this.find(params(paramObj));
	}

	public Map<String, Object> find(Map<String, Object> paramMap)
			throws SQLException {
		// TODO ���Iterator�Ή�������A������Ƀ��W�b�N��ύX(Fetch 1 �ŌĂяo��)
		List<Map<String, Object>> list = this.findAll(paramMap);
		return list.size() != 0 ? list.get(0) : null;
	}

	public List<Map<String, Object>> findAll() throws SQLException {
		return this.findAll((Map<String, Object>) null);
	}

	public List<Map<String, Object>> findAll(Object paramObj)
			throws SQLException {
		return this.findAll(params(paramObj));
	}

	public List<Map<String, Object>> findAll(Map<String, Object> paramMap)
			throws SQLException {

		ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			stmt = this.genStmt(paramMap);
			rs = stmt.executeQuery();
			ResultSetMetaData md = rs.getMetaData();

			while (rs.next()) {
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					map.put(md.getColumnLabel(i), rs.getObject(i));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			throw new SQLException(
					e.getMessage() + "\n\tsql:" + this.sqlGenerator.sql
							+ "\n\t" + this.sqlGenerator.params,
					e.getSQLState(), e.getErrorCode(), e);
		} finally {
			this.closeStatement();
		}
		return list;
	}

	public <T> T find(Class<T> entityClass) throws SQLException {
		return this.find(entityClass, (Map<String, Object>) null);
	}

	public <T> T find(Class<T> entityClass, Object paramObj)
			throws SQLException {
		return this.find(entityClass, params(paramObj));
	}

	public <T> T find(Class<T> entityClass, Map<String, Object> paramMap)
			throws SQLException {
		// TODO ���Iterator�Ή�������A������Ƀ��W�b�N��ύX(Fetch 1 �ŌĂяo��)
		List<T> list = this.findAll(entityClass, paramMap);
		return list.size() != 0 ? list.get(0) : null;
	}

	public <T> List<T> findAll(Class<T> entityClass) throws SQLException {
		return this.findAll(entityClass, null);
	}

	public <T> List<T> findAll(Class<T> entityClass, Object paramObj)
			throws SQLException {
		return this.findAll(entityClass, params(paramObj));
	}

	public <T> List<T> findAll(Class<T> entityClass,
			Map<String, Object> paramMap) throws SQLException {

		// TODO ���Iterator�Ή�������A������Ƀ��W�b�N��ύX
		ArrayList<T> list = new ArrayList<T>();
		try {
			stmt = this.genStmt(paramMap);
			rs = stmt.executeQuery();
			ResultSetMetaData md = rs.getMetaData();
			Class<?> clazz = entityClass;
			HashMap<String, FSet> fieldMap = new HashMap<String, FSet>();
			while (clazz != null && clazz != Object.class) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					if (fieldMap.containsKey(field.getName()))
						continue;
					fieldMap.put(field.getName(),
							FSet.genFSet(field, field.isAccessible()));
				}
				clazz = clazz.getSuperclass();
			}

			while (rs.next()) {
				T instance = entityClass.newInstance();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					FSet fset = fieldMap.get(md.getColumnLabel(i));
					if (fset == null)
						fset = fieldMap.get(conv(md.getColumnLabel(i)));
					if (fset == null)
						continue;
					fset.f.setAccessible(true);
					fset.f.set(instance, getSQLData(fset.f, rs, i));
					fset.f.setAccessible(fset.b);
				}
				list.add(instance);
			}
		} catch (SQLException e) {
			throw new SQLException(
					e.getMessage() + "\n\tsql:" + this.sqlGenerator.sql
							+ "\n\t" + this.sqlGenerator.params,
					e.getSQLState(), e.getErrorCode(), e);
		} catch (InstantiationException e) {
			throw new WrapException(entityClass.getSimpleName()
					+ " must have default constructor.", e);
		} catch (IllegalAccessException e) {
			throw new WrapException(entityClass.getSimpleName()
					+ " have security problem.", e);
		} finally {
			this.closeStatement();
		}
		return list;
	}

	public int update() throws SQLException {
		return this.update((Map<String, Object>) null);
	}

	public int update(Object paramObj) throws SQLException {
		return this.update(params(paramObj));
	}

	public int update(Map<String, Object> paramMap) throws SQLException {
		try {
			stmt = this.genStmt(paramMap);
			return stmt.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException(
					e.getMessage() + "\n\tsql:" + this.sqlGenerator.sql
							+ "\n\t" + this.sqlGenerator.params, e);
		} finally {
			this.closeStatement();
		}
	}

	public void closeStatement() throws SQLException {
		if (rs != null) {
			rs.close();
			rs = null;
		}
		if (stmt != null) {
			stmt.close();
			stmt = null;
		}
		manager.removeExecutor(this);
	}

	public String getExecutedSql() {
		return this.sqlGenerator.sql;
	}

	public List<Object> getExecutedParams() {
		return this.sqlGenerator.params;
	}

	public String genSql() {
		return genSql(null);
	}

	public String genSql(Map<String, Object> paramMap) {
		String sql = sqlGenerator.genSql(paramMap, lineTreeList);
		manager.setInfo(resourceInfo, sql, sqlGenerator.params);
		return sql;
	}

	PreparedStatement genStmt(Map<String, Object> paramMap)
			throws SQLException {
		stmt = manager.con().prepareStatement(genSql(paramMap));
		manager.putExecutor(this);
		int i = 1;
		for (Object param : this.sqlGenerator.params) {
			stmt.setObject(i++, param);
		}
		return stmt;
	}

	private Object conv(String columnLabel) {
		String[] strings = columnLabel.toLowerCase().split("_");
		StringBuilder sb = new StringBuilder();
		for (String s : strings) {
			if (s.length() == 0)
				continue;
			if (sb.length() == 0)
				sb.append(s);
			else
				sb.append(s.substring(0, 1).toUpperCase() + s.substring(1));
		}
		return sb.toString();
	}

	private static class FSet {
		Field f;
		boolean b;

		static FSet genFSet(Field f, boolean b) {
			FSet fset = new FSet();
			fset.f = f;
			fset.b = b;
			return fset;
		}
	}
}

package com.joe.springrediscachequickstart.entity;

import java.io.Serializable;

public class Category implements Serializable{
	
	public enum Type{
		common, self
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1289950669856890598L;
	private Long id;
	private String name;
	private Type type;
	public Category(Long id, String name) {
		super();
		this.id = id;
		this.name = name;
		this.type = Type.common;
	}
	public Category(Long id, String name, Type type) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
}

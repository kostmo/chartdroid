package com.kostmo.market.revenue.container;

public class UsernamePassword {
	private String username, password;

	public UsernamePassword(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUserName() {
		return username;
	}

	public String getPassword() {
		return password;
	}
}

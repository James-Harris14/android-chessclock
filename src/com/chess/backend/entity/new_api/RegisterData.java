package com.chess.backend.entity.new_api;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 21.12.12
 * Time: 6:51
 */
public class RegisterData {
/*
	"user_id": 23900,
	"login_token": "058dfd8f10a7961e10112de0e3eaf779"
*/
	private String login_token;
	private long user_id;

	public String getLogin_token() {
		return login_token;
	}

	public void setLogin_token(String login_token) {
		this.login_token = login_token;
	}

	public long getUser_id() {
		return user_id;
	}

	public void setUser_id(long user_id) {
		this.user_id = user_id;
	}

}
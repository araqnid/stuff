package org.araqnid.stuff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import com.google.common.base.Optional;

@Singleton
public class MerlotRepository {
	private final Map<UUID, User> users = new HashMap<>();
	private final Map<String, UUID> usersByName = new HashMap<>();

	public MerlotRepository() {
		createUser("alice@example.com", "Alice Aintree", "w0nderland".toCharArray());
		createUser("bob@example.com", "Uncle Bob", "b0b".toCharArray());
		createUser("ivan@example.com", "Иван Иванович Иванов", "s0v.b0i".toCharArray());
	}

	public synchronized User createUser(String username, String commonName, char[] password) {
		if (usersByName.containsKey(username)) throw new IllegalArgumentException("Username already used: " + username);
		User user = new User();
		user.id = UUID.randomUUID();
		user.username = username;
		user.commonName = commonName;
		user.password = password;
		users.put(user.id, user);
		usersByName.put(user.username, user.id);
		return user;
	}

	public synchronized Optional<User> findUserById(UUID id) {
		return Optional.fromNullable(users.get(id));
	}

	public synchronized Optional<User> findUserByName(String username) {
		for (User user : users.values()) {
			if (user.username.equals(username)) return Optional.of(user);
		}
		return Optional.absent();
	}

	public synchronized void updateUser(User user) {
		User existing = users.get(user.id);
		if (existing == null) throw new IllegalArgumentException("User id not found: " + user.id);
		if (!existing.username.equals(user.username)) {
			if (usersByName.containsKey(user.username)) throw new IllegalArgumentException("Username already used: "
					+ user.username);
			usersByName.remove(existing.username);
			usersByName.put(user.username, user.id);
		}
		users.put(user.id, user);
	}

	public synchronized boolean deleteUser(UUID userId) {
		User user = users.get(userId);
		if (user == null) return false;
		users.remove(user.id);
		usersByName.remove(user.username);
		return true;
	}

	public static class User {
		public UUID id;
		public String username;
		public String commonName;
		public char[] password;
	}
}

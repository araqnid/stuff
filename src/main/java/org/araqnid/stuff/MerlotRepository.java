package org.araqnid.stuff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Optional;
import com.google.inject.Singleton;

@Singleton
public class MerlotRepository {
	private final Map<UUID, User> users = new HashMap<>();

	public synchronized User createUser(String username, String commonName) {
		User user = new User();
		user.id = UUID.randomUUID();
		user.username = username;
		user.commonName = commonName;
		users.put(user.id, user);
		return user;
	}

	public synchronized Optional<User> findUserById(UUID id) {
		return Optional.fromNullable(users.get(id));
	}

	public synchronized void updateUser(User user) {
		if (!users.containsKey(user.id)) throw new IllegalArgumentException();
		users.put(user.id, user);
	}

	public synchronized void deleteUser(User user) {
		users.remove(user.id);
	}

	public synchronized boolean deleteUser(UUID id) {
		return users.remove(id) != null;
	}

	public static class User {
		public UUID id;
		public String username;
		public String commonName;
	}
}

package org.araqnid.stuff.testutil;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;

public final class RandomData {
	private static final Random RANDOM = new Random();

	private RandomData() {
	}

	public static String randomEmailAddress() {
		List<String> tlds = ImmutableList.of("com", "net", "org", "co.uk", "org.uk");
		String tld = tlds.get(RANDOM.nextInt(tlds.size()));
		return randomString() + "@" + randomString() + ".example." + tld;
	}

	public static String randomString(String prefix) {
		return prefix + "-" + randomString();
	}

	public static String randomString() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int len = 10;
		StringBuilder builder = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
		}
		return builder.toString();
	}

	public static <T extends Enum<T>> T randomEnumInstance(Class<T> enumClass) {
		return pickOne(EnumSet.allOf(enumClass));
	}

	public static <T extends Enum<T>> T randomOtherInstanceOfEnum(Class<T> enumClass, T excludedValue) {
		return pickOne(EnumSet.complementOf(EnumSet.of(excludedValue)));
	}

	public static <T> T pickOne(Set<T> values) {
		int index = new Random().nextInt(values.size());
		Iterator<T> iter = values.iterator();
		while (index-- > 0) {
			iter.next();
		}
		return iter.next();
	}
}

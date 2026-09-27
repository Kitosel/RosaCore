package pl.kiosel.rosacore.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberUtils {

	public static boolean isInt(String number) {
		try {
			Integer.parseInt(number);
			return true;
		} catch (NumberFormatException ignore) {
			return false;
		}
	}

	public static boolean isLong(String value) {
		try {
			if (value == null) return false;
			Long.parseLong(value);
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	public static int parseInt(String number, int... fallback) {
		int fallbackValue = fallback == null || fallback.length == 0 ? 0 : fallback[0];
		if (number == null || number.isEmpty()) return fallbackValue;
		if (isInt(number)) return Integer.parseInt(number);
		return fallbackValue;
	}

	public static long parseLongOrZero(String value) {
		try {
			return value == null ? 0L : Long.parseLong(value);
		} catch (NumberFormatException ignored) {
			return 0L;
		}
	}

	public static long safeAdd(long first, long second) {
		if (second > 0L && first > Long.MAX_VALUE - second) {
			return Long.MAX_VALUE;
		}
		if (second < 0L && first < Long.MIN_VALUE - second) {
			return Long.MIN_VALUE;
		}
		return first + second;
	}

	public static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	public static double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	public static long clampSeconds(long value, long minimum, long maximum) {
		return Math.min(maximum, Math.max(minimum, value));
	}

	public static String formatTps(double tps) {
		if (!Double.isFinite(tps)) return "0.0";
		return formatNumber(Math.max(0.0D, Math.min(20.0D, tps)), 1);
	}

	public static String formatNumber(Number value) {
		return formatNumber(value, 2);
	}

	public static double requirePositiveOrZero(double value, String field) {
		if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
			throw new IllegalArgumentException(field + " must be a finite number greater than or equal to zero");
		}
		return value;
	}

	public static String formatNumber(Number value, int precision) {
		if (value == null) {
			return "";
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		DecimalFormat df = new DecimalFormat("#,##0", symbols);

		df.setMinimumFractionDigits(precision);
		df.setMaximumFractionDigits(precision);

		return df.format(value);
	}
}

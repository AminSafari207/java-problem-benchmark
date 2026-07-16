package org.example.jpb.core.shared.constants;

public final class ModelLimits {

	public static final int ID_MIN_LENGTH = 1;
	public static final int ID_MAX_LENGTH = 64;

	public static final int DISPLAY_NAME_MIN_LENGTH = 1;
	public static final int DISPLAY_NAME_MAX_LENGTH = 120;

	private ModelLimits() {
		throw new AssertionError("No instances");
	}
}

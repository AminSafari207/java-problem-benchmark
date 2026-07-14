package org.example.jpb.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.example.jpb.core.model.SolutionInvoker;

public final class ReflectionExecutor {

	private ReflectionExecutor() {
		System.out.println("ReflectionExecutor must not be instantiated");
	}

	public static Object invoke(Object instance, Method method, Object... args) {
		try {
			if (!method.canAccess(instance)) {
				method.setAccessible(true);
			}

			return method.invoke(instance, args);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();

			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}

			if (cause instanceof Error error) {
				throw error;
			}

			throw new RuntimeException(cause);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static SolutionInvoker createInvoker(Object instance, Method method) {
		if (instance == null) {
			throw new IllegalArgumentException("instance must not be null");
		}

		if (method == null) {
			throw new IllegalArgumentException("method must not be null");
		}

		if (!method.canAccess(instance)) {
			method.setAccessible(true);
		}

		return arguments -> invoke(instance, method, arguments);
	}
}

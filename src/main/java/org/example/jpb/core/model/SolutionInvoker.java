package org.example.jpb.core.model;

@FunctionalInterface
public interface SolutionInvoker {
	Object invoke(Object[] arguments);
}

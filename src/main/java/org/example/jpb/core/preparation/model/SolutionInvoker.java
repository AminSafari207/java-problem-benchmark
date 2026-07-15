package org.example.jpb.core.preparation.model;

@FunctionalInterface
public interface SolutionInvoker {
	Object invoke(Object[] arguments);
}

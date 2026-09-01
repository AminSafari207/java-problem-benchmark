package org.example.jpb.core.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.function.Supplier;
import org.example.jpb.util.ModelChecks;

public final class SystemCommand {

	private final List<String> elements;

	@JsonCreator
	public SystemCommand(List<String> elements) {
		this.elements = ModelChecks.requireNonEmptyCopy(elements, "elements");
	}

	public SystemCommand(Supplier<List<String>> elementsSupplier) {
		this.elements = ModelChecks.requireNonEmptyCopy(
			ModelChecks.requireNonNull(elementsSupplier, "elementsSupplier").get(),
			"elementsSupplier.get()"
		);
	}

	@JsonValue
	public List<String> getElements() {
		return elements;
	}
}

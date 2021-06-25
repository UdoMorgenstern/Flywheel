package com.jozufozu.flywheel.backend.pipeline;

import com.jozufozu.flywheel.backend.loading.Program;
import com.jozufozu.flywheel.backend.loading.TaggedField;
import com.jozufozu.flywheel.backend.loading.TypeHelper;
import com.jozufozu.flywheel.backend.pipeline.span.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderStruct {

	// https://regexr.com/5t207
	public static final Pattern struct = Pattern.compile("struct\\s+([\\w\\d]*)\\s*\\{([\\w\\d \\t#\\[\\](),;\\n]*)}\\s*;");

	public Span name;
	public Span body;

	List<StructField> fields = new ArrayList<>(4);
	Map<String, String> fields2Types = new HashMap<>();

	public ShaderStruct(Span self, Span name, Span body) {
		Matcher fielder = StructField.fieldPattern.matcher(body.getValue());

		while (fielder.find()) {
			fields.add(new StructField(fielder));
			fields2Types.put(fielder.group(2), fielder.group(1));
		}
	}

	public void addPrefixedAttributes(Program builder, String prefix) {
		for (StructField field : fields) {
			int attributeCount = TypeHelper.getAttributeCount(field.type);

			builder.addAttribute(prefix + field.name, attributeCount);
		}
	}
}
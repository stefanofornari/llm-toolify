/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ste.ai.tool;

import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import ste.ai.toolify.tool.ExecutionJSONObject;

/**
 * JSONObject extension to represent a tool execution. It basically consists of
 * a name and a list of arguments. Each argument can be a string or an array
 * of strings (for now, more types may be supported in the future).
 *
 */
public class ExecutionJSONObjectTest {
    @Test
    public void construction() {
        ExecutionJSONObject o = new ExecutionJSONObject("tool");

        then(o.name).isEqualTo("tool");
        then(o.arguments()).isEmpty();

        o = new ExecutionJSONObject("anotherTool", """
        "arg1": "string value",
        "arg2": null,
        "arg3": [
             "one", "two", "three"
        ]
        """);
        then(o.name).isEqualTo("anotherTool");
        then(o.arguments()).isNotNull().isNotEmpty();
    }

    @Test
    public void arguments_by_direct_access() {
        ExecutionJSONObject o = new ExecutionJSONObject("anotherTool", """
        "arg1": "string value",
        "arg2": null,
        "arg3": [
             "one", "two", "three"
        ]
        }""");
        then(o.length()).isEqualTo(3);
        then(o.name).isEqualTo("anotherTool");
        then(o.getString("arg1")).isEqualTo("string value");
        then(o.get("arg2")).isEqualTo(JSONObject.NULL);
        then(o.getJSONArray("arg3").toList()).containsExactly("one", "two", "three");
    }

    @Test
    public void arguments_returns_their_values_by_name() {
        ExecutionJSONObject o = new ExecutionJSONObject("anotherTool", """
        "arg1": "string value",
        "arg2": null,
        "arg3": [
             "one", "two", "three"
        ]
        """);
        then(o.name).isEqualTo("anotherTool");

        final Object[] args =  o.arguments("arg3", "arg1", "arg2");
        then(args).hasSize(3);
        then((List)args[0]).containsExactly("one", "two", "three");
        then(args[1]).isEqualTo("string value");
        then(args[2]).isNull();
    }

    @Test
    public void arguments_returns_null_for_missing_parameters() {
        ExecutionJSONObject o = new ExecutionJSONObject("anotherTool", """
        "argOne": "valueOne"
        """);
        then(o.arguments("argOne", "argTwo")).containsExactly("valueOne", null);
    }
}

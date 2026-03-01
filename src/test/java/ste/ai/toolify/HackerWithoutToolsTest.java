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

package ste.ai.toolify;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.toonformat.jtoon.JToon;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ste.ai.model.DummyChatModel;
import ste.ai.model.DummyChatModelListener;
import ste.ai.model.DummyTool;
import ste.ai.test.DummyJeddictBrainListener;
import ste.ai.toolify.tool.AbstractTool;
import ste.ai.toolify.tool.FileSystemTools;
import static ste.lloop.Loop.on;

/**
 *
 */
public class HackerWithoutToolsTest {

    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ARGUMENTS = "arguments";

    @TempDir
    Path basedir;

    @Test
    void add_and_remove_listeners() throws Exception {
        final DummyChatModel model = chatModel();
        final List<AbstractTool> tools = List.of(new DummyTool());

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", List.of(), model
        );

        final JeddictBrainListener listener1 = new DummyJeddictBrainListener();
        final JeddictBrainListener listener2 = new DummyJeddictBrainListener();

        then(hacker.listeners()).isEmpty();

        hacker.addListener(listener1);
        then(hacker.listeners()).containsExactly(listener1);

        hacker.addListener(listener2);
        then(hacker.listeners()).containsExactly(listener1, listener2);

        hacker.removeListener(listener1);
        then(hacker.listeners()).containsExactly(listener2);

        hacker.removeListener(listener2);
        then(hacker.listeners()).isEmpty();

        // Sanity checks
        thenThrownBy(() -> hacker.addListener(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("listener can not be null");

        // Should not throw when removing null or non-existent listener
        hacker.removeListener(null);
        hacker.removeListener(new DummyJeddictBrainListener());
    }

    @Test
    public void list_of_tools() throws Exception {
        final DummyChatModel model = chatModel();
        final List<AbstractTool> tools = List.of(new DummyTool());

        HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", List.of(), model
        );

        then(hacker.tools).isEmpty();

        hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", tools, model
        );
        then(hacker.tools).containsExactlyElementsOf(tools);
    }

    @Test
    public void system_message_contains_tool_description() throws Exception {
        final String SYSTEM_MESSAGE = "system message";
        final List<AbstractTool> tools = List.of(new DummyTool());
        final DummyChatModelListener listener = new DummyChatModelListener();
        final DummyChatModel model = chatModel(listener);

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", tools, model
        );

        hacker.hack("use mock 'hello world.txt'");

        final ChatRequest r = (ChatRequest)listener.lastRequestContext.get().chatRequest();
        final SystemMessage m = (SystemMessage)r.messages().get(0);
        String systemMessage = m.text();

        then(systemMessage).startsWith(SYSTEM_MESSAGE + "\n");

        systemMessage = systemMessage.substring(SYSTEM_MESSAGE.length() + 1);

        //
        // TOON encoding
        // we need to sort the array to make the requirements below consistenly
        // work.
        //
        final List<JSONObject> descriptions = new ArrayList();
        on(new JSONArray(JToon.decodeToJson(systemMessage))).loop((entry) -> descriptions.add((JSONObject)entry));
        descriptions.sort((a, b) -> {
            return a.getString(KEY_NAME).compareTo(b.getString(KEY_NAME));
        });

        then(descriptions).hasSize(7);
        then(descriptions.get(0).getString(KEY_NAME)).isEqualTo("dummyTool");
        then(descriptions.get(0).getString(KEY_DESCRIPTION)).isEqualTo("simple tool that does nothing");
        then(descriptions.get(0).has(KEY_ARGUMENTS)).isFalse();

        then(descriptions.get(1).getString(KEY_NAME)).isEqualTo("dummyToolError");
        then(descriptions.get(1).has(KEY_DESCRIPTION)).isFalse();
        then(descriptions.get(1).has(KEY_ARGUMENTS)).isFalse();

        then(descriptions.get(3).getString(KEY_NAME)).isEqualTo("dummyToolRead");
        then(descriptions.get(3).getString(KEY_DESCRIPTION)).isEqualTo("simple READONLY tool that does nothing");
        then(descriptions.get(3).has(KEY_ARGUMENTS)).isFalse();

        then(descriptions.get(5).getString(KEY_NAME)).isEqualTo("dummyToolWithArgs");
        then(descriptions.get(5).getString(KEY_DESCRIPTION)).isEqualTo("simple tool that does nothing but with arguments");
        then(descriptions.get(5).getJSONObject(KEY_ARGUMENTS).getString("arg1")).isEqualTo("the first argument");
        then(descriptions.get(5).getJSONObject(KEY_ARGUMENTS).has("arg2")).isFalse();
    }

    @Test
    public void execute_tools() throws Exception {
        final DummyChatModel model = chatModel();
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);

        //
        // No arguments
        //
        HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", tools, model
        );

        hacker.hack("use mock 'dummy tool.txt'");
        then(tool.executed()).isTrue();
        then(tool.arguments()).isNull();

        //
        // With arguments
        //
        tool.reset();
        hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", tools, model
        );
        hacker.hack("use mock 'dummy tool with args.txt'");
        then(tool.executed()).isTrue();
        then(tool.arguments())
            .containsExactly("val1", List.of("val2"));
    }

    @Test
    public void execute_tool_chat_session() throws Exception {
        final String SYSTEM_PROMPT = "use mock 'multi dummy tool.txt'";
        final String USER_PROMPT = "let's go!";
        final DummyChatModelListener listener = new DummyChatModelListener();

        //
        // When a tool is executed, the hacker shall return back autonomously to
        // the LLM to deliver the result of the execution
        //
        final DummyChatModel model = chatModel(listener);
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> SYSTEM_PROMPT, tools, model
        );
        hacker.hack(USER_PROMPT);

        //
        // 1. --> prompt
        // 2. <-- llm response with tool execution
        // 3. --> tool execution result
        // 4. <-- llm response
        //
        final ChatRequest lastRequest = listener.lastResponseContext.get().chatRequest();
        final ChatResponse lastResponse = listener.lastResponseContext.get().chatResponse();

        then(lastResponse.aiMessage().text()).isEqualToIgnoringNewLines("Done executing the DummyTool");
        Object[] messages = lastRequest.messages().toArray();
        then(messages).hasSize(6);


        SystemMessage systemPrompt = null;
        UserMessage userMessage = null;
        AiMessage aiMessage = null;

        int i = 0;
        systemPrompt = (SystemMessage)messages[i++];
        then(systemPrompt.text()).startsWith(SYSTEM_PROMPT);

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo(USER_PROMPT);

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("first round");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo("dummyTool: OK\ntrue");

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("second round");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).contains("dummyToolWithArgs: OK\ntrue\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
    }

    @Test
    public void provide_error_message_if_tool_does_not_exist() throws Exception {
        final DummyChatModel model = chatModel();
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", tools, model
        );

        hacker.addListener(listener);
        hacker.hack("use mock 'not existing tool.txt'");

        then(listener.collector.get(3).getLeft()).isEqualTo("onRequest");
        final UserMessage msg =
            (UserMessage)((ChatRequest)listener.collector.get(3).getRight()).messages().get(3);
        then(msg.contents().toString()).contains("iDoNotExist: ERR java.lang.RuntimeException: tool iDoNotExist not found");
    }

    @Test
    public void send_error_on_tool_execution_exception() throws Exception {
        final String SYSTEM_PROMPT = "use mock 'tool in error.txt'";
        final String USER_PROMPT = "let's go!";

        final DummyChatModel model = chatModel();
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);
        final DummyChatModelListener listener = new DummyChatModelListener();

        model.addListener(listener);

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> SYSTEM_PROMPT, tools, model
        );

        hacker.hack(USER_PROMPT);

        final ChatRequest lastRequest = listener.lastResponseContext.get().chatRequest();
        final ChatResponse lastResponse = listener.lastResponseContext.get().chatResponse();

        then(lastResponse.aiMessage().text()).isEqualToIgnoringNewLines("Thanks for reporting the error...");
        Object[] messages = lastRequest.messages().toArray();
        then(messages).hasSize(4);

        SystemMessage systemPrompt = null;
        UserMessage userMessage = null;
        AiMessage aiMessage = null;

        int i = 0;
        systemPrompt = (SystemMessage)messages[i++];
        then(systemPrompt.text()).startsWith(SYSTEM_PROMPT);

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo(USER_PROMPT);

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("This tool throws an exception");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo("dummyToolError: ERR java.lang.RuntimeException: error in dummyTool");
    }

    @Test
    public void create_calculator_application() throws Exception {
        final DummyChatModel model = chatModel();
        final FileSystemTools tools = new FileSystemTools(basedir.toAbsolutePath().toString());
        final List<ChatModelResponseContext> collector = new ArrayList();

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> "system message", List.of(tools), model
        );

        hacker.hack("use mock 'create calculator.txt'");

        then(basedir.resolve("calculator")).exists();
        then(basedir.resolve("calculator/pom.xml")).exists();
        then(basedir.resolve("calculator/src/main/java/com/example/calculator/CalculatorApp.java")).exists();
        then(basedir.resolve("calculator/src/main/resources")).exists();
        then(basedir.resolve("calculator/README.md")).exists();
    }

    @Test
    public void all_listeners_receive_receive_all_events_ok() throws IOException {
        final String SYSTEM_PROMPT = "use mock 'multi dummy tool.txt'";
        final String USER_PROMPT = "let's go!";
        final DummyTool tool = new DummyTool();
        final DummyChatModel model = chatModel();

        final DummyJeddictBrainListener
            listener1 = new DummyJeddictBrainListener(),
            listener2 = new DummyJeddictBrainListener();

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> SYSTEM_PROMPT, List.of(tool), model
        );
        hacker.addListener(listener1); hacker.addListener(listener2);
        tool.addListener(listener1); tool.addListener(listener2);

        hacker.hack(USER_PROMPT);

        //
        // 0 - chatStarted
        //
        final AtomicInteger i = new AtomicInteger();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatStarted");

            final SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
            final UserMessage u = (UserMessage)((Object[])args.getRight())[1];
            then(s.text()).startsWith(SYSTEM_PROMPT);
            then(u.singleText()).isEqualToIgnoringNewLines(USER_PROMPT);
        });

        //
        // 1 - chatRequest - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(SYSTEM_PROMPT);
            then(u.singleText()).startsWith(USER_PROMPT);
        });

        //
        // 2 - chatResponse - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(SYSTEM_PROMPT);
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isTrue();
        });

        //
        // 3 - toolProgress
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onProgress");

            then(args.getRight()).isEqualTo("\nexecuting dummyTool");
        });

        //
        // 4 - toolExecuted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onToolExecuted");

            final Object[] exec = (Object[])args.getRight();
            final ToolExecutionRequest request = (ToolExecutionRequest)exec[0];
            final String result = (String)exec[1];

            then(request.name()).isEqualTo("dummyTool");
            then(result).isEqualTo("true");
        });

        //
        // 5 - chatRequest - tool execution result
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            then(req.messages()).hasSize(4);
            then(((UserMessage)req.messages().get(3)).singleText()).isEqualTo("dummyTool: OK\ntrue");
        });

        //
        //  6 - chatResponse
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(SYSTEM_PROMPT);
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isTrue();
        });

        //
        // 7 - toolProgress
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onProgress");

            then((String)args.getRight()).startsWith("\nexecuting dummyToolWithArgs");
        });

        //
        // 8 - toolExecuted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onToolExecuted");

            final Object[] exec = (Object[])args.getRight();
            final ToolExecutionRequest request = (ToolExecutionRequest)exec[0];
            final String result = (String)exec[1];

            then(request.name()).isEqualTo("dummyToolWithArgs");
            then(result).isEqualTo("true\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
        });

        //
        // 9 - chatRequest - tool execution result
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            then(req.messages()).hasSize(6);
            then(((UserMessage)req.messages().get(5)).singleText())
                .isEqualTo("dummyToolWithArgs: OK\ntrue\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
        });

        //
        //  10 - chatResponse
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(SYSTEM_PROMPT);
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isFalse();
        });

        //
        // 11 - chatCompleted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatCompleted");

            final ChatResponse res = (ChatResponse)args.getRight();
            then(res.aiMessage().text()).isEqualToIgnoringNewLines("Done executing the DummyTool");
        });
    }

    // --------------------------------------------------------- private methods

    private DummyChatModel chatModel(final ChatModelListener listener) {
        final DummyChatModel model = chatModel();

        model.listeners().add(listener);

        return model;
    }

    /**
     * A model used in LLMToolify must be initialized with the listener adapter
     * so that events generated by the model can be properly manipulated and
     * routed by HackerWithoutTool
     */
    private DummyChatModel chatModel() {
        final DummyChatModel model = new DummyChatModel();

        model.addListener(new HackerWithoutTools.JeddictListenerAdapter());

        return model;
    }
}

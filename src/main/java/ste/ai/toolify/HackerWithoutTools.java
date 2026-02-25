/**
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ste.ai.toolify;

import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.toonformat.jtoon.JToon;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.json.JSONArray;
import org.json.JSONObject;
import ste.ai.toolify.tool.AbstractTool;
import ste.ai.toolify.tool.ExecutionJSONObject;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class HackerWithoutTools {

    private final ToolifyAgent toolify;
    private final Logger LOG = Logger.getLogger(getClass().getName());

    protected final Function<Object, String> systemPromptProvider;
    protected final List<AbstractTool> tools;
    protected final Parser parser;

    private final List<JeddictBrainListener> listeners = new CopyOnWriteArrayList<>();

    public HackerWithoutTools(
        final String endpoint,
        final String apiKey,
        final String modelName,
        final Function<Object, String> systemPromptProvider,
        final List<AbstractTool> tools,
        final ChatModel chatModel
    ) {
        this.systemPromptProvider = systemPromptProvider;
        this.toolify = AiServices.builder(ToolifyAgent.class)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
                .systemMessageProvider(this::systemPromptProvider)
                .chatModel(chatModel)
                .build();
        this.tools = tools;
        this.parser = Parser.builder().build();
    }

    public HackerWithoutTools(
        final String endpoint,
        final String apiKey,
        final String modelName,
        final Function<Object, String> systemPromptProvider,
        final List<AbstractTool> tools
    ) {
        this(
            endpoint, apiKey, modelName, systemPromptProvider, tools,
            OpenAiChatModel.builder()
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build()
        );
    }

    public List<JeddictBrainListener> listeners() {
        return listeners;
    }

    public void addListener(final JeddictBrainListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        listeners.add(listener);
    }

    public void removeListener(final JeddictBrainListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public String chat(final String prompt) {
        final boolean[] toolExecution = new boolean[] { false };

        final String[] nextPrompt = new String[] { prompt };

        String answer;
        int n = 0;

        do {
            answer = toolify.chat(nextPrompt[0]);

            //
            // is there any tool execution block? if so, let's execute it
            //
            final Node document = parser.parse(answer);

            toolExecution[0] = false;
            document.accept(new AbstractVisitor() {
                @Override
                public void visit(FencedCodeBlock block) {
                    // Check if the info string (language tag) is "tool"
                    final String type = StringUtils.defaultString(block.getInfo());
                    if (type.startsWith("tool:")) {
                        toolExecution[0] = true;
                        final String name = type.substring(5);
                        final String content = block.getLiteral();
                        LOG.finest(() ->
                            "found tool block for tool %s with content:\n%s".formatted(
                                name,
                                StringUtils.abbreviateMiddle(content, "...", 100)
                            )
                        );
                        try {
                            final String result = executeTool(new ExecutionJSONObject(name, content));
                            nextPrompt[0] = "%s: OK\n%s".formatted(name, result);
                        } catch (ToolExecutionException x) {
                            //
                            // If the exception does not have any root cause,
                            // the issue is with the process of executing a tool
                            // which we want to report to the listeners.
                            // If instead there is a root cause, the tool itself
                            // got an error, therefore we just need to notify it
                            // to the llm
                            //
                            if (x.getCause() == null) {
                                on(listeners).loop((l) -> l.onError(x));
                            } else {
                                nextPrompt[0] = "%s: ERR %s".formatted(name, String.valueOf(x.getCause()));
                            }
                        } catch (Throwable x) {
                            //
                            // for any other issues we can't do much more than
                            // reporting it to the listeners
                            //
                            on(listeners).loop((l) -> l.onError(x));
                        }
                    }
                }
            });

            ++n;
        } while (toolExecution[0] && n <= 5);

        return answer;
    }

    private String executeTool(final ExecutionJSONObject toolExecution) {
        LOG.finest(() -> "executing " + toolExecution.name);

        final String[] result = new String[1];
        final Boolean found = on(tools).loop((tool) -> {
            final Class cls = tool.getClass();
            final Boolean foundInClass = on(cls.getMethods()).loop((method) -> {
                if (method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class) && method.getName().equals(toolExecution.name)) {
                    try {
                        final List<String> parameterNames = new ArrayList<>();
                        on(method.getParameters()).loop(p -> parameterNames.add(p.getName()));

                        final Object[] args = (parameterNames.isEmpty()) ? new Object[0] : toolExecution.arguments(parameterNames.toArray(new String[0]));

                        final Object ret = method.invoke(tool, args);

                        if (ret == null) {
                            result[0] = null;
                        } else {
                            result[0] = (ret instanceof String strret) ? strret : String.valueOf(ret);
                        }
                    } catch (Exception x) {
                        LOG.info(() -> "tried to execute %s on %s@%d but got the error %s".formatted(
                            toolExecution.name, tool.getClass(), tool.hashCode(), x.toString()
                        ));
                        throw new ToolExecutionException(
                             (x instanceof InvocationTargetException ite) ? ite.getTargetException() : x
                        );
                    }
                    _break_(true);
                }
            });
            if (foundInClass != null) {
                _break_(true);
            }
        });

        if (found == null) {
            //
            // ToolsExecutionException always return cause. If not provided
            // it returns itself, which is not very useful. That is why we
            // create a Exception cause.
            //
            throw new ToolNotFoundException(toolExecution.name);
        }

        return result[0];
    }

    /**
     * This is to build the final system message, which is built of the following:
     *
     * 1. the prompt provided by calling {@code systemPromptProvider}
     * 2. the tool descriptions for the provided {@code tools}
     *
     * @param o - ignored
     *
     * @return the combination of the provided system prompt and tools description
     */
    private String systemPromptProvider(final Object o) {

        //
        // 1. the provided system message
        //
        final String systemPrompt = systemPromptProvider.apply(o);

        //
        // 2. the tool description
        //
        final JSONArray array = new JSONArray();
        on(tools).loop((toolIstance) -> {
            on(ToolSpecifications.toolSpecificationsFrom(toolIstance.getClass())).loop((toolDesc) -> {
                final JSONObject tool = new JSONObject();

                final JSONObject toolArgs = new JSONObject();
                if (toolDesc.parameters() != null) {
                    on(toolDesc.parameters().properties()).loop((name, description) -> {
                        if (description != null) {
                            toolArgs.put(name, description.description());
                        }
                    });
                }

                tool.put("name", toolDesc.name());
                if (toolDesc.description() != null) {
                    tool.put("description", toolDesc.description());
                }
                if (toolArgs.length() > 0) {
                    tool.put("arguments", toolArgs);
                }

                array.put(tool);
            });
        });
        return systemPrompt + "\n" + JToon.encodeJson(array.toString());
    }

}
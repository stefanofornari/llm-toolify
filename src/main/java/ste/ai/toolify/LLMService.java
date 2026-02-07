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

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.util.function.Function;

public class LLMService {

    private final ToolifyAgent toolify;

    public LLMService(
        final String endpoint, 
        final String apiKey, 
        final String model, 
        final Function<Object, String> systemMessageProvider
    ) {
        System.out.println(endpoint);
        System.out.println(apiKey);
        System.out.println(model);
        final ChatModel chatModel = OpenAiChatModel.builder()
            .baseUrl(endpoint)
            .apiKey(apiKey)
            .modelName(model)
            .logRequests(true)
            .logResponses(true)
            .build();
        
        toolify = AiServices.builder(ToolifyAgent.class)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
                .systemMessageProvider(systemMessageProvider)
                .chatModel(chatModel)
                .build();
    }

    public String chat(final String prompt) {
        return toolify.chat(prompt);
    }

}
/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.spring.demo.ai.playground.services;

import java.time.LocalDate;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * * @author Christian Tzolov
 */
@Service
public class CustomerSupportAssistant {

	private final ChatClient chatClient;

	public CustomerSupportAssistant(ChatClient.Builder modelBuilder, VectorStore vectorStore, ChatMemory chatMemory) {

		// @formatter:off
		this.chatClient = modelBuilder
				.defaultSystem("""
						Você é um agente de suporte de chat ao cliente de uma companhia aérea chamada "Epic Airlines".
						  Responda de forma amigável, prestativa e alegre.
						  Você está interagindo com os clientes por meio de um sistema de chat online.
						  Antes de fornecer informações sobre uma reserva ou cancelar uma reserva, você DEVE sempre
						  obter as seguintes informações do usuário: número da reserva, nome e sobrenome do cliente.
						  Verifique o histórico de mensagens para obter essas informações antes de perguntar ao usuário.
						  Antes de alterar uma reserva, você DEVE garantir que isso seja permitido pelos termos.
						  Se houver uma cobrança pela alteração, você DEVE pedir o consentimento do usuário antes de prosseguir.
						  Use as funções fornecidas para buscar detalhes da reserva, alterar reservas e cancelar reservas.
						  Use a chamada de função paralela, se necessário.
						  Hoje é {current_date}.
					""")
				.defaultAdvisors(
						new PromptChatMemoryAdvisor(chatMemory),

						new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()),

						new LoggingAdvisor())

				// ⬇️ adicione aqui os métodos do booking tools que podem ser executados pela IA
				.defaultFunctions("getBookingDetails")

				.build();
		// @formatter:on
	}

	public Flux<String> chat(String chatId, String userMessageContent) {

		return this.chatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a
						.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
						.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.stream().content();
	}
}
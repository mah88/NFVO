/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
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
 *
 */

package org.openbaton.nfvo.system;

import com.google.gson.JsonSyntaxException;
import org.openbaton.nfvo.core.interfaces.ComponentManager;
import org.openbaton.nfvo.core.interfaces.EventDispatcher;
import org.openbaton.vnfm.interfaces.manager.VnfmReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Created by lto on 09/11/15. */
@Configuration
@EnableRabbit
@ConfigurationProperties(prefix = "nfvo.rabbit")
public class RabbitConfiguration {

  static final String queueName_managerRegister = "nfvo.manager.handling";

  static final String queueName_vnfmCoreActions = "vnfm.nfvo.actions";
  static final String queueName_vnfmCoreActionsReply = "vnfm.nfvo.actions.reply";
  static final String queueName_eventRegister = "nfvo.event.register";
  static final String queueName_eventUnregister = "nfvo.event.unregister";

  @Value("${nfvo.rabbitmq.autodelete:true}")
  private boolean autodelete;

  @Value("${nfvo.rabbitmq.durable:true}")
  private boolean durable;

  @Value("${nfvo.rabbitmq.exclusive:false}")
  private boolean exclusive;

  @Value("${spring.rabbitmq.listener.concurrency:5}")
  private int minConcurrency;

  @Value("${spring.rabbitmq.listener.max-concurrency:15}")
  private int maxConcurrency;

  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  public void setMaxConcurrency(int maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
  }

  public int getMinConcurrency() {
    return minConcurrency;
  }

  public void setMinConcurrency(int minConcurrency) {
    this.minConcurrency = minConcurrency;
  }

  public boolean isAutodelete() {
    return autodelete;
  }

  public void setAutodelete(boolean autodelete) {
    this.autodelete = autodelete;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  /**
   * ***************************
   *
   * <p>Defining Queues
   *
   * <p>***************************
   */
  @Bean
  Queue queue_eventRegister() {
    return new Queue(queueName_eventRegister, durable, exclusive, autodelete);
  }

  @Bean
  Queue queue_eventUnregister() {
    return new Queue(queueName_eventUnregister, durable, exclusive, autodelete);
  }

  @Bean
  Queue queue_managerRegister() {
    return new Queue(queueName_managerRegister, true, exclusive, true);
  }

  @Bean
  Queue queue_vnfmCoreActions() {
    return new Queue(queueName_vnfmCoreActions, durable, exclusive, autodelete);
  }

  @Bean
  Queue queue_vnfmCoreActionsReply() {
    return new Queue(queueName_vnfmCoreActionsReply, durable, exclusive, autodelete);
  }

  @Bean
  TopicExchange exchange() {
    return new TopicExchange("openbaton-exchange");
  }

  /**
   * ***************************
   *
   * <p>Defining Bindings
   *
   * <p>***************************
   */
  @Bean
  Binding binding_managerRegister(TopicExchange exchange) {
    return BindingBuilder.bind(queue_managerRegister())
        .to(exchange)
        .with(queueName_managerRegister);
  }

  @Bean
  Binding binding_eventRegister(TopicExchange exchange) {
    return BindingBuilder.bind(queue_eventRegister()).to(exchange).with(queueName_eventRegister);
  }

  @Bean
  Binding binding_vnfmCoreAction(TopicExchange exchange) {
    return BindingBuilder.bind(queue_vnfmCoreActions())
        .to(exchange)
        .with(queueName_vnfmCoreActions);
  }

  @Bean
  Binding binding_vnfmCoreActionReply(TopicExchange exchange) {
    return BindingBuilder.bind(queue_vnfmCoreActionsReply())
        .to(exchange)
        .with(queueName_vnfmCoreActionsReply);
  }

  @Bean
  Binding binding_eventUnregister(TopicExchange exchange) {
    return BindingBuilder.bind(queue_eventUnregister())
        .to(exchange)
        .with(queueName_eventUnregister);
  }

  /**
   * ***************************
   *
   * <p>Defining Listeners
   *
   * <p>***************************
   */
  @Bean
  MessageListenerAdapter listenerAdapter_eventRegister(EventDispatcher eventDispatcher) {
    return new MessageListenerAdapter(eventDispatcher, "register");
  }

  @Bean
  MessageListenerAdapter listenerAdapter_eventUnregister(EventDispatcher eventDispatcher) {
    return new MessageListenerAdapter(eventDispatcher, "unregister");
  }

  @Bean
  MessageListenerAdapter listenerAdapter_managerRegister(ComponentManager componentManager) {
    return new MessageListenerAdapter(componentManager, "enableManager");
  }

  @Bean
  MessageListenerAdapter listenerAdapter_vnfmCoreActions(
      @Qualifier("rabbitVnfmReceiver") VnfmReceiver vnfmReceiver) {
    return new MessageListenerAdapter(vnfmReceiver, "actionFinishedVoid");
  }

  @Bean
  MessageListenerAdapter listenerAdapter_vnfmCoreActionsReply(
      @Qualifier("rabbitVnfmReceiver") VnfmReceiver vnfmReceiver) {
    return new MessageListenerAdapter(vnfmReceiver, "actionFinished");
  }

  /**
   * ***************************
   *
   * <p>Defining Containers
   *
   * <p>***************************
   */
  @Bean
  SimpleMessageListenerContainer container_managerRegister(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_managerRegister") MessageListenerAdapter listenerAdapter) {
    return getSimpleMessageListenerContainer(
        connectionFactory, listenerAdapter, queueName_managerRegister);
  }

  @Bean
  SimpleMessageListenerContainer container_eventRegister(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_eventRegister") MessageListenerAdapter listenerAdapter) {
    return getSimpleMessageListenerContainer(
        connectionFactory, listenerAdapter, queueName_eventRegister);
  }

  private SimpleMessageListenerContainer getSimpleMessageListenerContainer(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_eventRegister") MessageListenerAdapter listenerAdapter,
      String queueName_eventRegister) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(queueName_eventRegister);
    container.setMessageListener(listenerAdapter);
    container.setErrorHandler(
        new ConditionalRejectingErrorHandler(new HandleJsonSyntaxExceptionStrategy()));
    return container;
  }

  @Bean
  SimpleMessageListenerContainer container_eventUnregister(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_eventUnregister") MessageListenerAdapter listenerAdapter) {
    SimpleMessageListenerContainer container =
        getSimpleMessageListenerContainer(
            connectionFactory, listenerAdapter, queueName_eventUnregister);
    return container;
  }

  @Bean
  SimpleMessageListenerContainer container_vnfmCoreActions(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_vnfmCoreActions") MessageListenerAdapter listenerAdapter) {
    return getSimpleMessageListenerContainer(
        connectionFactory,
        listenerAdapter,
        queueName_vnfmCoreActions,
        minConcurrency,
        maxConcurrency);
  }

  private SimpleMessageListenerContainer getSimpleMessageListenerContainer(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_vnfmCoreActions") MessageListenerAdapter listenerAdapter,
      String queueName_vnfmCoreActions,
      int minConcurrency,
      int maxConcurrency) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(queueName_vnfmCoreActions);
    if (minConcurrency <= 0 || maxConcurrency <= 0 || minConcurrency > maxConcurrency) {
      container.setConcurrentConsumers(5);
      container.setMaxConcurrentConsumers(15);
    } else {
      container.setConcurrentConsumers(minConcurrency);
      container.setMaxConcurrentConsumers(maxConcurrency);
    }
    container.setMessageListener(listenerAdapter);
    container.setErrorHandler(
        new ConditionalRejectingErrorHandler(new HandleJsonSyntaxExceptionStrategy()));
    return container;
  }

  @Bean
  SimpleMessageListenerContainer container_vnfmCoreActionsReply(
      ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter_vnfmCoreActionsReply") MessageListenerAdapter listenerAdapter) {
    SimpleMessageListenerContainer container =
        getSimpleMessageListenerContainer(
            connectionFactory,
            listenerAdapter,
            queueName_vnfmCoreActionsReply,
            minConcurrency,
            maxConcurrency);
    return container;
  }

  /**
   * Extension of Spring-AMQP's {@link ConditionalRejectingErrorHandler.DefaultExceptionStrategy}.
   * It regards a {@link JsonSyntaxException}, which may appear while demarshalling a message from a
   * queue, as fatal and drops it. Otherwise the message would be sent back to the queue resulting
   * in an infinite loop.
   */
  private class HandleJsonSyntaxExceptionStrategy implements FatalExceptionStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isFatal(Throwable t) {
      if (t instanceof ListenerExecutionFailedException
          && (t.getCause() instanceof MessageConversionException
              || t.getCause() instanceof JsonSyntaxException)) {
        log.error(
            "Fatal message conversion error; message rejected; "
                + "it will be dropped or routed to a dead letter exchange, if so configured: "
                + ((ListenerExecutionFailedException) t).getFailedMessage(),
            t);
        return true;
      }
      return false;
    }
  }
}

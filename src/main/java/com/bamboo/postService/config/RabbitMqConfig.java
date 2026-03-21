package com.bamboo.postService.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("user.events", true, false);
    }

    @Bean
    public TopicExchange contentTopicExchange() {
        return new TopicExchange("content.events", true, false);
    }

    @Bean
    public TopicExchange collabTopicExchange() {
        return new TopicExchange("collab.events", true, false);
    }

    @Bean
    public Queue profileUpdatedQueue() {
        return new Queue("queue.user.profile.updated", true);
    }

    @Bean
    public Binding profileUpdatedBinding(
            @Qualifier("topicExchange") TopicExchange topicExchange, Queue profileUpdatedQueue) {
        return BindingBuilder.bind(profileUpdatedQueue).to(topicExchange).with("profile.updated");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

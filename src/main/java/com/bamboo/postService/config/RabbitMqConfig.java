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
    public TopicExchange commentTopicExchange() {
        return new TopicExchange("comment.events", true, false);
    }

    @Bean
    public Queue profileUpdatedQueue() {
        return new Queue("queue.user.profile.updated", true);
    }

    @Bean
    public Queue commentPublishedQueue() {
        return new Queue("queue.comment.published", true);
    }

    @Bean
    public Queue commentDeletedQueue() {
        return new Queue("queue.comment.deleted", true);
    }

    @Bean
    public Binding profileUpdatedBinding(
            @Qualifier("topicExchange") TopicExchange topicExchange, Queue profileUpdatedQueue) {
        return BindingBuilder.bind(profileUpdatedQueue).to(topicExchange).with("profile.updated");
    }

    @Bean
    public Binding commentPublishedBinding(
            @Qualifier("commentTopicExchange") TopicExchange commentTopicExchange,
            @Qualifier("commentPublishedQueue") Queue commentPublishedQueue) {
        return BindingBuilder.bind(commentPublishedQueue)
                .to(commentTopicExchange)
                .with("comment.event.published");
    }

    @Bean
    public Binding commentDeletedBinding(
            @Qualifier("commentTopicExchange") TopicExchange commentTopicExchange,
            @Qualifier("commentDeletedQueue") Queue commentDeletedQueue) {
        return BindingBuilder.bind(commentDeletedQueue)
                .to(commentTopicExchange)
                .with("comment.event.deleted");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
